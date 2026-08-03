package dev.modsweb.dto;

/** 列表卡片项：Mod 基本信息 + 其「最新版」下载信息（切入点三聚合结果） */
public record ModListItemDto(
        Long modId,
        String slug,
        String name,
        String logoUrl,
        String shortDesc,
        String category,
        String modLoader,
        long downloadCount,
        boolean hasDeclaration,
        VersionDto latestVersion
) {
}
