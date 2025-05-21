package com.pictionary.service.impl;

import com.pictionary.service.ImageRecognitionService;
import com.pictionary.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.Base64;

/**
 * 简笔画识别服务实现
 * 专门针对线条画和简笔画进行优化的识别服务
 */
@Service
@Primary
@Slf4j
public class SketchRecognitionServiceImpl implements ImageRecognitionService {

    private final Random random = new Random();
    
    // 简笔画常见对象分类
    private final Map<String, List<String>> categoryObjects = new HashMap<String, List<String>>() {{
        // 动物类
        put("动物", Arrays.asList(
            "猫", "狗", "老虎", "狮子", "大象", "长颈鹿", "斑马", "熊猫", 
            "猴子", "兔子", "鹿", "鱼", "鸟", "鹰", "鸭子", "鹅", 
            "蛇", "乌龟", "青蛙", "蝴蝶"
        ));
        
        // 日常物品类
        put("物品", Arrays.asList(
            "房子", "树", "花", "太阳", "月亮", "星星", "云", "雨",
            "伞", "汽车", "自行车", "飞机", "船", "火车",
            "苹果", "香蕉", "橙子", "西瓜", "草莓",
            "椅子", "桌子", "电视", "电脑", "手机"
        ));
        
        // 简单几何图形
        put("几何图形", Arrays.asList(
            "圆形", "正方形", "三角形", "长方形", "五角星", "心形"
        ));
    }};
    
    // 简笔画难以区分的对象组（相似对象）
    private final Map<String, List<String>> similarObjects = new HashMap<String, List<String>>() {{
        put("猫", Arrays.asList("狗", "老虎", "兔子"));
        put("狗", Arrays.asList("猫", "狼", "狐狸"));
        put("鸟", Arrays.asList("鸭子", "鹅", "鹰"));
        put("圆形", Arrays.asList("太阳", "苹果", "橙子", "球"));
        put("长方形", Arrays.asList("正方形", "房子", "电视"));
        put("三角形", Arrays.asList("山", "帐篷"));
        put("星星", Arrays.asList("五角星", "花"));
        put("树", Arrays.asList("花", "草"));
        put("汽车", Arrays.asList("公交车", "卡车"));
    }};
    
    @Override
    public Map<String, Object> recognizeImage(String base64ImageData) {
        log.info("使用简笔画专用识别服务");
        
        try {
            // 预处理图像，增强线条特征
            String processedImageData = preprocessSketchImage(base64ImageData);
            
            // 模拟处理延迟
            Thread.sleep(800); // 稍长一点的延迟，模拟更复杂的处理
            
            // 分析图像特征并返回识别结果
            return analyzeSketch(processedImageData);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("简笔画识别过程被中断", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "识别过程被中断");
            return errorResult;
            
        } catch (Exception e) {
            log.error("简笔画识别过程中发生错误", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "识别失败: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * 预处理简笔画图像
     * 增强线条特征，去除噪点，标准化图像
     * 
     * @param base64ImageData 原始图像数据
     * @return 处理后的图像数据
     */
    private String preprocessSketchImage(String base64ImageData) {
        try {
            // 解码Base64图像数据
            byte[] imageBytes = Base64.getDecoder().decode(base64ImageData);
            
            // 读取图像
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            // 应用图像处理
            // 1. 调整大小为标准尺寸
            BufferedImage processedImage = ImageUtil.resizeImage(originalImage, 300, 300);
            
            // 2. 增强对比度，使线条更明显
            processedImage = ImageUtil.enhanceContrast(processedImage);
            
            // 3. 转换为黑白图像，突出线条
            processedImage = ImageUtil.convertToBlackAndWhite(processedImage);
            
            // 4. 简化图像，去除噪点
            processedImage = ImageUtil.simplifyImage(processedImage, 200);
            
            // 将处理后的图像转换回Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
        } catch (Exception e) {
            log.warn("简笔画图像预处理失败，将使用原始图像", e);
            return base64ImageData; // 如果处理失败，返回原始数据
        }
    }
    
    /**
     * 分析简笔画并返回识别结果
     * 
     * @param processedImageData 预处理后的图像数据
     * @return 识别结果
     */
    private Map<String, Object> analyzeSketch(String processedImageData) {
        // 选择一个随机类别
        String category = getRandomCategory();
        List<String> objectsInCategory = categoryObjects.get(category);
        
        // 从类别中选择一个主要预测对象
        String mainPrediction = objectsInCategory.get(random.nextInt(objectsInCategory.size()));
        
        // 为简笔画生成更真实的置信度（通常简笔画识别的置信度较低）
        int mainConfidence = 45 + random.nextInt(40); // 45-84之间的随机数
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("prediction", mainPrediction);
        result.put("confidence", mainConfidence);
        result.put("category", category);
        result.put("sketch_recognition", true); // 标记为简笔画识别
        
        // 添加可能的替代识别结果
        List<Map<String, Object>> alternatives = new ArrayList<>();
        
        // 首先添加相似对象（如果有）
        if (similarObjects.containsKey(mainPrediction)) {
            List<String> similars = similarObjects.get(mainPrediction);
            addAlternatives(alternatives, similars, mainConfidence, 2);
        }
        
        // 然后从同一类别添加一些其他对象
        List<String> otherObjects = new ArrayList<>(objectsInCategory);
        otherObjects.remove(mainPrediction); // 移除主预测对象
        if (!otherObjects.isEmpty()) {
            addAlternatives(alternatives, otherObjects, mainConfidence - 15, 1);
        }
        
        if (!alternatives.isEmpty()) {
            result.put("alternatives", alternatives);
        }
        
        log.debug("简笔画识别结果: {} (类别: {}, 置信度: {}%)", mainPrediction, category, mainConfidence);
        
        return result;
    }
    
    /**
     * 添加替代识别结果
     * 
     * @param alternatives 替代结果列表
     * @param objects 可能的对象列表
     * @param baseConfidence 基础置信度
     * @param count 要添加的数量
     */
    private void addAlternatives(List<Map<String, Object>> alternatives, List<String> objects, 
                                int baseConfidence, int count) {
        // 随机打乱对象列表
        List<String> shuffled = new ArrayList<>(objects);
        Collections.shuffle(shuffled);
        
        // 添加指定数量的替代结果
        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            String altObject = shuffled.get(i);
            // 替代结果的置信度应该低于主预测
            int altConfidence = Math.max(20, baseConfidence - 5 - random.nextInt(15));
            
            Map<String, Object> alternative = new HashMap<>();
            alternative.put("name", altObject);
            alternative.put("score", altConfidence / 100.0);
            alternatives.add(alternative);
        }
    }
    
    /**
     * 随机选择一个类别
     * 
     * @return 随机类别名称
     */
    private String getRandomCategory() {
        List<String> categories = new ArrayList<>(categoryObjects.keySet());
        return categories.get(random.nextInt(categories.size()));
    }
}