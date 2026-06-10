package com.meetbowl.domain.community;

import java.util.Optional;
import java.util.UUID;

import com.meetbowl.domain.common.Paged;

/** 게시글 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface PostRepositoryPort {

    Post save(Post post);

    Optional<Post> findById(UUID id);

    /** 목록 조회(최신순). {@code category}가 null이면 전체, 아니면 해당 카테고리만. {@code page}는 1부터 시작한다. */
    Paged<Post> findPage(CommunityCategory category, int page, int size);

    void deleteById(UUID id);
}