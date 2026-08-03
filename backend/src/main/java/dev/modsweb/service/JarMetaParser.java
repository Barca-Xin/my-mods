package dev.modsweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.modsweb.exception.ApiException;
import org.springframework.stereotype.Component;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * jar 元数据解析器（切入点五）。
 * 读取 jar 包内的 fabric.mod.json（Fabric）或 META-INF/mods.toml（Forge/NeoForge），
 * 提取 modId / version / displayName 以及依赖，供后台发布时自动回填表单。
 */
@Component
public class JarMetaParser {

    public record JarMeta(String modId, String version, String name, String gameVersion, List<String> dependencies) {
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public JarMeta parse(byte[] bytes) {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();
                if ("fabric.mod.json".equals(name)) {
                    return parseFabric(readAll(zin));
                }
                if ("META-INF/mods.toml".equals(name)) {
                    return parseForge(readAll(zin));
                }
            }
        } catch (IOException e) {
            throw ApiException.badRequest("无法读取 jar 文件");
        }
        throw ApiException.badRequest("未识别的 Mod 文件格式：缺少 fabric.mod.json 或 META-INF/mods.toml");
    }

    private JarMeta parseFabric(byte[] bytes) throws IOException {
        JsonNode root = mapper.readTree(bytes);
        String modId = root.path("id").asText("");
        String version = root.path("version").asText("");
        String name = root.path("name").asText(modId);
        List<String> deps = new ArrayList<>();
        JsonNode depends = root.path("depends");
        String gameVersion = "";
        if (depends.isObject()) {
            depends.fieldNames().forEachRemaining(deps::add);
            // depends.minecraft 通常是 "~1.21.4" 之类的版本范围，提取游戏版本用于回填
            JsonNode mc = depends.path("minecraft");
            if (!mc.isMissingNode()) {
                String range = mc.isTextual() ? mc.asText() : mc.toString();
                gameVersion = extractVersion(range);
            }
        }
        if (modId.isEmpty()) {
            throw ApiException.badRequest("fabric.mod.json 缺少 id 字段");
        }
        return new JarMeta(modId, version, name, gameVersion, deps);
    }

    private JarMeta parseForge(byte[] bytes) {
        TomlParseResult result;
        try {
            result = Toml.parse(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw ApiException.badRequest("mods.toml 读取失败");
        }
        if (result.hasErrors()) {
            throw ApiException.badRequest("mods.toml 解析失败: " + result.errors().toString());
        }
        TomlArray mods = result.getArray("mods");
        if (mods == null || mods.isEmpty()) {
            throw ApiException.badRequest("mods.toml 缺少 [[mods]] 定义");
        }
        TomlTable mod = (TomlTable) mods.get(0);
        String modId = mod.getString("modId");
        String version = mod.getString("version");
        String name = mod.getString("displayName");
        if (modId == null || modId.isEmpty()) {
            throw ApiException.badRequest("mods.toml 缺少 modId");
        }
        List<String> deps = new ArrayList<>();
        String gameVersion = "";
        // [[dependencies.{modId}]] 数组，每一项带 modId 字段；minecraft 项带 versionRange
        TomlArray depArray = result.getArray("dependencies." + modId);
        if (depArray != null) {
            for (int i = 0; i < depArray.size(); i++) {
                Object o = depArray.get(i);
                if (o instanceof TomlTable t) {
                    String depId = t.getString("modId");
                    if (depId == null || depId.isEmpty()) {
                        continue;
                    }
                    deps.add(depId);
                    if ("minecraft".equals(depId)) {
                        gameVersion = extractVersion(t.getString("versionRange"));
                    }
                }
            }
        }
        return new JarMeta(modId, version == null ? "" : version, name == null ? modId : name,
                gameVersion, deps);
    }

    /** 从版本范围（如 "~1.21.4"、"[1.20.1,1.21)"）里提取 "1.21.4" 形式的版本号 */
    private String extractVersion(String range) {
        if (range == null) {
            return "";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)").matcher(range);
        return m.find() ? m.group(1) : "";
    }

    private byte[] readAll(ZipInputStream zin) throws IOException {
        return zin.readAllBytes();
    }
}
