package dev.modsweb.repository;

import dev.modsweb.entity.ModVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModVersionRepository extends JpaRepository<ModVersion, Long> {

    /**
     * 列表聚合（切入点三）：按 mod_id 聚合，每个模组只返回「当前筛选条件下最新」的一个版本。
     * 用子查询挑出该 mod 下满足游戏版本/加载器条件的最新版本 id，外层再精确匹配那一行。
     * 返回 [mod_id, version_id] 对。
     */
    @Query(value = """
            SELECT mv.mod_id, mv.id
            FROM mod_versions mv
            JOIN mods m ON m.id = mv.mod_id
            WHERE (:gameVersion IS NULL OR mv.game_version = :gameVersion)
              AND (:loader IS NULL OR mv.mod_loader = :loader)
              AND (:category IS NULL OR m.category = :category)
              AND (:keyword IS NULL OR m.name LIKE '%' || :keyword || '%'
                   OR m.short_desc LIKE '%' || :keyword || '%')
              AND mv.id = (
                  SELECT mv2.id FROM mod_versions mv2
                  WHERE mv2.mod_id = mv.mod_id
                    AND (:gameVersion IS NULL OR mv2.game_version = :gameVersion)
                    AND (:loader IS NULL OR mv2.mod_loader = :loader)
                  ORDER BY mv2.release_date DESC, mv2.id DESC LIMIT 1
              )
            ORDER BY mv.release_date DESC, mv.id DESC
            """, nativeQuery = true)
    List<Object[]> findLatestVersions(@Param("gameVersion") String gameVersion,
                                      @Param("loader") String loader,
                                      @Param("category") String category,
                                      @Param("keyword") String keyword);

    @Query("SELECT v FROM ModVersion v JOIN FETCH v.mod WHERE v.id IN :ids")
    List<ModVersion> findWithModByIds(@Param("ids") Collection<Long> ids);

    List<ModVersion> findByModIdOrderByReleaseDateDesc(Long modId);

    List<ModVersion> findByModIdAndGameVersionOrderByReleaseDateDesc(Long modId, String gameVersion);

    Optional<ModVersion> findFirstByModIdAndRecommendedTrue(Long modId);

    /** 每个模组所有版本下载数之和，用于列表卡片显示总下载量 */
    @Query("SELECT v.mod.id, COALESCE(SUM(v.downloadCount), 0) FROM ModVersion v GROUP BY v.mod.id")
    List<Object[]> sumDownloadCountByMod();
}
