package dev.modsweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模组主表（切入点一）。slug 为 URL 友好标识，对应 fabric.mod.json 的 modId。
 */
@Entity
@Table(name = "mods", indexes = {
        @Index(name = "idx_mod_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
public class Mod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "short_desc", length = 1024)
    private String shortDesc;

    @Column(length = 32)
    private String category;

    @Column(name = "mod_loader", length = 32)
    private String modLoader;

    @Column(name = "source_code_url", length = 512)
    private String sourceCodeUrl;

    @Column(name = "wiki_url", length = 512)
    private String wikiUrl;

    /** 预留自动化同步口子（如 GitHub Release webhook），首版手工上传可为空 */
    @Column(name = "auto_sync_source", length = 512)
    private String autoSyncSource;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "mod", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "mod", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModDependency> dependencies = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
