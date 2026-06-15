package com.campus.system.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.service.interfaces.OssService;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// 阿里云 OSS 文件服务实现。
@Service
public class OssServiceImpl implements OssService {

    @Value("${campus.aliyun.access-key-id:}")
    private String keyId;

    @Value("${campus.aliyun.access-key-secret:}")
    private String keySecret;

    @Value("${campus.aliyun.oss-endpoint:}")
    private String endpoint;

    @Value("${campus.aliyun.oss-bucket:}")
    private String bucket;

    // 上传文件
    @Override
    public String upload(String extension, InputStream input) {
        OSS oss = client();
        String key = "campus_picture/" + UUID.randomUUID() + "." + extension;
        try {
            oss.putObject(bucket, key, input);
            return key;
        } finally {
            oss.shutdown();
        }
    }

    // 提供对外访问URL
    @Override
    public URL signedUrl(String key) {
        OSS oss = client();
        try {
            return oss.generatePresignedUrl(bucket, key, new Date(System.currentTimeMillis() + Duration.ofMinutes(15).toMillis()));
        } finally {
            oss.shutdown();
        }
    }

    @Override
    public void delete(String key) {
        OSS oss = client();
        try {
            oss.deleteObject(bucket, key);
        } finally {
            oss.shutdown();
        }
    }

    // 链接阿里云OSS客户端
    private OSS client() {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket) || !StringUtils.hasText(keyId) || !StringUtils.hasText(keySecret)) {
            throw BusinessException.conflict("阿里云 OSS 尚未配置");
        }
        return new OSSClientBuilder().build(endpoint, keyId, keySecret);
    }
}
