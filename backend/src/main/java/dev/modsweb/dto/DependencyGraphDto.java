package dev.modsweb.dto;

import java.util.List;

/** 依赖图谱数据（切入点六）：nodes + edges，前端手写 SVG 渲染 */
public record DependencyGraphDto(
        List<NodeDto> nodes,
        List<EdgeDto> edges
) {
    public record NodeDto(String id, String label, String type, String slug) {
    }

    /** source/target 均为 NodeDto.id */
    public record EdgeDto(String source, String target) {
    }
}
