package dev.modsweb.service;

/**
 * 文件存储抽象（切入点二）。本地实现存 uploads/ 目录；
 * 以后切阿里云 OSS / 七牛，写一个 OssStorageService 实现并改 app.storage.type 即可，业务代码零改动。
 */
public interface StorageService {

    /** 保存文件，bucketPath 为相对路径，如 mods/advancedenchanting/advancedenchanting-1.0.0-1.21.4.jar */
    void save(String bucketPath, byte[] bytes);

    void delete(String bucketPath);

    /** 返回浏览器可直接访问的下载 URL；本地为 /files/{bucketPath}，OSS 为签名 URL */
    String getDownloadUrl(String bucketPath);

    boolean exists(String bucketPath);
}
