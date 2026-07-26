package com.practicket.community.application;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.community.component.DeletePasswordEncoder;
import com.practicket.community.component.PostPasswordAttemptLimiter;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    // ============ create() ============

    @Test
    @DisplayName("create - 작성 시점의 닉네임을 글에 박아둔다")
    void createSnapshotsNickname() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode("1234")).thenReturn("$2a$hashed");

        // when
        postService.create(createRequest("제목", "본문", "1234"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

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
        postService.create(createRequest("제목", "본문", "1234"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(captureSavedPost().getNickname()).isEqualTo("익명");
    }

    @Test
    @DisplayName("create - 삭제 비밀번호는 평문이 아니라 해시로 저장한다")
    void createStoresHashedPassword() {
        // given
        givenClient(AUTHOR_CLIENT_ID, "티켓요정");
        givenSaveReturnsArgument();
        when(deletePasswordEncoder.encode("1234")).thenReturn("$2a$10$hashed");

        // when
        postService.create(createRequest("제목", "본문", "1234"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        Post saved = captureSavedPost();
        assertThat(saved.getDeletePasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(saved.getDeletePasswordHash()).isNotEqualTo("1234");
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
                postService.create(createRequest("제목", "본문", "1234"), clientInfo(AUTHOR_CLIENT_ID), CLIENT_IP);

        // then
        assertThat(response.getIp()).isEqualTo("118.235");
        assertThat(response.getMine()).isTrue();
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
        verify(passwordAttemptLimiter).recordFailure(CLIENT_IP);
        verify(passwordAttemptLimiter, never()).clearFailures(anyString());
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
        verify(passwordAttemptLimiter).clearFailures(CLIENT_IP);
        verify(passwordAttemptLimiter, never()).recordFailure(anyString());
    }

    @Test
    @DisplayName("delete - 잠긴 IP 면 비밀번호 대조 자체를 하지 않는다")
    void deleteStopsBeforeComparingWhenLocked() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        doThrow(new GlobalException(ErrorCode.POST_PASSWORD_LOCKED))
                .when(passwordAttemptLimiter).validateNotLocked(CLIENT_IP);

        // when & then
        assertThatThrownBy(() -> postService.delete(POST_ID, deleteRequest("1234"), clientInfo(OTHER_CLIENT_ID), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_PASSWORD_LOCKED);

        assertThat(post.getDeletedAt()).isNull();
        verifyNoInteractions(deletePasswordEncoder);
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

    private void givenSaveReturnsArgument() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
