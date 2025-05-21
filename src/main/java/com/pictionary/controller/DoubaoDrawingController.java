package com.pictionary.controller;

import com.pictionary.service.ImageRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 豆包API画布识别控制器
 * 处理前端发送的画布数据并返回豆包AI识别结果
 */
@RestController
@RequestMapping("/api/drawing/doubao")
@Slf4j
public class DoubaoDrawingController {

    @Autowired
    @Qualifier("doubaoImageRecognitionService")
    private ImageRecognitionService imageRecognitionService;

    /**
     * 接收画布数据并使用豆包AI进行识别
     *
     * @param requestData 包含base64编码的画布图像数据
     * @return AI识别结果
     */
    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognizeDrawing(@RequestBody Map<String, String> requestData) {
        try {
            String imageData = requestData.get("imageData");
            if (imageData == null || imageData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "画布数据不能为空"
                ));
            }

            // 去除可能的Data URL前缀
            if (imageData.startsWith("data:image")) {
                imageData = imageData.substring(imageData.indexOf(",") + 1);
            }

            // 调用豆包服务进行图像识别
            Map<String, Object> result = imageRecognitionService.recognizeImage(imageData);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("豆包图像识别过程中发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "识别过程中发生错误: " + e.getMessage()
            ));
        }
    }
}