package dev.modsweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 依赖表（多对多）。
 * dependsOn 为空表示外部依赖（如 fabric-api），name 记录前置名；依赖图谱上画成外部节点。
 */
@Entity
@Table(name = "mod_dependencies")
@Getter
@Setter
public class ModDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mod_id", nullable = false)
    private Mod mod;

    /** 站内前置模组（可选），为空表示外部依赖 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_mod_id")
    private Mod dependsOn;

    /** 前置名：内部依赖用站内模组名，外部依赖用 fabric-api 这类标识 */
    @Column(length = 128)
    private String name;

    /** 依赖类型：depends(前置) / recommends(联动) */
    @Column(length = 16)
    private String type = "depends";
}
