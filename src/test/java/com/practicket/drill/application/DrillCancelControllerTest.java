package com.practicket.drill.application;

import com.practicket.client.domain.Client;
import com.practicket.client.domain.ClientRepository;
import com.practicket.drill.domain.DrillCancelResultRepository;
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

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrillCancelControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    DrillCancelResultRepository drillCancelResultRepository;

    @MockBean
    TicketScheduler ticketScheduler;

    private String token;

    @BeforeEach
    void setUp() {
        token = newClient();
    }

    @AfterEach
    void tearDown() {
        drillCancelResultRepository.deleteAll();
    }

    @Test
    @DisplayName("일반형은 다섯 라운드 합 ÷ 5 를 저장하고 놓친 취소표를 합해 둔다")
    void generalAverageAndMissed() throws Exception {
        save("GENERAL", general(2000, 1, 0) + "," + general(2100, 0, 2) + "," + general(1900, 0, 0) + ","
                + general(2200, 0, 0) + "," + general(2300, 0, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.best.ms").value(2100))
                .andExpect(jsonPath("$.best.misclicks").value(1))
                .andExpect(jsonPath("$.best.missed").value(2))
                .andExpect(jsonPath("$.clean_best").value(nullValue()))
                .andExpect(jsonPath("$.recent[0].missed").value(2));

        var saved = drillCancelResultRepository.findAll().get(0);
        assertThat(saved.getTotalMs()).isEqualTo(10500);
        assertThat(saved.getMissedTickets()).isEqualTo(2);
        assertThat(saved.getRounds()).contains("\"discover_ms\":400");
    }

    @Test
    @DisplayName("N형은 놓친 취소표를 비워 두고, 오클릭 없는 판이 정확한 최고다")
    void nTicketHasNoMissed() throws Exception {
        save("N_TICKET", repeat(nTicket(600, 1))).andExpect(status().isOk());
        save("N_TICKET", repeat(nTicket(700, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.best.ms").value(600))
                .andExpect(jsonPath("$.best.missed").value(nullValue()))
                .andExpect(jsonPath("$.clean_best.ms").value(700))
                .andExpect(jsonPath("$.recent", hasSize(2)));

        assertThat(drillCancelResultRepository.findAll()).allMatch(r -> r.getMissedTickets() == null);
    }

    @Test
    @DisplayName("일반형 정확한 최고는 오클릭도 놓침도 없는 판에서만 고른다")
    void generalCleanBestExcludesMissed() throws Exception {
        save("GENERAL", repeat(general(1500, 0, 1))).andExpect(status().isOk());
        save("GENERAL", repeat(general(1800, 1, 0))).andExpect(status().isOk());
        save("GENERAL", repeat(general(2000, 0, 0))).andExpect(status().isOk());

        getRecord("GENERAL")
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.best.ms").value(1500))
                .andExpect(jsonPath("$.clean_best.ms").value(2000));
    }

    @Test
    @DisplayName("유형이 다르면 기록이 섞이지 않는다")
    void separatedByForm() throws Exception {
        save("GENERAL", repeat(general(2000, 0, 0))).andExpect(status().isOk());

        getRecord("N_TICKET").andExpect(jsonPath("$.count").value(0));
        getRecord("GENERAL").andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("일반형 구간 합이 전체 이상이거나 범위를 벗어나면 거절한다")
    void rejectsInvalidGeneral() throws Exception {
        String overlap = "{\"elapsed_ms\":1000,\"discover_ms\":400,\"select_ms\":600,\"misclicks\":0,\"sold_clicks\":0,\"missed\":0}";
        save("GENERAL", repeat(overlap)).andExpect(status().isBadRequest());
        save("GENERAL", repeat(general(99, 0, 0))).andExpect(status().isBadRequest());
        save("GENERAL", repeat(general(600_001, 0, 0))).andExpect(status().isBadRequest());
        save("GENERAL", repeat(general(2000, 0, 101))).andExpect(status().isBadRequest());
        save("GENERAL", String.join(",", Collections.nCopies(4, general(2000, 0, 0)))).andExpect(status().isBadRequest());
        save("GENERAL", String.join(",", Collections.nCopies(6, general(2000, 0, 0)))).andExpect(status().isBadRequest());
        assertThat(drillCancelResultRepository.count()).isZero();
    }

    @Test
    @DisplayName("N형은 발견 구간이 있거나, 선택 시간이 전체와 다르거나, 놓침이 있으면 거절한다")
    void rejectsInvalidNTicket() throws Exception {
        save("N_TICKET", repeat("{\"elapsed_ms\":640,\"discover_ms\":10,\"select_ms\":630,\"misclicks\":0,\"sold_clicks\":0,\"missed\":0}"))
                .andExpect(status().isBadRequest());
        save("N_TICKET", repeat("{\"elapsed_ms\":640,\"discover_ms\":0,\"select_ms\":600,\"misclicks\":0,\"sold_clicks\":0,\"missed\":0}"))
                .andExpect(status().isBadRequest());
        save("N_TICKET", repeat("{\"elapsed_ms\":640,\"discover_ms\":0,\"select_ms\":640,\"misclicks\":0,\"sold_clicks\":0,\"missed\":1}"))
                .andExpect(status().isBadRequest());
        save("N_TICKET", repeat(nTicket(49, 0))).andExpect(status().isBadRequest());
        save("N_TICKET", repeat("{\"elapsed_ms\":640,\"discover_ms\":0,\"select_ms\":640,\"misclicks\":1,\"sold_clicks\":2,\"missed\":0}"))
                .andExpect(status().isBadRequest());
        assertThat(drillCancelResultRepository.count()).isZero();
    }

    private static String general(int elapsedMs, int misclicks, int missed) {
        return "{\"elapsed_ms\":%d,\"discover_ms\":%d,\"select_ms\":%d,\"misclicks\":%d,\"sold_clicks\":0,\"missed\":%d}"
                .formatted(elapsedMs, elapsedMs / 5, elapsedMs / 2, misclicks, missed);
    }

    private static String nTicket(int elapsedMs, int misclicks) {
        return "{\"elapsed_ms\":%d,\"discover_ms\":0,\"select_ms\":%d,\"misclicks\":%d,\"sold_clicks\":0,\"missed\":0}"
                .formatted(elapsedMs, elapsedMs, misclicks);
    }

    private static String repeat(String round) {
        return String.join(",", Collections.nCopies(5, round));
    }

    private ResultActions save(String form, String rounds) throws Exception {
        return mockMvc.perform(post("/api/drill/cancel/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"form\":\"%s\",\"rounds\":[%s]}".formatted(form, rounds)));
    }

    private ResultActions getRecord(String form) throws Exception {
        return mockMvc.perform(get("/api/drill/cancel/record").param("form", form)
                .header("Authorization", "Bearer " + token));
    }

    private String newClient() {
        String t = UUID.randomUUID().toString();
        clientRepository.save(Client.builder()
                .ip("test").token(t).device("test").name("tester").referer("test").banned(false)
                .build());
        return t;
    }
}
