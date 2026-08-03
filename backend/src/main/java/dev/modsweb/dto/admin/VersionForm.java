package dev.modsweb.dto.admin;

import java.util.List;

/**
 * 版本上传表单（multipart 的普通字段部分，jar 文件单独传）。
 * releaseDate 接受 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss；dependencies 为重复参数。
 */
public record VersionForm(
        String modVersion,
        String gameVersion,
        String modLoader,
        String changelog,
        String releaseDate,
        Boolean recommended,
        List<String> dependencies
) {
}
