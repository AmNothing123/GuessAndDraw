package com.pictionary.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pictionary.service.ImageRecognitionService;
import com.pictionary.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * 豆包API图像识别服务实现
 * 使用豆包API进行线条画识别
 */
@Service("doubaoImageRecognitionService")
@Slf4j
public class DoubaoImageRecognitionServiceImpl implements ImageRecognitionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.doubao.api-key:}")
    private String apiKey;
    
    @Value("${ai.doubao.enabled:true}")
    private boolean enabled;
    
    // 豆包API接口地址
    private static final String CHAT_COMPLETION_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    
    public DoubaoImageRecognitionServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Map<String, Object> recognizeImage(String base64ImageData) {
        try {
            // 如果豆包AI服务未启用，返回模拟数据
            if (!enabled) {
                log.warn("豆包AI服务未启用，返回模拟数据");
                return getMockResult();
            }
            
            // 解码Base64图像数据
            byte[] imageBytes = Base64.getDecoder().decode(base64ImageData);
            
            // 使用ImageUtil处理图像
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            BufferedImage processedImage = ImageUtil.resizeImage(originalImage, 300, 300);
            processedImage = ImageUtil.enhanceContrast(processedImage);
            
            // 将处理后的图像转换回Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "PNG", outputStream);
            String processedBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
            // 准备请求参数
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            headers.set("Volc-Access-Key", apiKey);
            
            // 构建多模态请求内容
            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("type", "text");
            messageContent.put("text", "这是什么画？请只回答一个词语，表示你认为画的是什么，不要有任何额外解释。");

            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            
            // 构建图像URL对象
            Map<String, String> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:image/png;base64," + processedBase64);
            imageContent.put("image_url", imageUrl);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "doubao-1.5-ui-tars-250328");
            
            // 构建消息数组
            List<Map<String, Object>> contentList = new ArrayList<>();
            contentList.add(messageContent);
            contentList.add(imageContent);
            
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", contentList);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 50);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求到豆包API
            Map<String, Object> responseMap = restTemplate.postForObject(CHAT_COMPLETION_URL, request, Map.class);
            
            // 解析豆包API返回的结果
            return parseRecognitionResult(responseMap);
            
        } catch (Exception e) {
            log.error("图像识别过程中发生错误", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "识别失败: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * 解析豆包API返回的识别结果
     *
     * @param responseMap 豆包API返回的原始结果
     * @return 格式化后的结果
     */
    private Map<String, Object> parseRecognitionResult(Map<String, Object> responseMap) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        
        try {
            if (responseMap.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    if (firstChoice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message.containsKey("content")) {
                            String content = (String) message.get("content");
                            
                            // 提取第一行作为猜测的词语，并移除可能的标点符号
                            String guessedWord = content.split("\\n")[0]
                                    .replaceAll("[，。！？,.!?]", "")
                                    .trim();
                            
                            result.put("prediction", guessedWord);
                            result.put("confidence", 85); // 设置一个默认的置信度
                            log.info("豆包AI识别结果: {}", guessedWord);
                        }
                    }
                }
            }
            
            // 如果没有成功解析出预测结果，设置默认值
            if (!result.containsKey("prediction")) {
                result.put("prediction", "未知物体");
                result.put("confidence", 0);
                result.put("error", responseMap);
                log.warn("无法从豆包API响应中提取有效内容: {}", responseMap);
            }
        } catch (Exception e) {
            log.error("解析豆包API响应时发生异常", e);
            result.put("prediction", "解析错误");
            result.put("confidence", 0);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取模拟的识别结果（当豆包AI服务未启用时使用）
     *
     * @return 模拟的识别结果
     */
    private Map<String, Object> getMockResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("prediction", "猫");
        result.put("confidence", 88);
        result.put("mock", true);
        
        return result;
    }
    
    /**
     * 测试与豆包API的连接
     * @return 是否连接成功
     */
    public boolean testConnection() {
        try {
            // 构建简单的测试请求
            Map<String, Object> testRequest = new HashMap<>();
            testRequest.put("model", "doubao-1.5-ui-tars-250328");

            // 构建消息内容
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "测试连接");
            
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(message);

            testRequest.put("messages", messages);
            testRequest.put("max_tokens", 5);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Volc-Access-Key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(testRequest, headers);

            // 发送请求
            Map<String, Object> response = restTemplate.postForObject(CHAT_COMPLETION_URL, entity, Map.class);

            return response != null && response.containsKey("choices");
        } catch (Exception e) {
            log.error("豆包API连接测试失败", e);
            return false;
        }
    }
}