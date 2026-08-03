package dev.modsweb.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record ModRequest(
        @NotBlank(message = "slug 不能为空") String slug,
        @NotBlank(message = "名称不能为空") String name,
        String logoUrl,
        String shortDesc,
        String category,
        String modLoader,
        String sourceCodeUrl,
        String wikiUrl,
        String autoSyncSource
) {
}
