package dev.modsweb.service;

import dev.modsweb.config.AppProperties;
import dev.modsweb.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path uploadDir;

    public LocalStorageService(AppProperties props) {
        this.uploadDir = Paths.get(props.getStorage().getUploadDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(uploadDir);
    }

    @Override
    public void save(String bucketPath, byte[] bytes) {
        Path target = resolve(bucketPath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "文件写入失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String bucketPath) {
        try {
            Files.deleteIfExists(resolve(bucketPath));
        } catch (IOException ignored) {
        }
    }

    @Override
    public String getDownloadUrl(String bucketPath) {
        return "/files/" + bucketPath;
    }

    @Override
    public boolean exists(String bucketPath) {
        return Files.exists(resolve(bucketPath));
    }

    /** 解析并校验 bucketPath 不越出 uploadDir（防止路径穿越） */
    public Path resolve(String bucketPath) {
        // {*path} 捕获的路径可能带前导 /，统一清洗；正斜杠统一成 Windows 分隔符
        String cleaned = bucketPath.replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        Path target = uploadDir.resolve(cleaned).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "非法文件路径");
        }
        return target;
    }

    public Path getUploadDir() {
        return uploadDir;
    }
}
