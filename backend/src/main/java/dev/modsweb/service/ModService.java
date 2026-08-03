package dev.modsweb.service;

import dev.modsweb.dto.DependencyGraphDto;
import dev.modsweb.dto.ModDetailDto;
import dev.modsweb.dto.ModListItemDto;
import dev.modsweb.dto.PageResult;
import dev.modsweb.dto.VersionDto;
import dev.modsweb.entity.Mod;
import dev.modsweb.entity.ModDependency;
import dev.modsweb.entity.ModVersion;
import dev.modsweb.exception.ApiException;
import dev.modsweb.repository.ModDependencyRepository;
import dev.modsweb.repository.ModRepository;
import dev.modsweb.repository.ModVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModService {

    private final ModRepository modRepository;
    private final ModVersionRepository versionRepository;
    private final ModDependencyRepository dependencyRepository;
    private final StorageService storageService;

    public ModService(ModRepository modRepository, ModVersionRepository versionRepository,
                      ModDependencyRepository dependencyRepository, StorageService storageService) {
        this.modRepository = modRepository;
        this.versionRepository = versionRepository;
        this.dependencyRepository = dependencyRepository;
        this.storageService = storageService;
    }

    /**
     * 列表接口（切入点三）：后端已按 mod_id 聚合出每个模组的最新版，前端拿到的就是去重后的卡片数据。
     */
    @Transactional(readOnly = true)
    public PageResult<ModListItemDto> listMods(String gameVersion, String loader, String category,
                                               String keyword, int page, int size) {
        List<Object[]> rows = versionRepository.findLatestVersions(
                blankToNull(gameVersion), blankToNull(loader), blankToNull(category), blankToNull(keyword));
        long total = rows.size();
        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + size, rows.size());

        List<Long> pageIds = rows.subList(from, to).stream()
                .map(r -> ((Number) r[1]).longValue())
                .toList();
        Map<Long, ModVersion> byId = pageIds.isEmpty() ? Map.of()
                : versionRepository.findWithModByIds(pageIds).stream()
                        .collect(Collectors.toMap(ModVersion::getId, Function.identity()));

        Map<Long, Long> downloadSum = versionRepository.sumDownloadCountByMod().stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()));

        List<ModListItemDto> items = pageIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(v -> toListItem(v, downloadSum.getOrDefault(v.getMod().getId(), 0L)))
                .toList();
        return PageResult.of(items, total, page, size);
    }

    @Transactional(readOnly = true)
    public ModDetailDto getDetail(String slug) {
        Mod mod = getModBySlug(slug);
        List<ModVersion> versions = versionRepository.findByModIdOrderByReleaseDateDesc(mod.getId());
        return new ModDetailDto(mod.getId(), mod.getSlug(), mod.getName(), mod.getLogoUrl(),
                mod.getShortDesc(), mod.getCategory(), mod.getModLoader(),
                mod.getSourceCodeUrl(), mod.getWikiUrl(),
                versions.stream().map(this::toVersionDto).toList());
    }

    /** 详情页版本切换器数据（切入点四）：可按游戏版本过滤 */
    @Transactional(readOnly = true)
    public List<VersionDto> getVersions(String slug, String gameVersion) {
        Mod mod = getModBySlug(slug);
        List<ModVersion> versions = StringUtils.hasText(gameVersion)
                ? versionRepository.findByModIdAndGameVersionOrderByReleaseDateDesc(mod.getId(), gameVersion)
                : versionRepository.findByModIdOrderByReleaseDateDesc(mod.getId());
        return versions.stream().map(this::toVersionDto).toList();
    }

    /** 依赖图谱（切入点六）：当前 Mod 居中，前置/联动节点辐条相连 */
    @Transactional(readOnly = true)
    public DependencyGraphDto getDependencies(String slug) {
        Mod mod = getModBySlug(slug);
        List<ModDependency> deps = dependencyRepository.findByModId(mod.getId());
        List<DependencyGraphDto.NodeDto> nodes = new ArrayList<>();
        List<DependencyGraphDto.EdgeDto> edges = new ArrayList<>();

        String selfId = "mod:" + mod.getId();
        nodes.add(new DependencyGraphDto.NodeDto(selfId, mod.getName(), "mod", mod.getSlug()));
        for (ModDependency dep : deps) {
            Mod target = dep.getDependsOn();
            String id = target != null ? "mod:" + target.getId() : "ext:" + dep.getName();
            String label = target != null ? target.getName() : dep.getName();
            nodes.add(new DependencyGraphDto.NodeDto(id, label,
                    target != null ? "mod" : "external", target != null ? target.getSlug() : null));
            edges.add(new DependencyGraphDto.EdgeDto(selfId, id));
        }
        return new DependencyGraphDto(nodes, edges);
    }

    /** 后台 Mod 列表：所有 Mod 及其全部版本 */
    @Transactional(readOnly = true)
    public List<ModDetailDto> listAllForAdmin() {
        return modRepository.findAll().stream()
                .map(m -> {
                    List<ModVersion> versions = versionRepository.findByModIdOrderByReleaseDateDesc(m.getId());
                    return new ModDetailDto(m.getId(), m.getSlug(), m.getName(), m.getLogoUrl(),
                            m.getShortDesc(), m.getCategory(), m.getModLoader(),
                            m.getSourceCodeUrl(), m.getWikiUrl(),
                            versions.stream().map(this::toVersionDto).toList());
                })
                .sorted(Comparator.comparing(ModDetailDto::modId))
                .toList();
    }

    public Mod getModBySlug(String slug) {
        return modRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("模组不存在: " + slug));
    }

    public Mod getModById(Long id) {
        return modRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("模组不存在"));
    }

    private ModListItemDto toListItem(ModVersion v, long totalDownloads) {
        Mod m = v.getMod();
        return new ModListItemDto(m.getId(), m.getSlug(), m.getName(), m.getLogoUrl(),
                m.getShortDesc(), m.getCategory(), m.getModLoader(), totalDownloads, toVersionDto(v));
    }

    public VersionDto toVersionDto(ModVersion v) {
        String downloadUrl = v.getFileRecord() != null
                ? storageService.getDownloadUrl(v.getFileRecord().getBucketPath())
                : null;
        return new VersionDto(v.getId(), v.getGameVersion(), v.getModVersion(), v.getModLoader(),
                v.getFileSize(), v.getMd5(), v.getChangelog(),
                v.getReleaseDate() == null ? null : v.getReleaseDate().toString(),
                v.isRecommended(),
                downloadUrl,
                v.getDownloadCount() == null ? 0L : v.getDownloadCount());
    }

    static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
