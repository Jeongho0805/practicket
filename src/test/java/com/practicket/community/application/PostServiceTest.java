package com.practicket.community.application;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.component.ProfanityValidator;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.common.exception.ValidateException;
import com.practicket.community.component.DeletePasswordEncoder;
import com.practicket.community.component.PostPasswordAttemptLimiter;
import com.practicket.community.component.PostRateLimiter;
import com.practicket.community.component.PostViewCounter;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostLike;
import com.practicket.community.domain.entity.PostTag;
import com.practicket.community.domain.repository.PostLikeRepository;
import com.practicket.community.domain.repository.PostQueryCondition;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.domain.repository.PostTagRepository;
import com.practicket.community.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Long AUTHOR_CLIENT_ID = 1L;
    private static final Long OTHER_CLIENT_ID = 2L;
    private static final Long POST_ID = 100L;
    private static final String CLIENT_IP = "118.235.13.7";

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ClientManager clientManager;

    @Mock
    private DeletePasswordEncoder deletePasswordEncoder;

    @Mock
    private PostPasswordAttemptLimiter passwordAttemptLimiter;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostViewCounter viewCounter;

    @Mock
    private PostTagRepository postTagRepository;

    @Mock
    private PostRateLimiter postRateLimiter;

    @Mock
    private ProfanityValidator profanityValidator;

    // ============ create() ============

    @Test
    @DisplayName("create - 작성 시점의 닉네임을 글에 박아둔다")
    void createSnapshotsNickname() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode("8317")).thenReturn("$2a$hashed");

        // when
        postService.create(createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        Post saved = captureSavedPost();
        assertThat(saved.getNickname()).isEqualTo("티켓요정");
        assertThat(saved.getIp()).isEqualTo(CLIENT_IP);
    }

    @Test
    @DisplayName("create - 닉네임이 없으면 익명으로 저장한다")
    void createFallsBackToAnonymousNickname() {
        // given
        givenClient(AUTHOR_CLIENT_ID, null);
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        // when
        postService.create(createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(captureSavedPost().getNickname()).isEqualTo("익명");
    }

    @Test
    @DisplayName("create - 삭제 비밀번호는 평문이 아니라 해시로 저장한다")
    void createStoresHashedPassword() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode("8317")).thenReturn("$2a$10$hashed");

        // when
        postService.create(createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        Post saved = captureSavedPost();
        assertThat(saved.getDeletePasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(saved.getDeletePasswordHash()).isNotEqualTo("8317");
    }

    @Test
    @DisplayName("create - 응답의 IP 는 앞 두 마디만 나간다")
    void createResponseMasksIp() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        // when
        PostResponse response =
                postService.create(createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(response.getIp()).isEqualTo("118.235");
        assertThat(response.getMine()).isTrue();
    }

    @Test
    @DisplayName("create - 영구 밴(banned=true, bannedUntil=null)이면 글을 쓸 수 없다")
    void createRejectsPermanentlyBannedClient() {
        // given
        givenBannedClient(AUTHOR_CLIENT_ID, null);

        // when & then
        assertThatThrownBy(() -> postService.create(
                createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_BANNED);

        verify(postRepository, never()).save(any());
        verifyNoInteractions(postRateLimiter, profanityValidator);
    }

    @Test
    @DisplayName("create - bannedUntil 이 아직 지나지 않은 기간 밴이면 글을 쓸 수 없다")
    void createRejectsTemporarilyBannedClient() {
        // given
        givenBannedClient(AUTHOR_CLIENT_ID, LocalDateTime.now().plusDays(1));

        // when & then
        assertThatThrownBy(() -> postService.create(
                createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_BANNED);

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - bannedUntil 이 지난 기간 밴은 별도 해제 배치 없이 그냥 통과한다")
    void createAllowsClientWhoseBanHasExpired() {
        // given
        givenBannedClient(AUTHOR_CLIENT_ID, LocalDateTime.now().minusMinutes(1));
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        // when
        postService.create(createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        verify(postRepository).save(any());
    }

    @Test
    @DisplayName("create - 토큰이든 IP 든 레이트리밋에 걸리면 저장하지 않고 그대로 전파한다")
    void createPropagatesRateLimitException() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        doThrow(new GlobalException(ErrorCode.POST_RATE_LIMIT_TOKEN))
                .when(postRateLimiter).validate(anyString(), anyString());

        // when & then
        assertThatThrownBy(() -> postService.create(
                createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_RATE_LIMIT_TOKEN);

        verify(postRepository, never()).save(any());
        verifyNoInteractions(profanityValidator);
    }

    @Test
    @DisplayName("create - 욕설 필터가 막으면 저장하지 않는다")
    void createRejectsProfanity() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        doThrow(new ValidateException(ErrorCode.INAPPROPRIATE_CONTENT))
                .when(profanityValidator).validateProfanityText("욕설 제목");

        // when & then
        assertThatThrownBy(() -> postService.create(
                createRequest("욕설 제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.INAPPROPRIATE_CONTENT);

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - 욕설 필터 API 가 죽어도 통과한다(fail-open) — ProfanityValidator 가 삼키므로 create 는 예외를 안 본다")
    void createSucceedsWhenProfanityCheckIsSilent() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        // ProfanityValidator 는 외부 API 장애를 자체적으로 삼키고 아무 것도 던지지 않는다(fail-open).
        // 여기서는 그 계약을 그대로 가정해 아무것도 stub 하지 않는 것으로 재현한다.

        // when
        postService.create(createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        verify(postRepository).save(any());
    }

    // ============ get() ============

    @Test
    @DisplayName("get - 없는 글이면 POST_NOT_FOUND")
    void getThrowsWhenPostIsMissing() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.get(POST_ID, clientInfo(AUTHOR_CLIENT_ID)))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("get - 남의 글이면 mine 이 false")
    void getMarksOthersPostAsNotMine() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));

        // when
        PostResponse response = postService.get(POST_ID, clientInfo(OTHER_CLIENT_ID));

        // then
        assertThat(response.getMine()).isFalse();
    }

    // ============ update() ============

    @Test
    @DisplayName("update - 작성자면 내용이 바뀌고 edited 가 true 가 된다")
    void updateMarksPostAsEdited() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        // when
        postService.update(POST_ID, updateRequest("바뀐 제목", "바뀐 본문"), clientInfo(AUTHOR_CLIENT_ID));

        // then
        assertThat(post.getTitle()).isEqualTo("바뀐 제목");
        assertThat(post.getContent()).isEqualTo("바뀐 본문");
        assertThat(post.getEdited()).isTrue();
    }

    @Test
    @DisplayName("update - 작성자가 아니면 비밀번호를 알아도 수정할 수 없다")
    void updateRejectsNonAuthorEvenWithPassword() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() ->
                postService.update(POST_ID, updateRequest("탈취 제목", "탈취 본문"), clientInfo(OTHER_CLIENT_ID)))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_FORBIDDEN);

        assertThat(post.getTitle()).isEqualTo("원래 제목");
        verifyNoInteractions(deletePasswordEncoder);
    }

    // ============ delete() ============

    @Test
    @DisplayName("delete - 작성자면 비밀번호를 묻지 않는다")
    void deleteSkipsPasswordForAuthor() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        // when
        postService.delete(POST_ID, null, clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(post.getDeletedAt()).isNotNull();
        verifyNoInteractions(deletePasswordEncoder, passwordAttemptLimiter);
    }

    @Test
    @DisplayName("delete - 행을 지우지 않고 deletedAt 만 남긴다")
    void deleteKeepsRowAndOnlyStampsDeletedAt() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        // when
        postService.delete(POST_ID, null, clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(post.getDeletedAt()).isNotNull();
        verify(postRepository, never()).delete(any());
        verify(postRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete - 남의 글인데 비밀번호가 없으면 POST_PASSWORD_REQUIRED")
    void deleteRequiresPasswordForNonAuthor() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.delete(POST_ID, deleteRequest(null), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_PASSWORD_REQUIRED);

        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("delete - 비밀번호가 틀리면 실패를 기록하고 거절한다")
    void deleteRecordsFailureOnWrongPassword() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(deletePasswordEncoder.matches("9999", "$2a$hashed")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> postService.delete(POST_ID, deleteRequest("9999"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_PASSWORD_MISMATCH);

        assertThat(post.getDeletedAt()).isNull();
        verify(passwordAttemptLimiter).recordFailure(POST_ID, CLIENT_IP);
        verify(passwordAttemptLimiter, never()).clearFailures(any(), anyString());
    }

    @Test
    @DisplayName("delete - 비밀번호가 맞으면 삭제되고 실패 누적이 지워진다")
    void deleteSucceedsWithCorrectPassword() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(deletePasswordEncoder.matches("1234", "$2a$hashed")).thenReturn(true);

        // when
        postService.delete(POST_ID, deleteRequest("1234"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(post.getDeletedAt()).isNotNull();
        verify(passwordAttemptLimiter).clearFailures(POST_ID, CLIENT_IP);
        verify(passwordAttemptLimiter, never()).recordFailure(any(), anyString());
    }

    @Test
    @DisplayName("delete - 잠긴 IP 면 비밀번호 대조 자체를 하지 않는다")
    void deleteStopsBeforeComparingWhenLocked() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        doThrow(new GlobalException(ErrorCode.POST_PASSWORD_LOCKED))
                .when(passwordAttemptLimiter).validateNotLocked(POST_ID, CLIENT_IP);

        // when & then
        assertThatThrownBy(() -> postService.delete(POST_ID, deleteRequest("1234"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_PASSWORD_LOCKED);

        assertThat(post.getDeletedAt()).isNull();
        verifyNoInteractions(deletePasswordEncoder);
    }

    @Test
    @DisplayName("create - 뻔한 비밀번호는 거절한다. 시도 제한이 있어도 이건 뚫린다")
    void createRejectsCommonPassword() {
        // when & then
        assertThatThrownBy(() -> postService.create(
                createRequest("제목", "본문", "1234"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_PASSWORD_TOO_COMMON);

        verify(postRepository, never()).save(any());
    }

    // ============ search() - 정렬 ============

    @Test
    @DisplayName("search - sort 를 안 주면 최신순(LATEST) 조건이 리포지토리에 전달된다")
    void searchDefaultsToLatestSort() {
        assertSortMapsTo(null, PostQueryCondition.PostSortType.LATEST);
    }

    @Test
    @DisplayName("search - sort=like 면 LIKE 조건이 전달된다")
    void searchPassesLikeSort() {
        assertSortMapsTo("like", PostQueryCondition.PostSortType.LIKE);
    }

    @Test
    @DisplayName("search - sort=view 면 VIEW 조건이 전달된다")
    void searchPassesViewSort() {
        assertSortMapsTo("view", PostQueryCondition.PostSortType.VIEW);
    }

    @Test
    @DisplayName("search - sort=comment 면 COMMENT 조건이 전달된다")
    void searchPassesCommentSort() {
        assertSortMapsTo("comment", PostQueryCondition.PostSortType.COMMENT);
    }

    @Test
    @DisplayName("search - 알 수 없는 sort 값은 에러 없이 기본값(LATEST)으로 떨어진다 — 주소창은 사용자가 직접 고칠 수 있다")
    void searchFallsBackToLatestForUnknownSort() {
        assertSortMapsTo("updatedAt", PostQueryCondition.PostSortType.LATEST);
    }

    private void assertSortMapsTo(String rawSort, PostQueryCondition.PostSortType expected) {
        // given
        when(postRepository.search(any(), any())).thenReturn(Page.empty());
        PostSearchCondition condition = new PostSearchCondition();
        condition.setSort(rawSort);

        // when
        postService.search(condition, PageRequest.of(0, 20));

        // then
        ArgumentCaptor<PostQueryCondition> captor = ArgumentCaptor.forClass(PostQueryCondition.class);
        verify(postRepository).search(captor.capture(), any());
        assertThat(captor.getValue().getSort()).isEqualTo(expected);
    }

    // ============ searchMine() ============

    @Test
    @DisplayName("searchMine - 이 토큰이 쓴 글로만 좁힌다")
    void searchMineFiltersByClient() {
        // given
        when(postRepository.search(any(), any())).thenReturn(Page.empty());

        // when
        postService.searchMine(clientInfo(AUTHOR_CLIENT_ID), PageRequest.of(0, 20));

        // then
        ArgumentCaptor<PostQueryCondition> captor = ArgumentCaptor.forClass(PostQueryCondition.class);
        verify(postRepository).search(captor.capture(), any());
        assertThat(captor.getValue().getClientId()).isEqualTo(AUTHOR_CLIENT_ID);
    }

    // ============ 태그 ============

    @Test
    @DisplayName("create - 태그는 정규화해서 저장한다")
    void createSavesNormalizedTags() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        PostCreateRequest request = createRequest("제목", "본문", "8317");
        request.setTags(List.of(" #IVE ", "세븐틴!"));

        // when
        postService.create(request, clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(captureSavedTags()).containsExactly("ive", "세븐틴");
    }

    @Test
    @DisplayName("create - 태그가 없어도 글은 저장된다. 기본 태그를 붙이지 않는다")
    void createAllowsNoTags() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        // when
        PostResponse response = postService.create(
                createRequest("제목", "본문", "8317"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(response.getTags()).isEmpty();
        verify(postTagRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - 기존 태그를 지우고 새로 넣는다")
    void updateReplacesTags() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        PostUpdateRequest request = updateRequest("새 제목", "새 본문");
        request.setTags(List.of("후기"));

        // when
        postService.update(POST_ID, request, clientInfo(AUTHOR_CLIENT_ID));

        // then
        verify(postTagRepository).deleteByPost(post);
        assertThat(captureSavedTags()).containsExactly("후기");
    }

    // ============ toggleLike() ============

    @Test
    @DisplayName("toggleLike - 본인 글은 추천할 수 없다")
    void toggleLikeRejectsOwnPost() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");

        // when & then
        assertThatThrownBy(() -> postService.toggleLike(POST_ID, clientInfo(AUTHOR_CLIENT_ID)))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_LIKE_SELF);

        verify(postLikeRepository, never()).save(any());
        verify(postRepository, never()).incrementLikeCount(any());
    }

    @Test
    @DisplayName("toggleLike - 처음 누르면 추천이 쌓이고 수가 하나 오른다")
    void toggleLikeAddsLike() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        when(postLikeRepository.existsByPostAndClient(any(), any())).thenReturn(false);

        // when
        PostLikeResponse response = postService.toggleLike(POST_ID, clientInfo(OTHER_CLIENT_ID));

        // then
        assertThat(response.getLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(1L);
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).incrementLikeCount(POST_ID);
    }

    @Test
    @DisplayName("toggleLike - 이미 눌렀으면 취소되고 수가 하나 내린다")
    void toggleLikeCancelsExistingLike() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        when(postLikeRepository.existsByPostAndClient(any(), any())).thenReturn(true);

        // when
        PostLikeResponse response = postService.toggleLike(POST_ID, clientInfo(OTHER_CLIENT_ID));

        // then
        assertThat(response.getLiked()).isFalse();
        assertThat(response.getLikeCount()).isZero();
        verify(postLikeRepository).deleteByPostAndClient(any(), any());
        verify(postRepository).decrementLikeCount(POST_ID);
        verify(postLikeRepository, never()).save(any());
    }

    // ============ getAndCountView() ============

    @Test
    @DisplayName("getAndCountView - 처음 보는 사람이면 조회수를 올린다")
    void getAndCountViewCountsFirstView() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        when(viewCounter.markViewed(POST_ID, OTHER_CLIENT_ID)).thenReturn(true);

        // when
        postService.getAndCountView(POST_ID, clientInfo(OTHER_CLIENT_ID));

        // then
        verify(postRepository).incrementViewCount(POST_ID);
    }

    @Test
    @DisplayName("getAndCountView - 24시간 안에 이미 본 사람이면 조회수를 올리지 않는다")
    void getAndCountViewSkipsRepeatedView() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        when(viewCounter.markViewed(POST_ID, OTHER_CLIENT_ID)).thenReturn(false);

        // when
        postService.getAndCountView(POST_ID, clientInfo(OTHER_CLIENT_ID));

        // then
        verify(postRepository, never()).incrementViewCount(any());
    }

    @Test
    @DisplayName("getAndCountView - 내가 추천한 글이면 liked 로 알려준다")
    void getAndCountViewTellsWhetherViewerLiked() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        when(postLikeRepository.existsByPostAndClient(any(), any())).thenReturn(true);

        // when
        PostResponse response = postService.getAndCountView(POST_ID, clientInfo(OTHER_CLIENT_ID));

        // then
        assertThat(response.getLiked()).isTrue();
    }

    // ============ helpers ============

    private void givenClient(Long clientId, String name) {
        Client client = Client.builder()
                .id(clientId)
                .token("token-" + clientId)
                .ip(CLIENT_IP)
                .device("device")
                .referer("referer")
                .name(name)
                .banned(false)
                .build();
        when(clientManager.findById(clientId)).thenReturn(client);
    }

    /** bannedUntil 이 null 이면 영구 밴, 값이 있으면 그 시각까지의 기간 밴이다(Q8). */
    private void givenBannedClient(Long clientId, LocalDateTime bannedUntil) {
        Client client = Client.builder()
                .id(clientId)
                .token("token-" + clientId)
                .ip(CLIENT_IP)
                .device("device")
                .referer("referer")
                .name("티켓요정")
                .banned(true)
                .bannedUntil(bannedUntil)
                .build();
        when(clientManager.findById(clientId)).thenReturn(client);
    }

    private void givenSaveReturnsArgument() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private List<String> captureSavedTags() {
        ArgumentCaptor<PostTag> captor = ArgumentCaptor.forClass(PostTag.class);
        verify(postTagRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().map(PostTag::getTag).toList();
    }

    private Post captureSavedPost() {
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        return captor.getValue();
    }

    private Post post() {
        Client author = Client.builder()
                .id(AUTHOR_CLIENT_ID)
                .token("token-" + AUTHOR_CLIENT_ID)
                .ip(CLIENT_IP)
                .device("device")
                .referer("referer")
                .name("티켓요정")
                .banned(false)
                .build();

        return Post.builder()
                .id(POST_ID)
                .client(author)
                .title("원래 제목")
                .content("원래 본문")
                .nickname("티켓요정")
                .ip(CLIENT_IP)
                .deletePasswordHash("$2a$hashed")
                .build();
    }

    private ClientInfo clientInfo(Long clientId) {
        return ClientInfo.builder()
                .clientId(clientId)
                .token("token-" + clientId)
                .banned(false)
                .build();
    }

    private PostCreateRequest createRequest(String title, String content, String deletePassword) {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setDeletePassword(deletePassword);
        return request;
    }

    private PostUpdateRequest updateRequest(String title, String content) {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle(title);
        request.setContent(content);
        return request;
    }

    private PostDeleteRequest deleteRequest(String deletePassword) {
        PostDeleteRequest request = new PostDeleteRequest();
        request.setDeletePassword(deletePassword);
        return request;
    }
}
