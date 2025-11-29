package com.practicket.chat;

import com.practicket.chat.dto.ChatResponseDto;
import com.practicket.chat.component.ChatConnectionManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.awaitility.Awaitility.await;
import static org.bson.assertions.Assertions.assertNotNull;

public class ChatConnectionStoreTest {

    private ChatConnectionManager chatConnectionStore;

    @BeforeEach
    void init() {
        chatConnectionStore = new ChatConnectionManager();
    }

    @Test
    @DisplayName("save 호출 시 Emitter을 저장하고 반환하다")
    void saveTest() {
        // given
        String key = "test";

        // when
        SseEmitter emitter = chatConnectionStore.save(key);

        // then
        assertNotNull(emitter);
    }

    @Test
    @DisplayName("complete 발생시 Emitter 저장소에서 삭제된다.")
    void onCompleteTest() throws InterruptedException {
        // given
        String key = "test";
        SseEmitter emitter = chatConnectionStore.save(key);

        // when
        emitter.complete();
        chatConnectionStore.broadcast(ChatResponseDto.builder().build());

        // then
        await().untilAsserted(() ->
                Assertions.assertThat(chatConnectionStore.getEmitters().size()).isEqualTo(0)
        );
    }
}
