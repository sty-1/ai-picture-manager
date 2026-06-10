package com.nanhua.yupicturebackend.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.aliyun.oss.model.OSSObject;
import com.nanhua.yupicturebackend.annotation.AuthCheck;
import com.nanhua.yupicturebackend.common.BaseResponse;
import com.nanhua.yupicturebackend.common.ResultUtils;
import com.nanhua.yupicturebackend.config.OssClientConfig;
import com.nanhua.yupicturebackend.constant.UserConstant;
import com.nanhua.yupicturebackend.exception.BusinessException;
import com.nanhua.yupicturebackend.exception.ErrorCode;
import com.nanhua.yupicturebackend.manager.OssManager;
import com.nanhua.yupicturebackend.model.entity.User;
import com.nanhua.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private OssManager ossManager;

    @Resource
    private OssClientConfig ossClientConfig;

    @Resource
    private UserService userService;

    /**
     * 通用文件上传（返回公网访问 URL）
     *
     * @param file 文件
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile file,
                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String uploadPath = String.format("avatar/%s/%s.%s",
                loginUser.getId(), UUID.randomUUID().toString().replace("-", ""), suffix);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(uploadPath, null);
            file.transferTo(tempFile);
            ossManager.putObject(uploadPath, tempFile);
        } catch (Exception e) {
            log.error("file upload error, uploadPath = " + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (tempFile != null) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.error("temp file delete error, path = {}", tempFile.getAbsolutePath());
                }
            }
        }
        String url = ossClientConfig.getHost() + "/" + uploadPath;
        return ResultUtils.success(url);
    }

    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            ossManager.putObject(filepath, file);
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        InputStream objectInput = null;
        try {
            OSSObject ossObject = ossManager.getObject(filepath);
            objectInput = ossObject.getObjectContent();
            byte[] bytes = IoUtil.readBytes(objectInput);
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (objectInput != null) {
                objectInput.close();
            }
        }
    }

    /**
     * 图片代理（解决 vue-cropper 的 crossOrigin 导致的 CORS 问题）
     * 后端代理 COS 图片，返回给前端时不涉及跨域限制
     */
    @GetMapping("/image/proxy")
    public void proxyImage(@RequestParam String url, HttpServletResponse response) throws IOException {
        String decodedUrl = URLDecoder.decode(url, "UTF-8");
        InputStream objectInput = null;
        OutputStream outputStream = null;
        try {
            String key = ossManager.extractKey(decodedUrl);
            OSSObject ossObject = ossManager.getObject(key);
            objectInput = ossObject.getObjectContent();
            // 根据文件扩展名设置 Content-Type
            String contentType = "image/png";
            String lowerKey = key.toLowerCase();
            if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lowerKey.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (lowerKey.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (lowerKey.endsWith(".bmp")) {
                contentType = "image/bmp";
            } else if (lowerKey.endsWith(".svg")) {
                contentType = "image/svg+xml";
            }
            response.setContentType(contentType);
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = objectInput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            log.error("image proxy error, url = " + decodedUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片代理失败");
        } finally {
            if (objectInput != null) {
                objectInput.close();
            }
        }
    }
}
