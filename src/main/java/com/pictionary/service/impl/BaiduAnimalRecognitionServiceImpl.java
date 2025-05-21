package com.pictionary.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pictionary.service.ImageRecognitionService;
import com.pictionary.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百度AI线条识别服务实现
 * 基于百度AI平台的手写文字识别API，适用于识别画布上的简笔画和线条
 */
@Service("baiduAnimalRecognitionService")
@Slf4j
public class BaiduAnimalRecognitionServiceImpl implements ImageRecognitionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${ai.baidu.api-key:}")
    private String apiKey;
    
    @Value("${ai.baidu.secret-key:}")
    private String secretKey;
    
    @Value("${ai.baidu.enabled:false}")
    private boolean enabled;
    
    // 百度AI接口地址
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String HANDWRITING_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/handwriting";
    
    public BaiduAnimalRecognitionServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 获取百度AI访问令牌
     *
     * @return 访问令牌
     */
    private String getAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "client_credentials");
            params.add("client_id", apiKey);
            params.add("client_secret", secretKey);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            } else {
                throw new RuntimeException("获取百度AI访问令牌失败");
            }
        } catch (Exception e) {
            log.error("获取百度AI访问令牌时发生错误", e);
            throw new RuntimeException("获取百度AI访问令牌失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> recognizeImage(String base64ImageData) {
        try {
            // 如果百度AI服务未启用，返回模拟数据
            if (!enabled) {
                log.warn("百度AI服务未启用，返回模拟数据");
                return getMockResult();
            }
            
            // 解码Base64图像数据
            byte[] imageBytes = Base64.getDecoder().decode(base64ImageData);
            
            try {
                // 使用ImageUtil处理图像，增强线条对比度
                BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (originalImage != null) {
                    log.debug("原始图像尺寸: {}x{}", originalImage.getWidth(), originalImage.getHeight());
                    
                    // 调整图像大小，确保不超过API限制
                    BufferedImage processedImage = ImageUtil.resizeImage(originalImage, 500, 500);
                    // 增强对比度，使线条更清晰
                    processedImage = ImageUtil.enhanceContrast(processedImage);
                    // 可以添加更多的图像处理步骤，如二值化处理，使线条更明显
                    
                    // 将处理后的图像转换回Base64
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ImageIO.write(processedImage, "PNG", outputStream);
                    base64ImageData = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                    log.debug("图像已预处理，增强线条对比度");
                } else {
                    log.warn("无法解析图像数据，将使用原始数据");
                }
            } catch (Exception e) {
                log.warn("图像预处理失败，将使用原始图像: {}", e.getMessage());
            }
            
            // 获取访问令牌
            String accessToken = getAccessToken();
            
            // 准备请求参数
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("image", base64ImageData);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            String url = HANDWRITING_URL + "?access_token=" + accessToken;
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            
            // 解析百度AI返回的结果
            return parseRecognitionResult(responseMap);
            
        } catch (Exception e) {
            log.error("线条识别过程中发生错误", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "识别失败: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * 解析百度AI返回的识别结果
     *
     * @param responseMap 百度AI返回的原始结果
     * @return 格式化后的结果
     */
    private Map<String, Object> parseRecognitionResult(Map<String, Object> responseMap) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        
        // 记录原始响应数据，帮助调试
        log.debug("百度手写识别API返回原始数据: {}", responseMap);
        
        if (responseMap.containsKey("words_result") && responseMap.get("words_result") instanceof List) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) responseMap.get("words_result");
            log.debug("识别结果列表大小: {}", items.size());
            
            if (!items.isEmpty()) {
                // 合并所有识别出的文字，作为预测结果
                StringBuilder prediction = new StringBuilder();
                for (Map<String, Object> item : items) {
                    if (item.containsKey("words")) {
                        if (prediction.length() > 0) {
                            prediction.append(", ");
                        }
                        prediction.append(item.get("words"));
                    }
                }
                
                String predictionText = prediction.toString();
                log.debug("识别结果: {}", predictionText);
                
                // 计算置信度（这里简化处理，实际可能需要根据API返回的具体数据调整）
                int confidence = items.size() > 0 ? 85 : 0;
                
                result.put("prediction", predictionText);
                result.put("confidence", confidence);
                result.put("alternatives", items);
            } else {
                log.debug("识别结果为空列表");
                result.put("prediction", "未识别出内容");
                result.put("confidence", 0);
            }
        } else {
            log.debug("识别结果不包含有效的words_result字段或格式不正确: {}", responseMap);
            // 检查是否包含error_code和error_msg
            if (responseMap.containsKey("error_code")) {
                log.error("百度API返回错误: code={}, msg={}", responseMap.get("error_code"), responseMap.get("error_msg"));
                result.put("prediction", "识别服务暂时不可用");
            } else {
                // 如果没有错误码但格式不正确，可能是空白画布或无法识别的图像
                result.put("prediction", "无法识别，请尝试绘制更清晰的图像");
            }
            result.put("confidence", 0);
            result.put("error", responseMap);
        }
        
        return result;
    }
    
    /**
     * 获取模拟的识别结果（当百度AI服务未启用时使用）
     *
     * @return 模拟的识别结果
     */
    private Map<String, Object> getMockResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("prediction", "简笔画");
        result.put("confidence", 85);
        result.put("mock", true);
        
        return result;
    }
}