package com.meetbowl.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    void createsPageResponseWithCalculatedTotalPages() {
        PageResponse<String> response = PageResponse.of(List.of("meeting-1", "meeting-2"), 1, 2, 5);

        assertThat(response.items()).containsExactly("meeting-1", "meeting-2");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void zeroTotalElementsHasZeroTotalPages() {
        PageResponse<String> response = PageResponse.of(List.of(), 1, 20, 0);

        assertThat(response.totalPages()).isZero();
    }

    @Test
    void pageMustStartFromOne() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be greater than or equal to 1");
    }
}
