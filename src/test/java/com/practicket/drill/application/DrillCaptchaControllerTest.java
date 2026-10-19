package com.practicket.drill.application;

import com.practicket.client.domain.Client;
import com.practicket.client.domain.ClientRepository;
import com.practicket.drill.domain.DrillCaptchaResultRepository;
import com.practicket.drill.domain.DrillCaptchaStatRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrillCaptchaControllerTest {

    private static final String ROUND = """
            {"problems":[
              {"answer":"KWQZMT","elapsed_ms":4120,"typos":[{"position":1,"typed":"V"}]},
              {"answer":"HXRANW","elapsed_ms":5380,"typos":[{"position":1,"typed":"Z"},{"position":5,"typed":"V"}]},
              {"answer":"PDGLSE","elapsed_ms":3280,"typos":[{"position":0,"typed":"한"}]}]}
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientRepository clientRepository;

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
        drillCaptchaResultRepository.deleteAll();
        drillCaptchaStatRepository.deleteAll();
    }

    @Test
    @DisplayName("세 문제 평균을 서버가 계산해 저장하고 갱신된 기록 카드를 돌려준다")
    void saveComputesAverage() throws Exception {
        save(ROUND)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.best_ms").value(4260))
                .andExpect(jsonPath("$.recent", hasSize(1)))
                .andExpect(jsonPath("$.recent[0].ms").value(4260))
                .andExpect(jsonPath("$.recent[0].latency_ms").doesNotExist());
    }

    @Test
    @DisplayName("세 문제 원본을 판 기록에 남긴다")
    void keepsProblems() throws Exception {
        save(ROUND).andExpect(status().isOk());

        String problems = drillCaptchaResultRepository.findAll().get(0).getProblems();
        assertThat(problems).contains("\"answer\":\"HXRANW\"", "\"elapsed_ms\":5380", "\"typed\":\"Z\"");
    }

    @Test
    @DisplayName("기록이 없으면 빈 기록 카드를 돌려준다")
    void emptyRecord() throws Exception {
        mockMvc.perform(get("/api/drill/captcha/record").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.best_ms").value(nullValue()))
                .andExpect(jsonPath("$.recent", hasSize(0)));
    }

    @Test
    @DisplayName("누적 오타는 첫 판에 행을 만들고 다음 판부터 같은 행에 더한다")
    void accumulatesTypos() throws Exception {
        save(ROUND).andExpect(status().isOk());
        save(ROUND).andExpect(status().isOk());

        assertThat(drillCaptchaStatRepository.count()).isEqualTo(1);
        typoStats()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ime_count").value(2))
                .andExpect(jsonPath("$.letters[?(@.letter == 'W')].seen", contains(4)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'W')].slots", contains(4)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'W')].miss", contains(4)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'W')].typed[0].letter", contains("V")))
                .andExpect(jsonPath("$.letters[?(@.letter == 'W')].typed[0].count", contains(4)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'X')].typed[0].letter", contains("Z")))
                .andExpect(jsonPath("$.letters[?(@.letter == 'P')].miss", contains(0)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'P')].slots", contains(0)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'K')].seen", contains(2)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'K')].typed", contains(hasSize(0))));
    }

    @Test
    @DisplayName("한 자리에서 여러 번 틀리면 실수한 자리는 1, 대신 친 글자는 친 만큼 센다")
    void repeatedTyposInOneSlot() throws Exception {
        save(round("BYVKHE", 4000, "[{\"position\":0,\"typed\":\"P\"},{\"position\":0,\"typed\":\"P\"},{\"position\":0,\"typed\":\"P\"}]"))
                .andExpect(status().isOk());

        typoStats()
                .andExpect(jsonPath("$.letters[?(@.letter == 'B')].seen", contains(1)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'B')].slots", contains(1)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'B')].miss", contains(3)))
                .andExpect(jsonPath("$.letters[?(@.letter == 'B')].typed[0].count", contains(3)));
    }

    @Test
    @DisplayName("오타 통계는 사람마다 따로 쌓이고, 기록이 없으면 빈 응답이다")
    void typoStatsPerClient() throws Exception {
        save(ROUND).andExpect(status().isOk());

        mockMvc.perform(get("/api/drill/captcha/typo-stats").header("Authorization", "Bearer " + newClient()))
                .andExpect(jsonPath("$.letters", hasSize(0)))
                .andExpect(jsonPath("$.ime_count").value(0));
    }

    @Test
    @DisplayName("문제가 3개가 아니면 거절한다")
    void problemCount() throws Exception {
        save("""
                {"problems":[
                  {"answer":"KWQZMT","elapsed_ms":4000,"typos":[]},
                  {"answer":"HXRANW","elapsed_ms":4000,"typos":[]}]}
                """).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정답 형식·위치·같은 글자 오타·짧은 경과는 거절한다")
    void invalidProblem() throws Exception {
        save(round("KWQZMO", 4000, "[]")).andExpect(status().isBadRequest());
        save(round("kwqzmt", 4000, "[]")).andExpect(status().isBadRequest());
        save(round("KWQZMT", 4000, "[{\"position\":6,\"typed\":\"V\"}]")).andExpect(status().isBadRequest());
        save(round("KWQZMT", 4000, "[{\"position\":1,\"typed\":\"W\"}]")).andExpect(status().isBadRequest());
        save(round("KWQZMT", 4000, "[{\"position\":1,\"typed\":\"가\"}]")).andExpect(status().isBadRequest());
        save(round("KWQZMT", 299, "[]")).andExpect(status().isBadRequest());
        assertThat(drillCaptchaResultRepository.count()).isZero();
    }

    @Test
    @DisplayName("세 문제 합계가 1분을 넘으면 거절한다")
    void overTimeLimit() throws Exception {
        save("""
                {"problems":[
                  {"answer":"KWQZMT","elapsed_ms":20000,"typos":[]},
                  {"answer":"HXRANW","elapsed_ms":20000,"typos":[]},
                  {"answer":"PDGLSE","elapsed_ms":20001,"typos":[]}]}
                """).andExpect(status().isBadRequest());
        assertThat(drillCaptchaResultRepository.count()).isZero();
        assertThat(drillCaptchaStatRepository.count()).isZero();
    }

    private String round(String firstAnswer, int firstElapsed, String firstTypos) {
        return """
                {"problems":[
                  {"answer":"%s","elapsed_ms":%d,"typos":%s},
                  {"answer":"HXRANW","elapsed_ms":4000,"typos":[]},
                  {"answer":"PDGLSE","elapsed_ms":4000,"typos":[]}]}
                """.formatted(firstAnswer, firstElapsed, firstTypos);
    }

    private String newClient() {
        String t = UUID.randomUUID().toString();
        clientRepository.save(Client.builder()
                .ip("test").token(t).device("test").name("tester").referer("test").banned(false)
                .build());
        return t;
    }

    private ResultActions save(String body) throws Exception {
        return mockMvc.perform(post("/api/drill/captcha/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions typoStats() throws Exception {
        return mockMvc.perform(get("/api/drill/captcha/typo-stats").header("Authorization", "Bearer " + token));
    }
}
