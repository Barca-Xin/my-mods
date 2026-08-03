package dev.modsweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通用文件记录表（切入点二）：存 file_name / md5 / size / bucket_path / download_count。
 * 不只模组 jar 用，以后用户头像、截图都能复用。
 */
@Entity
@Table(name = "file_records")
@Getter
@Setter
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(length = 64)
    private String md5;

    @Column
    private Long size;

    /** 对象存储 / 本地目录下的相对路径 */
    @Column(name = "bucket_path", nullable = false)
    private String bucketPath;

    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.downloadCount == null) {
            this.downloadCount = 0L;
        }
    }
}
