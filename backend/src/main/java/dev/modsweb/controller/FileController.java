package dev.modsweb.controller;

import dev.modsweb.exception.ApiException;
import dev.modsweb.service.LocalStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地存储文件下载（v1 本地实现专属）。
 * 以后切 OSS 后此控制器弃用：POST /api/downloads 直接返回 OSS 签名 URL，浏览器直达对象存储。
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private final LocalStorageService localStorageService;

    public FileController(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    @GetMapping("/{*path}")
    public ResponseEntity<Resource> getFile(@PathVariable String path) {
        Path file = localStorageService.resolve(path);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw ApiException.notFound("文件不存在");
        }
        String filename = file.getFileName().toString();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(file))
                    .body(new FileSystemResource(file));
        } catch (IOException e) {
            throw ApiException.notFound("文件读取失败");
        }
    }
}
