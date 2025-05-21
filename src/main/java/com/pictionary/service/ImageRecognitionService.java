package com.pictionary.service;

import java.util.Map;

/**
 * 图像识别服务接口
 * 定义图像识别相关的方法
 */
public interface ImageRecognitionService {
    
    /**
     * 识别图像内容
     *
     * @param base64ImageData Base64编码的图像数据
     * @return 识别结果，包含识别到的物体名称和置信度
     */
    Map<String, Object> recognizeImage(String base64ImageData);
}