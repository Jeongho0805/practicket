package com.practicket.drill.application;

import com.practicket.client.domain.Client;
import com.practicket.client.domain.ClientRepository;
import com.practicket.drill.domain.DrillGrapeResultRepository;
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

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
class DrillGrapeControllerTest {

    private static final int[] SEATS = {4, 5, 6, 7, 8};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    DrillGrapeResultRepository drillGrapeResultRepository;

    @MockBean
    TicketScheduler ticketScheduler;

    private String token;

    @BeforeEach
    void setUp() {
        token = newClient();
    }

    @AfterEach
    void tearDown() {
        drillGrapeResultRepository.deleteAll();
    }

    @Test
    @DisplayName("좌석 30석 합으로 평균을 내고 전체 시간·오클릭·라운드 원본을 저장한다")
    void saveComputesAveragePerSeat() throws Exception {
        save("GENERAL", rounds(new int[]{1900, 2300, 2800, 3300, 4700}, new int[]{0, 1, 0, 1, 0}, new int[]{0, 1, 0, 0, 0}))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.best.ms").value(500))
                .andExpect(jsonPath("$.best.misclicks").value(2))
                .andExpect(jsonPath("$.best.missed").value(nullValue()))
                .andExpect(jsonPath("$.clean_best").value(nullValue()))
                .andExpect(jsonPath("$.recent", hasSize(1)))
                .andExpect(jsonPath("$.recent[0].ms").value(500));

        var saved = drillGrapeResultRepository.findAll().get(0);
        assertThat(saved.getTotalMs()).isEqualTo(15000);
        assertThat(saved.getSeatCount()).isEqualTo(30);
        assertThat(saved.getRounds()).contains("\"sold_clicks\":1");
    }

    @Test
    @DisplayName("기록이 없으면 0회, 두 최고 기록 없음, 빈 목록이다")
    void emptyRecord() throws Exception {
        getRecord("GENERAL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.best").value(nullValue()))
                .andExpect(jsonPath("$.clean_best").value(nullValue()))
                .andExpect(jsonPath("$.recent", hasSize(0)));
    }

    @Test
    @DisplayName("최근 10판만 오래된 순으로 주고, 최고 기록과 횟수는 전체에서 구한다")
    void recentTenButBestFromAll() throws Exception {
        saveAverage("GENERAL", 300, 0);
        for (int i = 0; i < 11; i++) {
            saveAverage("GENERAL", 500 + i * 10, 1);
        }

        getRecord("GENERAL")
                .andExpect(jsonPath("$.count").value(12))
                .andExpect(jsonPath("$.best.ms").value(300))
                .andExpect(jsonPath("$.clean_best.ms").value(300))
                .andExpect(jsonPath("$.recent", hasSize(10)))
                .andExpect(jsonPath("$.recent[0].ms").value(510))
                .andExpect(jsonPath("$.recent[9].ms").value(600));
    }

    @Test
    @DisplayName("오클릭 없는 최고는 오클릭 0 판 중에서 고르고, 동률이면 먼저 낸 판이다")
    void cleanBestAndTies() throws Exception {
        saveAverage("GENERAL", 400, 2);
        saveAverage("GENERAL", 450, 0);
        String firstClean = recentLastCreatedAt("GENERAL");
        saveAverage("GENERAL", 450, 0);

        getRecord("GENERAL")
                .andExpect(jsonPath("$.best.ms").value(400))
                .andExpect(jsonPath("$.best.misclicks").value(2))
                .andExpect(jsonPath("$.clean_best.ms").value(450))
                .andExpect(jsonPath("$.clean_best.created_at").value(firstClean));
    }

    @Test
    @DisplayName("유형과 사람이 다르면 기록이 섞이지 않는다")
    void separatedByFormAndClient() throws Exception {
        saveAverage("GENERAL", 400, 0);
        saveAverage("N_TICKET", 300, 0);

        getRecord("GENERAL").andExpect(jsonPath("$.count").value(1)).andExpect(jsonPath("$.best.ms").value(400));
        mockMvc.perform(get("/api/drill/grape/record").param("form", "GENERAL")
                        .header("Authorization", "Bearer " + newClient()))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("라운드 수, 좌석 수 순서, 범위가 어긋나면 거절한다")
    void rejectsInvalid() throws Exception {
        int[] ms = {1000, 1000, 1000, 1000, 1000}, zero = {0, 0, 0, 0, 0};
        save("GENERAL", rounds(ms, zero, zero).replaceFirst("\\{[^}]*},", "")).andExpect(status().isBadRequest());
        save("GENERAL", "[" + roundsBody(ms, zero, zero) + ",{\"seats\":8,\"elapsed_ms\":1000,\"misclicks\":0,\"sold_clicks\":0}]")
                .andExpect(status().isBadRequest());
        save("GENERAL", rounds(ms, zero, zero).replace("\"seats\":4", "\"seats\":5")).andExpect(status().isBadRequest());
        save("GENERAL", rounds(new int[]{99, 1000, 1000, 1000, 1000}, zero, zero)).andExpect(status().isBadRequest());
        save("GENERAL", rounds(new int[]{600_001, 1000, 1000, 1000, 1000}, zero, zero)).andExpect(status().isBadRequest());
        save("GENERAL", rounds(ms, new int[]{-1, 0, 0, 0, 0}, zero)).andExpect(status().isBadRequest());
        save("GENERAL", rounds(ms, new int[]{1, 0, 0, 0, 0}, new int[]{2, 0, 0, 0, 0})).andExpect(status().isBadRequest());
        save(null, rounds(ms, zero, zero)).andExpect(status().isBadRequest());
        assertThat(drillGrapeResultRepository.count()).isZero();
    }

    private void saveAverage(String form, int averageMs, int misclicks) throws Exception {
        int[] ms = Arrays.stream(SEATS).map(s -> s * averageMs).toArray();
        save(form, rounds(ms, new int[]{misclicks, 0, 0, 0, 0}, new int[]{0, 0, 0, 0, 0})).andExpect(status().isOk());
    }

    private static String rounds(int[] ms, int[] misclicks, int[] sold) {
        return "[" + roundsBody(ms, misclicks, sold) + "]";
    }

    private static String roundsBody(int[] ms, int[] misclicks, int[] sold) {
        return IntStream.range(0, 5)
                .mapToObj(i -> "{\"seats\":%d,\"elapsed_ms\":%d,\"misclicks\":%d,\"sold_clicks\":%d}"
                        .formatted(SEATS[i], ms[i], misclicks[i], sold[i]))
                .collect(Collectors.joining(","));
    }

    private ResultActions save(String form, String rounds) throws Exception {
        String formJson = form == null ? "null" : "\"" + form + "\"";
        return mockMvc.perform(post("/api/drill/grape/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"form\":%s,\"rounds\":%s}".formatted(formJson, rounds)));
    }

    private ResultActions getRecord(String form) throws Exception {
        return mockMvc.perform(get("/api/drill/grape/record").param("form", form)
                .header("Authorization", "Bearer " + token));
    }

    private String recentLastCreatedAt(String form) throws Exception {
        String body = getRecord(form).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.recent[-1].created_at");
    }

    private String newClient() {
        String t = UUID.randomUUID().toString();
        clientRepository.save(Client.builder()
                .ip("test").token(t).device("test").name("tester").referer("test").banned(false)
                .build());
        return t;
    }
}
