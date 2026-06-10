package com.meetbowl.common.response;

import java.util.List;

/** 페이지 번호와 메타데이터 해석이 목록 API마다 달라지지 않도록 외부 페이지 계약을 한 타입으로 제한한다. */
public record PageResponse<T>(
        List<T> items, int page, int size, long totalElements, int totalPages) {

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
