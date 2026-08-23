package com.practicket.community.application;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.component.ProfanityValidator;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.common.exception.ValidateException;
import com.practicket.community.component.PostCommentRateLimiter;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostComment;
import com.practicket.community.domain.repository.PostCommentRepository;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.dto.PostCommentCreateRequest;
import com.practicket.community.dto.PostCommentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    private static final Long AUTHOR_CLIENT_ID = 1L;
    private static final Long OTHER_CLIENT_ID = 2L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 200L;
    private static final String CLIENT_IP = "118.235.13.7";

    @InjectMocks
    private PostCommentService postCommentService;

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ClientManager clientManager;

    @Mock
    private PostCommentRateLimiter commentRateLimiter;

    @Mock
    private ProfanityValidator profanityValidator;

    // ============ create() ============

    @Test
    @DisplayName("create - 작성 시점의 닉네임과 IP 를 댓글에 박아둔다")
    void createSnapshotsNicknameAndIp() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        givenSaveReturnsArgument();

        // when
        postCommentService.create(POST_ID, createRequest("좋은 글이네요"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP);

        // then
        PostComment saved = captureSavedComment();
        assertThat(saved.getNickname()).isEqualTo("구경꾼");
        assertThat(saved.getIp()).isEqualTo(CLIENT_IP);
    }

    @Test
    @DisplayName("create - 닉네임이 없으면 익명으로 저장한다")
    void createFallsBackToAnonymousNickname() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, null);
        givenSaveReturnsArgument();

        // when
        postCommentService.create(POST_ID, createRequest("좋은 글이네요"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(captureSavedComment().getNickname()).isEqualTo("익명");
    }

    @Test
    @DisplayName("create - 글의 댓글 수가 하나 오른다")
    void createIncrementsCommentCount() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        givenSaveReturnsArgument();

        // when
        postCommentService.create(POST_ID, createRequest("좋은 글이네요"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP);

        // then
        verify(postRepository).incrementCommentCount(POST_ID);
    }

    @Test
    @DisplayName("create - 없는 글에는 댓글을 달 수 없다")
    void createRejectsMissingPost() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postCommentService.create(
                POST_ID, createRequest("좋은 글이네요"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - 밴된 사용자는 댓글을 달 수 없다")
    void createRejectsBannedClient() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenBannedClient(OTHER_CLIENT_ID, null);

        // when & then
        assertThatThrownBy(() -> postCommentService.create(
                POST_ID, createRequest("좋은 글이네요"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_BANNED);

        verify(postCommentRepository, never()).save(any());
        verifyNoInteractions(commentRateLimiter, profanityValidator);
    }

    @Test
    @DisplayName("create - bannedUntil 이 지난 기간 밴은 별도 배치 없이 그냥 통과한다")
    void createAllowsClientWhoseBanHasExpired() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenBannedClient(OTHER_CLIENT_ID, LocalDateTime.now().minusMinutes(1));
        givenSaveReturnsArgument();

        // when
        postCommentService.create(POST_ID, createRequest("좋은 글이네요"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP);

        // then
        verify(postCommentRepository).save(any());
    }

    @Test
    @DisplayName("create - 레이트리밋에 걸리면 저장하지 않고 그대로 전파한다")
    void createPropagatesRateLimitException() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        doThrow(new GlobalException(ErrorCode.COMMENT_RATE_LIMIT_TOKEN))
                .when(commentRateLimiter).validate(anyString(), anyString());

        // when & then
        assertThatThrownBy(() -> postCommentService.create(
                POST_ID, createRequest("좋은 글이네요"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_RATE_LIMIT_TOKEN);

        verify(postCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - 욕설 필터가 막으면 저장하지 않고 레이트리밋도 세지 않는다")
    void createRejectsProfanity() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient(OTHER_CLIENT_ID, "구경꾼");
        doThrow(new ValidateException(ErrorCode.INAPPROPRIATE_CONTENT))
                .when(profanityValidator).validateProfanityText("욕설 댓글");

        // when & then
        assertThatThrownBy(() -> postCommentService.create(
                POST_ID, createRequest("욕설 댓글"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.INAPPROPRIATE_CONTENT);

        verify(postCommentRepository, never()).save(any());
        verifyNoInteractions(commentRateLimiter);
    }

    // ============ list() ============

    @Test
    @DisplayName("list - 본문은 escape 된 결과로만 나간다")
    void listReturnsEscapedContentOnly() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postCommentRepository.findByPostOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(comment(AUTHOR_CLIENT_ID, "<script>alert(1)</script>")));

        // when
        List<PostCommentResponse> responses = postCommentService.list(POST_ID, clientInfo(AUTHOR_CLIENT_ID));

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRenderedContent()).doesNotContain("<script>");
    }

    @Test
    @DisplayName("list - IP 는 앞 두 마디만 나간다")
    void listMasksIp() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postCommentRepository.findByPostOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(comment(AUTHOR_CLIENT_ID, "내용")));

        // when
        List<PostCommentResponse> responses = postCommentService.list(POST_ID, clientInfo(AUTHOR_CLIENT_ID));

        // then
        assertThat(responses.get(0).getIp()).isEqualTo("118.235");
    }

    @Test
    @DisplayName("list - 토큰이 작성자와 다르면 mine 이 아니다")
    void listMarksOnlyOwnComments() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postCommentRepository.findByPostOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(comment(AUTHOR_CLIENT_ID, "내용")));

        // when
        List<PostCommentResponse> responses = postCommentService.list(POST_ID, clientInfo(OTHER_CLIENT_ID));

        // then
        assertThat(responses.get(0).getMine()).isFalse();
    }

    // ============ delete() ============

    @Test
    @DisplayName("delete - 행을 지우지 않고 deletedAt 만 남긴다")
    void deleteKeepsRowAndOnlyStampsDeletedAt() {
        // given
        PostComment comment = comment(AUTHOR_CLIENT_ID, "내용");
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        // when
        postCommentService.delete(COMMENT_ID, clientInfo(AUTHOR_CLIENT_ID));

        // then
        assertThat(comment.getDeletedAt()).isNotNull();
        verify(postCommentRepository, never()).delete(any());
        verify(postRepository).decrementCommentCount(POST_ID);
    }

    @Test
    @DisplayName("delete - 남의 댓글은 지울 수 없다")
    void deleteRejectsOtherPeoplesComment() {
        // given
        PostComment comment = comment(AUTHOR_CLIENT_ID, "내용");
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> postCommentService.delete(COMMENT_ID, clientInfo(OTHER_CLIENT_ID)))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_FORBIDDEN);

        assertThat(comment.getDeletedAt()).isNull();
        verify(postRepository, never()).decrementCommentCount(any());
    }

    // ============ helpers ============

    private void givenClient(Long clientId, String name) {
        when(clientManager.findById(clientId)).thenReturn(client(clientId, name));
    }

    /** bannedUntil 이 null 이면 영구 밴, 값이 있으면 그 시각까지의 기간 밴이다(Q8). */
    private void givenBannedClient(Long clientId, LocalDateTime bannedUntil) {
        Client client = Client.builder()
                .id(clientId)
                .token("token-" + clientId)
                .ip(CLIENT_IP)
                .device("device")
                .referer("referer")
                .name("구경꾼")
                .banned(true)
                .bannedUntil(bannedUntil)
                .build();
        when(clientManager.findById(clientId)).thenReturn(client);
    }

    private void givenSaveReturnsArgument() {
        when(postCommentRepository.save(any(PostComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PostComment captureSavedComment() {
        ArgumentCaptor<PostComment> captor = ArgumentCaptor.forClass(PostComment.class);
        verify(postCommentRepository).save(captor.capture());
        return captor.getValue();
    }

    private Client client(Long clientId, String name) {
        return Client.builder()
                .id(clientId)
                .token("token-" + clientId)
                .ip(CLIENT_IP)
                .device("device")
                .referer("referer")
                .name(name)
                .banned(false)
                .build();
    }

    private Post post() {
        return Post.builder()
                .id(POST_ID)
                .client(client(AUTHOR_CLIENT_ID, "티켓요정"))
                .title("제목")
                .content("본문")
                .nickname("티켓요정")
                .ip(CLIENT_IP)
                .deletePasswordHash("$2a$hashed")
                .build();
    }

    private PostComment comment(Long writerClientId, String content) {
        return PostComment.builder()
                .id(COMMENT_ID)
                .post(post())
                .client(client(writerClientId, "티켓요정"))
                .content(content)
                .nickname("티켓요정")
                .ip(CLIENT_IP)
                .build();
    }

    private ClientInfo clientInfo(Long clientId) {
        return ClientInfo.builder()
                .clientId(clientId)
                .token("token-" + clientId)
                .banned(false)
                .build();
    }

    private PostCommentCreateRequest createRequest(String content) {
        PostCommentCreateRequest request = new PostCommentCreateRequest();
        request.setContent(content);
        return request;
    }
}
