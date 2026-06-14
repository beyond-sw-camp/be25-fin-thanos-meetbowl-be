package com.meetbowl.api.community;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.application.community.CommentResult;
import com.meetbowl.application.community.CreateCommentCommand;
import com.meetbowl.application.community.CreateCommentUseCase;
import com.meetbowl.application.community.CreatePostCommand;
import com.meetbowl.application.community.CreatePostUseCase;
import com.meetbowl.application.community.DeleteCommentUseCase;
import com.meetbowl.application.community.DeletePostUseCase;
import com.meetbowl.application.community.GetHotPostsUseCase;
import com.meetbowl.application.community.GetPostDetailUseCase;
import com.meetbowl.application.community.ListCommentsUseCase;
import com.meetbowl.application.community.ListPostUseCase;
import com.meetbowl.application.community.ToggleCommentLikeUseCase;
import com.meetbowl.application.community.TogglePostLikeUseCase;
import com.meetbowl.application.community.UpdateCommentCommand;
import com.meetbowl.application.community.UpdateCommentUseCase;
import com.meetbowl.application.community.UpdatePostCommand;
import com.meetbowl.application.community.UpdatePostUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 익명 커뮤니티 게시글 API다. 등록·목록 조회·Hot 조회를 제공한다(로그인 필수, User/Admin).
 *
 * <p>익명성: 작성자 식별자는 본문이 아닌 {@link CurrentUser}에서만 채워 사칭을 막고, 응답에는 실제 userId 대신 "익명N" 표시명만 노출한다.
 */
@Validated
@RestController
@RequireUserOrAdmin
@RequestMapping(ApiPaths.API_V1 + "/community/posts")
public class CommunityController extends BaseController {

    private final CreatePostUseCase createPostUseCase;
    private final ListPostUseCase listPostUseCase;
    private final GetHotPostsUseCase getHotPostsUseCase;
    private final UpdatePostUseCase updatePostUseCase;
    private final DeletePostUseCase deletePostUseCase;
    private final GetPostDetailUseCase getPostDetailUseCase;
    private final CreateCommentUseCase createCommentUseCase;
    private final ListCommentsUseCase listCommentsUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final TogglePostLikeUseCase togglePostLikeUseCase;
    private final ToggleCommentLikeUseCase toggleCommentLikeUseCase;

    public CommunityController(
            CreatePostUseCase createPostUseCase,
            ListPostUseCase listPostUseCase,
            GetHotPostsUseCase getHotPostsUseCase,
            UpdatePostUseCase updatePostUseCase,
            DeletePostUseCase deletePostUseCase,
            GetPostDetailUseCase getPostDetailUseCase,
            CreateCommentUseCase createCommentUseCase,
            ListCommentsUseCase listCommentsUseCase,
            UpdateCommentUseCase updateCommentUseCase,
            DeleteCommentUseCase deleteCommentUseCase,
            TogglePostLikeUseCase togglePostLikeUseCase,
            ToggleCommentLikeUseCase toggleCommentLikeUseCase) {
        this.createPostUseCase = createPostUseCase;
        this.listPostUseCase = listPostUseCase;
        this.getHotPostsUseCase = getHotPostsUseCase;
        this.updatePostUseCase = updatePostUseCase;
        this.deletePostUseCase = deletePostUseCase;
        this.getPostDetailUseCase = getPostDetailUseCase;
        this.createCommentUseCase = createCommentUseCase;
        this.listCommentsUseCase = listCommentsUseCase;
        this.updateCommentUseCase = updateCommentUseCase;
        this.deleteCommentUseCase = deleteCommentUseCase;
        this.togglePostLikeUseCase = togglePostLikeUseCase;
        this.toggleCommentLikeUseCase = toggleCommentLikeUseCase;
    }

