package com.meetbowl.common.response;

import java.util.List;

/**
 * 목록 API의 data 영역에 들어가는 공통 페이지 응답이다.
 * page는 API 명세 기준에 맞춰 1부터 시작하는 값을 사용한다.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        validate(page, size, totalElements);
        int totalPages = calculateTotalPages(size, totalElements);
        return new PageResponse<>(items, page, size, totalElements, totalPages);
    }

    private static void validate(int page, int size, long totalElements) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than or equal to 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be greater than or equal to 0");
        }
    }

    private static int calculateTotalPages(int size, long totalElements) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
