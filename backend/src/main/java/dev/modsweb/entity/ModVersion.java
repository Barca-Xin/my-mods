package dev.modsweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 版本表，与模组主表分开（切入点一）。
 * 复合索引 (game_version, mod_loader) 支撑「我的游戏是 1.20.1 + Forge 有哪些模组」的检索（切入点三）。
 */
@Entity
@Table(name = "mod_versions", indexes = {
        @Index(name = "idx_mv_game_loader", columnList = "game_version, mod_loader"),
        @Index(name = "idx_mv_mod_game", columnList = "mod_id, game_version")
})
@Getter
@Setter
public class ModVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mod_id", nullable = false)
    private Mod mod;

    @Column(name = "game_version", nullable = false, length = 16)
    private String gameVersion;

    @Column(name = "mod_version", nullable = false, length = 32)
    private String modVersion;

    @Column(name = "mod_loader", length = 32)
    private String modLoader;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(length = 64)
    private String md5;

    @Column(columnDefinition = "TEXT")
    private String changelog;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(name = "is_recommended")
    private boolean recommended;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "file_record_id")
    private FileRecord fileRecord;

    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @PrePersist
    public void prePersist() {
        if (this.downloadCount == null) {
            this.downloadCount = 0L;
        }
    }
}
