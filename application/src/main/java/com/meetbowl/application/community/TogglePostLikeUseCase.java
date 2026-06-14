package com.meetbowl.application.community;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.PostLike;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 게시글 좋아요 토글 UseCase다. 한 사용자는 한 게시글에 좋아요를 한 번만 누를 수 있고, 이미 누른 상태에서 다시 요청하면 취소된다.
 *
 * <p>중복/경합 처리: 중복 방지의 최종 보장은 DB의 (post_id, user_id) 유니크 제약이다. "있으면 취소, 없으면 추가"의 검사-실행 사이에 같은 사용자의
 * 동시 좋아요가 끼어들어도, 한 행만 살아남고 나머지 insert는 {@link DataIntegrityViolationException}으로 실패한다 — 이를 잡아 결과를
 * '좋아요됨' 상태로 수렴시킨다(중복 행 없음).
 *
 * <p>트랜잭션 경계: 이 UseCase는 트랜잭션으로 감싸지 않는다. 각 저장소 메서드(존재확인·삭제·저장·집계)가 각자 트랜잭션을 가지므로, insert 충돌 예외를 잡아도
 * 바깥 트랜잭션이 오염되지 않고 이어서 카운트를 집계할 수 있다.
 *
 * <p>likeCount 정합성: 좋아요 수는 별도 카운터가 아니라 행 수 집계라, 추가/취소가 커밋되면 카운트가 자동으로 맞는다. 토글 직후 값을 다시 집계해 응답한다.
 */
@Service
public class TogglePostLikeUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final PostLikeRepositoryPort postLikeRepositoryPort;

    public TogglePostLikeUseCase(
            PostRepositoryPort postRepositoryPort, PostLikeRepositoryPort postLikeRepositoryPort) {
        this.postRepositoryPort = postRepositoryPort;
        this.postLikeRepositoryPort = postLikeRepositoryPort;
    }

    public LikeToggleResult execute(UUID postId, UUID userId) {
        // 게시글이 없거나 삭제됐으면 좋아요를 누를 수 없다.
        if (postRepositoryPort.findById(postId).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        boolean liked;
        if (postLikeRepositoryPort.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepositoryPort.deleteByPostIdAndUserId(postId, userId);
            liked = false;
        } else {
            try {
                postLikeRepositoryPort.save(PostLike.create(postId, userId));
            } catch (DataIntegrityViolationException concurrentDuplicate) {
                // 같은 사용자의 동시 좋아요로 유니크 제약 충돌 → 한 행만 남는다. 결과는 '좋아요됨' 상태.
            }
            liked = true;
        }

        long likeCount = postLikeRepositoryPort.countByPostId(postId);
        return new LikeToggleResult(liked, likeCount);
    }
}
