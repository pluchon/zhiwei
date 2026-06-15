package com.campus.system.service.interfaces;

import java.io.InputStream;
import java.net.URL;

/**
 * OSS 文件服务接口。
 */
public interface OssService {

    String upload(String extension, InputStream input);

    URL signedUrl(String key);

    void delete(String key);
}
