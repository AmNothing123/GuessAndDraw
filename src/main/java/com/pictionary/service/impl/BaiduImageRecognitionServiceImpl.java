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
import java.util.Map;

/**
 * 百度AI图像识别服务实现
 * 使用百度AI开放平台的图像识别API
 */
@Service("baiduImageRecognitionService")
@Slf4j
public class BaiduImageRecognitionServiceImpl implements ImageRecognitionService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${ai.baidu.api-key:}")
    private String apiKey;
    
    @Value("${ai.baidu.secret-key:}")
    private String secretKey;
    
    @Value("${ai.baidu.enabled:false}")
    private boolean enabled;
    
    // 百度AI接口地址
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String REQUEST_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v1/image-understanding/request";
    
    // 轮询间隔和超时设置
    private static final long POLLING_INTERVAL_MS = 1000; // 轮询间隔1秒
    private static final long POLLING_TIMEOUT_MS = 30000; // 超时时间30秒
    
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
            
            // 使用ImageUtil处理图像
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            BufferedImage processedImage = ImageUtil.resizeImage(originalImage, 300, 300);
            processedImage = ImageUtil.enhanceContrast(processedImage);
            
            // 将处理后的图像转换回Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "PNG", outputStream);
            String processedBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
            // 获取访问令牌
            String accessToken = getAccessToken();
            
            // 提交识别请求并获取结果
            Map<String, Object> responseMap = submitRecognitionRequest(processedBase64, accessToken);
            
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
     * 提交识别请求
     *
     * @param base64ImageData Base64编码的图像数据
     * @param accessToken 访问令牌
     * @return 请求ID
     */
    private Map<String, Object> submitRecognitionRequest(String base64ImageData, String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("image", base64ImageData);
        params.add("baike_num", "5");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        String url = REQUEST_URL + "?access_token=" + accessToken;
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
        
        if (responseMap.containsKey("error_code")) {
            throw new RuntimeException("识别请求失败: " + responseMap);
        }
        return responseMap;
    }
    
    /**
     * 轮询获取识别结果
     *
     * @param requestId 请求ID
     * @param accessToken 访问令牌
     * @return 识别结果
     */
//    private Map<String, Object> pollRecognitionResult(String requestId, String accessToken) throws Exception {
//        long startTime = System.currentTimeMillis();
//
//        while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT_MS) {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//            params.add("request_id", requestId);
//
//            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
//            String url = GET_RESULT_URL + "?access_token=" + accessToken;
//
//            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
//            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
//
//            if (responseMap.containsKey("status") && "FINISHED".equals(responseMap.get("status"))) {
//                return parseRecognitionResult(responseMap);
//            }
//
//            Thread.sleep(POLLING_INTERVAL_MS);
//        }
//
//        Map<String, Object> timeoutResult = new HashMap<>();
//        timeoutResult.put("success", false);
//        timeoutResult.put("message", "识别请求超时");
//        return timeoutResult;
//    }
    
    /**
     * 解析百度AI返回的识别结果
     *
     * @param responseMap 百度AI返回的原始结果
     * @return 格式化后的结果
     */
    private Map<String, Object> parseRecognitionResult(Map<String, Object> responseMap) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        
        if (responseMap.containsKey("result") && responseMap.get("result") instanceof java.util.List) {
            java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) responseMap.get("result");
            
            if (!items.isEmpty()) {
                Map<String, Object> topResult = items.get(0);
                String name = (String) topResult.get("keyword");
                Double score = Double.parseDouble(topResult.get("score").toString());
                int confidence = (int) (score * 100);
                
                result.put("prediction", name);
                result.put("confidence", confidence);
                result.put("alternatives", items);
                
                // 添加百科信息（如果有）
                if (topResult.containsKey("baike_info")) {
                    result.put("baike_info", topResult.get("baike_info"));
                }
            } else {
                result.put("prediction", "未知物体");
                result.put("confidence", 0);
            }
        } else {
            result.put("prediction", "识别失败");
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
        result.put("prediction", "熊猫");
        result.put("confidence", 85);
        result.put("mock", true);
        
        return result;
    }
}