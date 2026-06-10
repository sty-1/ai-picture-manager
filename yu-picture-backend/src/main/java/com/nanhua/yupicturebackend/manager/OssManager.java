package com.nanhua.yupicturebackend.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.nanhua.yupicturebackend.config.OssClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

@Component
public class OssManager {

    @Resource
    private OssClientConfig ossClientConfig;

    @Resource
    private OSS ossClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(ossClientConfig.getBucketName(), key, file);
        return ossClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public OSSObject getObject(String key) {
        return ossClient.getObject(ossClientConfig.getBucketName(), key);
    }

    /**
     * 删除对象（支持传入完整 URL 或 OSS key）
     *
     * @param key 唯一键或完整 URL
     */
    public void deleteObject(String key) {
        // 如果传入的是完整 URL，提取路径部分作为 key
        String ossKey = extractKey(key);
        ossClient.deleteObject(ossClientConfig.getBucketName(), ossKey);
    }

    /**
     * 从完整 URL 中提取 OSS key
     */
    public String extractKey(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // 移除 host 前缀
            String host = ossClientConfig.getHost();
            String path;
            if (url.startsWith(host)) {
                path = url.substring(host.length());
            } else {
                // 如果 host 不匹配，尝试通过 URL 解析
                try {
                    java.net.URL parsed = new java.net.URL(url);
                    path = parsed.getPath();
                } catch (Exception ignored) {
                    path = url;
                }
            }
            // 去掉前导 /，OSS key 不以 / 开头
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        }
        return url;
    }
}
