package dev.modsweb.service;

import dev.modsweb.entity.FileRecord;
import dev.modsweb.entity.ModVersion;
import dev.modsweb.exception.ApiException;
import dev.modsweb.repository.FileRecordRepository;
import dev.modsweb.repository.ModVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DownloadService {

    private final ModVersionRepository versionRepository;
    private final FileRecordRepository fileRecordRepository;
    private final StorageService storageService;

    public DownloadService(ModVersionRepository versionRepository, FileRecordRepository fileRecordRepository,
                           StorageService storageService) {
        this.versionRepository = versionRepository;
        this.fileRecordRepository = fileRecordRepository;
        this.storageService = storageService;
    }

    /**
     * 下载计数（切入点六）：先自增计数，再返回真实下载 URL，前端再跳转。
     */
    @Transactional
    public Map<String, Object> registerDownload(Long versionId) {
        ModVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> ApiException.notFound("版本不存在"));
        FileRecord file = version.getFileRecord();
        if (file == null) {
            throw ApiException.notFound("该版本暂无下载文件");
        }
        if (!storageService.exists(file.getBucketPath())) {
            throw ApiException.notFound("下载文件已不存在");
        }
        file.setDownloadCount(file.getDownloadCount() + 1);
        fileRecordRepository.save(file);
        version.setDownloadCount(version.getDownloadCount() + 1);
        versionRepository.save(version);
        return Map.of("downloadUrl", storageService.getDownloadUrl(file.getBucketPath()));
    }
}
