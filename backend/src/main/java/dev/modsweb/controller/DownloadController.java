package dev.modsweb.controller;

import dev.modsweb.service.DownloadService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/downloads")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    /** 下载即统计（切入点六）：前端先 POST 计数，再跳转到返回的下载 URL */
    @PostMapping("/{versionId}")
    public Map<String, Object> registerDownload(@PathVariable Long versionId) {
        return downloadService.registerDownload(versionId);
    }
}
