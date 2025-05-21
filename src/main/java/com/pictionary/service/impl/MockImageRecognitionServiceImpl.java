package com.pictionary.service.impl;

import com.pictionary.service.ImageRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Arrays;
import java.util.List;

/**
 * 模拟图像识别服务实现
 * 用于开发和测试环境，不依赖外部API
 * 专门针对简笔画和线条画进行优化
 */
@Service
@Primary
@Slf4j
public class MockImageRecognitionServiceImpl implements ImageRecognitionService {

    private final Random random = new Random();
    
    // 可能的识别结果列表（简笔画常见对象）
    private final List<String> commonObjects = Arrays.asList(
        // 动物类
        "猫", "狗", "老虎", "狮子", "大象", "长颈鹿", "斑马", "熊猫", 
        "猴子", "兔子", "鹿", "鱼", "鸟", "鹰", "鸭子", "鹅", 
        "蛇", "乌龟", "青蛙", "蝴蝶",
        
        // 日常物品类
        "房子", "树", "花", "太阳", "月亮", "星星", "云", "雨",
        "伞", "汽车", "自行车", "飞机", "船", "火车",
        "苹果", "香蕉", "橙子", "西瓜", "草莓",
        "椅子", "桌子", "电视", "电脑", "手机",
        
        // 简单几何图形
        "圆形", "正方形", "三角形", "长方形", "五角星", "心形"
    );
    
    // 简笔画难以区分的对象组（相似对象）
    private final Map<String, List<String>> similarObjects = new HashMap<String, List<String>>() {{
        put("猫", Arrays.asList("狗", "老虎", "兔子"));
        put("鸟", Arrays.asList("鸭子", "鹅", "鹰"));
        put("圆形", Arrays.asList("太阳", "苹果", "橙子"));
        put("长方形", Arrays.asList("正方形", "房子", "电视"));
    }};
    
    @Override
    public Map<String, Object> recognizeImage(String base64ImageData) {
        log.info("使用模拟简笔画识别服务");
        
        // 模拟处理延迟
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 随机选择一个主要预测对象
        String mainPrediction = commonObjects.get(random.nextInt(commonObjects.size()));
        
        // 为简笔画生成更真实的置信度（通常简笔画识别的置信度较低）
        int mainConfidence = 50 + random.nextInt(40); // 50-89之间的随机数
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("prediction", mainPrediction);
        result.put("confidence", mainConfidence);
        result.put("mock", true);
        result.put("sketch_recognition", true); // 标记为简笔画识别
        
        // 添加可能的替代识别结果（相似对象）
        if (similarObjects.containsKey(mainPrediction)) {
            List<Map<String, Object>> alternatives = new java.util.ArrayList<>();
            List<String> similars = similarObjects.get(mainPrediction);
            
            // 添加1-3个相似对象作为替代结果
            int altCount = 1 + random.nextInt(Math.min(3, similars.size()));
            for (int i = 0; i < altCount; i++) {
                String altObject = similars.get(random.nextInt(similars.size()));
                int altConfidence = Math.max(20, mainConfidence - 10 - random.nextInt(30));
                
                Map<String, Object> alternative = new HashMap<>();
                alternative.put("name", altObject);
                alternative.put("score", altConfidence / 100.0);
                alternatives.add(alternative);
            }
            
            result.put("alternatives", alternatives);
        }
        
        log.debug("模拟简笔画识别结果: {} (置信度: {}%)", mainPrediction, mainConfidence);
        
        return result;
    }
    
    /**
     * 预处理简笔画图像（模拟方法）
     * 在实际实现中，这里可以添加图像增强、线条提取等处理
     * 
     * @param base64ImageData 图像数据
     * @return 处理后的图像数据
     */
    private String preprocessSketchImage(String base64ImageData) {
        // 模拟图像预处理
        // 在实际实现中，这里可以添加：
        // 1. 线条增强
        // 2. 背景去除
        // 3. 图像标准化
        // 4. 边缘检测和线条提取
        
        return base64ImageData; // 当前仅返回原始数据
    }
}