    /** 게시글 등록. 작성자는 인증 토큰에서 가져오고, 첫 활동이면 익명 번호를 발급해 "익명N"으로 노출한다. */
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody CreatePostRequest request) {
        // 카테고리 문자열(enum 이름/한글 라벨)은 UseCase에서 도메인 카테고리로 해석한다(알 수 없는 값이면 400).
        CreatePostCommand command =
                new CreatePostCommand(
                        request.category(),
                        request.title(),
                        request.content(),
                        currentUser.userId());
        return created(PostResponse.from(createPostUseCase.execute(command)));
    }

    /**
     * 게시글 목록 조회. {@code sort}는 latest(최신순)/popular(인기순), {@code category}는 카테고리 필터(미지정 시 전체),
     * {@code keyword}는 제목+내용 부분일치 검색이다. 응답은 conventions의 목록 포맷을 따른다.
     */
    @GetMapping
    public ApiResponse<PostPageResponse> listPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        // 카테고리/정렬 해석은 UseCase에서 수행한다(app-api는 도메인 타입에 의존하지 않는다).
        return ok(
                PostPageResponse.from(
                        listPostUseCase.execute(category, keyword, sort, page, size)));
    }

    /** Hot 게시글 조회. 최근 48시간 내 글 중 인기 점수 상위 3개를 목록 상단 노출용으로 내린다. */
    @GetMapping("/hot")
    public ApiResponse<List<PostListItemResponse>> hotPosts() {
        return ok(getHotPostsUseCase.execute().stream().map(PostListItemResponse::from).toList());
    }

    /** 게시글 상세 조회. 조회 시 조회수를 1 증가시키고 본문·카운트·작성 시각·현재 사용자의 좋아요 여부를 내린다. 없는 글이면 404. */
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPost(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID postId) {
        return ok(
                PostDetailResponse.from(
                        getPostDetailUseCase.execute(postId, currentUser.userId())));
    }

    /** 게시글 수정. 작성자 본인만 가능(아니면 403). 수정 항목은 카테고리·제목·내용뿐이며 조회수/좋아요수/댓글수는 보존한다. 없는 글이면 404. */
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {
        UpdatePostCommand command =
                new UpdatePostCommand(
                        postId,
                        currentUser.userId(),
                        request.category(),
                        request.title(),
                        request.content());
        return ok(PostResponse.from(updatePostUseCase.execute(command)));
    }

    /** 게시글 삭제. 작성자 본인만 가능(아니면 403). 게시글과 함께 댓글·좋아요(댓글 좋아요 포함)를 cascade 삭제한다. 없는 글이면 404. */
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID postId) {
        deletePostUseCase.execute(postId, currentUser.userId());
        return ok();
    }

    /** 댓글 등록. 작성자는 인증 토큰에서 가져오고, 게시글 별칭과 동일한 "익명N"으로 표시한다. 게시글이 없거나 삭제됐으면 404. */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResult result =
                createCommentUseCase.execute(
                        new CreateCommentCommand(postId, request.content(), currentUser.userId()));
        return created(CommentResponse.from(result));
    }

    /** 댓글 목록 조회. 한 게시글의 댓글을 등록순으로, 작성자 "익명N"·좋아요 수와 함께 내린다. 게시글이 없으면 404. */
    @GetMapping("/{postId}/comments")
    public ApiResponse<List<CommentListItemResponse>> listComments(@PathVariable UUID postId) {
        return ok(
                listCommentsUseCase.execute(postId).stream()
                        .map(CommentListItemResponse::from)
                        .toList());
    }

    /** 댓글 수정. 작성자 본인만 가능(아니면 403). 수정 항목은 내용뿐. 댓글이 없거나 경로 게시글과 맞지 않으면 404. */
    @PatchMapping("/{postId}/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentResult result =
                updateCommentUseCase.execute(
                        new UpdateCommentCommand(
                                postId, commentId, currentUser.userId(), request.content()));
        return ok(CommentResponse.from(result));
    }

    /** 댓글 삭제. 작성자 본인만 가능(아니면 403). 댓글과 함께 댓글 좋아요를 cascade 삭제한다. 댓글이 없거나 경로 게시글과 맞지 않으면 404. */
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @PathVariable UUID commentId) {
        deleteCommentUseCase.execute(postId, commentId, currentUser.userId());
        return ok();
    }

    /** 게시글 좋아요 토글. 누른 적 없으면 추가, 있으면 취소. 중복은 막고 토글 후 상태·카운트를 반환한다. 게시글이 없으면 404. */
    @PostMapping("/{postId}/likes")
    public ApiResponse<LikeResponse> togglePostLike(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID postId) {
        return ok(LikeResponse.from(togglePostLikeUseCase.execute(postId, currentUser.userId())));
    }

    /** 댓글 좋아요 토글. 규칙은 게시글 좋아요와 동일. 댓글이 없거나 경로 게시글과 맞지 않으면 404. */
    @PostMapping("/{postId}/comments/{commentId}/likes")
    public ApiResponse<LikeResponse> toggleCommentLike(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @PathVariable UUID commentId) {
        return ok(
                LikeResponse.from(
                        toggleCommentLikeUseCase.execute(postId, commentId, currentUser.userId())));
    }
}
