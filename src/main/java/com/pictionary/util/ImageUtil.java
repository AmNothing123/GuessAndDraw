package com.pictionary.util;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 图像处理工具类
 */
public class ImageUtil {

    /**
     * 调整图像大小
     *
     * @param originalImage 原始图像
     * @param targetWidth   目标宽度
     * @param targetHeight  目标高度
     * @return 调整大小后的图像
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resizedImage.createGraphics();

        // 设置高质量缩放
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制调整大小后的图像
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();

        return resizedImage;
    }

    /**
     * 增强图像对比度
     *
     * @param image 原始图像
     * @return 对比度增强后的图像
     */
    public static BufferedImage enhanceContrast(BufferedImage image) {
        BufferedImage enhancedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        // 对比度增强因子 (1.2 表示增强20%)
        float factor = 1.2f;

        // 对每个像素点进行处理
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);

                // 获取RGB值
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();
                int alpha = color.getAlpha();

                // 计算新的RGB值，增强对比度
                red = Math.min(255, Math.max(0, Math.round((red - 128) * factor + 128)));
                green = Math.min(255, Math.max(0, Math.round((green - 128) * factor + 128)));
                blue = Math.min(255, Math.max(0, Math.round((blue - 128) * factor + 128)));

                // 设置新的颜色
                Color newColor = new Color(red, green, blue, alpha);
                enhancedImage.setRGB(x, y, newColor.getRGB());
            }
        }

        return enhancedImage;
    }

    /**
     * 转换图像为黑白
     *
     * @param image 原始图像
     * @return 黑白图像
     */
    public static BufferedImage convertToBlackAndWhite(BufferedImage image) {
        BufferedImage bwImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = bwImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return bwImage;
    }

    /**
     * 简化图像，移除噪点
     *
     * @param image 原始图像
     * @param threshold 阈值 (0-255)
     * @return 简化后的图像
     */
    public static BufferedImage simplifyImage(BufferedImage image, int threshold) {
        BufferedImage simplifiedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);

                // 如果透明度为0，则跳过
                if (color.getAlpha() == 0) {
                    simplifiedImage.setRGB(x, y, color.getRGB());
                    continue;
                }

                // 计算亮度
                int brightness = (color.getRed() + color.getGreen() + color.getBlue()) / 3;

                // 根据阈值决定黑白
                if (brightness > threshold) {
                    // 白色 (透明)
                    simplifiedImage.setRGB(x, y, new Color(0, 0, 0, 0).getRGB());
                } else {
                    // 黑色
                    simplifiedImage.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        return simplifiedImage;
    }
}