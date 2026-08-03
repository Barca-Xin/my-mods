package dev.modsweb.dto;

/** 版本信息（详情页版本切换器 / 列表最新版用） */
public record VersionDto(
        Long id,
        String gameVersion,
        String modVersion,
        String modLoader,
        Long fileSize,
        String md5,
        String changelog,
        String releaseDate,
        boolean recommended,
        String downloadUrl,
        long downloadCount
) {
}
