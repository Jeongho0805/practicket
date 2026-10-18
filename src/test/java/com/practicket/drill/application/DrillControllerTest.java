package com.practicket.drill.application;

import com.practicket.client.domain.Client;
import com.practicket.client.domain.ClientRepository;
import com.practicket.drill.domain.DrillCaptchaResultRepository;
import com.practicket.drill.domain.DrillCaptchaStatRepository;
import com.practicket.drill.domain.DrillReactResultRepository;
import com.practicket.ticket.scheduler.TicketScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrillControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    DrillReactResultRepository drillReactResultRepository;

    @Autowired
    DrillCaptchaResultRepository drillCaptchaResultRepository;

    @Autowired
    DrillCaptchaStatRepository drillCaptchaStatRepository;

    @MockBean
    TicketScheduler ticketScheduler;

    private String token;

    @BeforeEach
    void setUp() {
        token = newClient();
    }

    @AfterEach
    void tearDown() {
        drillReactResultRepository.deleteAll();
        drillCaptchaResultRepository.deleteAll();
        drillCaptchaStatRepository.deleteAll();
    }

    @Test
    @DisplayName("기록이 없으면 횟수 0, 최고 기록·달성일 없음, 빈 목록을 돌려준다")
    void emptyRecord() throws Exception {
        getRecord("N_TICKET")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.best_ms").value(nullValue()))
                .andExpect(jsonPath("$.best_at").value(nullValue()))
                .andExpect(jsonPath("$.recent", hasSize(0)));
    }

    @Test
    @DisplayName("저장한 기록은 유형별로 나뉘고 최근 10판을 오래된 순으로 돌려준다")
    void recentTenOldestFirst() throws Exception {
        for (int ms = 100; ms <= 1200; ms += 100) {
            save("N_TICKET", ms, null).andExpect(status().isOk());
        }
        save("REFRESH", 50, null).andExpect(status().isOk());

        getRecord("N_TICKET")
                .andExpect(jsonPath("$.count").value(12))
                .andExpect(jsonPath("$.best_ms").value(100))
                .andExpect(jsonPath("$.recent", hasSize(10)))
                .andExpect(jsonPath("$.recent[0].ms").value(300))
                .andExpect(jsonPath("$.recent[9].ms").value(1200));
    }

    @Test
    @DisplayName("같은 최고 기록이 여러 번이면 먼저 낸 판의 날짜를 달성일로 본다")
    void bestAtIsFirstOfTies() throws Exception {
        save("N_TICKET", 300, null);
        String firstAt = recent0CreatedAt("N_TICKET");
        save("N_TICKET", 400, null);
        save("N_TICKET", 300, null);

        getRecord("N_TICKET")
                .andExpect(jsonPath("$.best_ms").value(300))
                .andExpect(jsonPath("$.best_at").value(firstAt));
    }

    @Test
    @DisplayName("저장 응답은 방금 판까지 반영한 기록 카드다")
    void saveReturnsUpdatedRecord() throws Exception {
        save("N_TICKET", 500, null);
        save("N_TICKET", 420, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.best_ms").value(420))
                .andExpect(jsonPath("$.best_at").value(notNullValue()))
                .andExpect(jsonPath("$.recent[1].ms").value(420));
    }

    @Test
    @DisplayName("고급형은 지연값을 함께 저장해 돌려주고, 다른 유형은 null 이다")
    void advancedKeepsLatency() throws Exception {
        save("ADVANCED", 820, 300).andExpect(status().isOk());
        save("REFRESH", 500, null).andExpect(status().isOk());

        getRecord("ADVANCED").andExpect(jsonPath("$.recent[0].latency_ms").value(300));
        getRecord("REFRESH").andExpect(jsonPath("$.recent[0].latency_ms").value(nullValue()));
    }

    @Test
    @DisplayName("고급형에 지연값이 없거나, 다른 유형에 지연값이 있으면 거절한다")
    void latencyMustMatchForm() throws Exception {
        save("ADVANCED", 820, null).andExpect(status().isBadRequest());
        save("REFRESH", 820, 300).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("범위를 벗어난 기록은 거절한다")
    void outOfRange() throws Exception {
        save("N_TICKET", 0, null).andExpect(status().isBadRequest());
        save("N_TICKET", 20_001, null).andExpect(status().isBadRequest());
        save("ADVANCED", 900, 5_001).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 사람의 기록은 보이지 않는다")
    void onlyMine() throws Exception {
        save("N_TICKET", 300, null);

        mockMvc.perform(get("/api/drill/react/record").param("form", "N_TICKET")
                        .header("Authorization", "Bearer " + newClient()))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("허브 요약 API 는 없어졌다")
    void summaryRemoved() throws Exception {
        mockMvc.perform(get("/api/drill/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String newClient() {
        String t = UUID.randomUUID().toString();
        clientRepository.save(Client.builder()
                .ip("test").token(t).device("test").name("tester").referer("test").banned(false)
                .build());
        return t;
    }

    private String recent0CreatedAt(String form) throws Exception {
        String body = getRecord(form).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.recent[0].created_at");
    }

    private ResultActions save(String form, int reactionMs, Integer latencyMs) throws Exception {
        String latency = latencyMs == null ? "null" : String.valueOf(latencyMs);
        String body = """
                {"form":"%s","reaction_ms":%d,"latency_ms":%s}
                """.formatted(form, reactionMs, latency);
        return mockMvc.perform(post("/api/drill/react/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getRecord(String form) throws Exception {
        return mockMvc.perform(get("/api/drill/react/record")
                .param("form", form)
                .header("Authorization", "Bearer " + token));
    }
}
