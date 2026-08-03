package dev.modsweb.dto;

import java.util.List;

/** 简单分页结果 */
public record PageResult<T>(List<T> items, long total, int page, int size, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResult<>(items, total, page, size, totalPages);
    }
}
