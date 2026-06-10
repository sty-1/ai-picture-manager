package com.nanhua.yupicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.nanhua.yupicturebackend.config.OssClientConfig;
import com.nanhua.yupicturebackend.exception.BusinessException;
import com.nanhua.yupicturebackend.exception.ErrorCode;
import com.nanhua.yupicturebackend.manager.OssManager;
import com.nanhua.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private OssClientConfig ossClientConfig;

    @Resource
    private OssManager ossManager;

    private static final Set<String> ALLOWED_FORMATS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));

    /**
     * 上传图片
     *
     * @param inputSource      文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);
        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginFilename(inputSource);
        String suffix = FileUtil.getSuffix(originalFilename).toLowerCase();
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3. 创建临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSource, file);
            // 4. 读取图片信息（宽高、格式、主色调）
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取图片信息");
            }
            int picWidth = image.getWidth();
            int picHeight = image.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            String picColor = getAverageColor(image);
            // 当 URL 无法提取到有效后缀时（如 Bing 爬取的图片），通过文件头探测真实格式
            if (!ALLOWED_FORMATS.contains(suffix)) {
                String detected = detectFormat(file);
                if (detected != null) {
                    suffix = detected;
                    uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
                    uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
                }
            }
            // 5. 上传原图到 OSS
            ossManager.putObject(uploadPath, file);
            // 6. 生成并上传缩略图
            String thumbnailKey = FileUtil.mainName(uploadPath) + "_thumbnail." + suffix;
            File thumbnailFile = null;
            try {
                thumbnailFile = File.createTempFile("thumb_" + uuid, "." + suffix);
                BufferedImage thumbnailImage = createThumbnail(image, 256, 256);
                if (!ImageIO.write(thumbnailImage, suffix, thumbnailFile)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缩略图生成失败");
                }
                ossManager.putObject(thumbnailKey, thumbnailFile);
            } finally {
                this.deleteTempFile(thumbnailFile);
            }
            // 7. 封装返回结果
            return buildResult(originalFilename, file, uploadPath, thumbnailKey,
                    picWidth, picHeight, picScale, suffix, picColor);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 8. 临时文件清理
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 生成缩略图（等比缩放，不超过 maxWidth x maxHeight）
     */
    private BufferedImage createThumbnail(BufferedImage original, int maxWidth, int maxHeight) {
        int w = original.getWidth();
        int h = original.getHeight();
        double ratio = Math.min(maxWidth * 1.0 / w, maxHeight * 1.0 / h);
        if (ratio >= 1.0 && w <= maxWidth && h <= maxHeight) {
            return original;
        }
        int newW = (int) (w * ratio);
        int newH = (int) (h * ratio);
        BufferedImage thumbnail = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original.getScaledInstance(newW, newH, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return thumbnail;
    }

    /**
     * 计算图片平均颜色（十六进制）
     */
    private String getAverageColor(BufferedImage image) {
        // 缩小图片加速计算
        BufferedImage scaled = createThumbnail(image, 100, 100);
        long sumR = 0, sumG = 0, sumB = 0;
        int w = scaled.getWidth();
        int h = scaled.getHeight();
        int total = w * h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = scaled.getRGB(x, y);
                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
            }
        }
        int avgR = (int) (sumR / total);
        int avgG = (int) (sumG / total);
        int avgB = (int) (sumB / total);
        return String.format("#%02x%02x%02x", avgR, avgG, avgB);
    }

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(String originalFilename, File file, String uploadPath,
                                             String thumbnailKey, int picWidth, int picHeight,
                                             double picScale, String picFormat, String picColor) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(ossClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setThumbnailUrl(ossClientConfig.getHost() + "/" + thumbnailKey);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(picFormat);
        uploadPictureResult.setPicColor(picColor);
        return uploadPictureResult;
    }

    /**
     * 清理临时文件
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

    /**
     * 通过文件头魔术字节探测图片真实格式
     */
    private String detectFormat(File file) {
        byte[] header = new byte[12];
        int read = 0;
        try (InputStream in = new FileInputStream(file)) {
            read = in.read(header);
        } catch (IOException e) {
            return null;
        }
        if (read < 3) return null;
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return "jpg";
        if (read >= 4 && header[0] == (byte) 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') return "png";
        if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return "webp";
        if (read >= 4 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F') return "gif";
        return null;
    }
}
