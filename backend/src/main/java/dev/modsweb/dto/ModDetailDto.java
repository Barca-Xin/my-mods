package dev.modsweb.dto;

import java.util.List;

/** 详情页：Mod 完整信息 + 全部历史版本（切入点四的版本切换器数据源） */
public record ModDetailDto(
        Long modId,
        String slug,
        String name,
        String logoUrl,
        String shortDesc,
        String category,
        String modLoader,
        String sourceCodeUrl,
        String wikiUrl,
        String declaration,
        boolean hasDeclaration,
        List<VersionDto> versions
) {
}
