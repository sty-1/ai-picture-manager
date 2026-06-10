package com.nanhua.yupicturebackend.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oss.client")
@Data
public class OssClientConfig {

    /**
     * 访问端点（例如 oss-cn-hangzhou.aliyuncs.com）
     */
    private String endpoint;

    /**
     * 访问密钥 ID
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret
     */
    private String accessKeySecret;

    /**
     * 存储空间名称
     */
    private String bucketName;

    /**
     * 访问域名（用于拼接文件公网访问 URL，例如 https://<bucket>.<endpoint> 或自定义 CDN 域名）
     */
    private String host;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
