package com.practicket.drill.application;

import com.practicket.drill.domain.DrillActivityQuery;
import com.practicket.drill.domain.DrillCaptchaResult;
import com.practicket.drill.domain.DrillCaptchaResultRepository;
import com.practicket.drill.domain.DrillCancelResult;
import com.practicket.drill.domain.DrillCancelResultRepository;
import com.practicket.drill.domain.DrillGrapeResult;
import com.practicket.drill.domain.DrillGrapeResultRepository;
import com.practicket.drill.domain.DrillReactForm;
import com.practicket.drill.domain.DrillReactResult;
import com.practicket.drill.domain.DrillReactResultRepository;
import com.practicket.drill.domain.DrillSeatForm;
import com.practicket.ticket.scheduler.TicketScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrillActivityControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DrillReactResultRepository drillReactResultRepository;

    @Autowired
    DrillCaptchaResultRepository drillCaptchaResultRepository;

    @Autowired
    DrillGrapeResultRepository drillGrapeResultRepository;

    @Autowired
    DrillCancelResultRepository drillCancelResultRepository;

    @Autowired
    DrillActivityService drillActivityService;

    @Autowired
    DrillActivityQuery drillActivityQuery;

    @MockBean
    TicketScheduler ticketScheduler;

    @BeforeEach
    void setUp() {
        drillActivityService.clear();
    }

    @AfterEach
    void tearDown() {
        drillReactResultRepository.deleteAll();
        drillCaptchaResultRepository.deleteAll();
        drillGrapeResultRepository.deleteAll();
        drillCancelResultRepository.deleteAll();
    }

    @Test
    @DisplayName("아무도 연습하지 않았으면 모두 0이고, 토큰 없이 조회된다")
    void empty() throws Exception {
        mockMvc.perform(get("/api/drill/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players").value(0))
                .andExpect(jsonPath("$.rounds").value(0))
                .andExpect(jsonPath("$.react.players").value(0))
                .andExpect(jsonPath("$.captcha.rounds").value(0))
                .andExpect(jsonPath("$.grape.players").value(0))
                .andExpect(jsonPath("$.cancel.rounds").value(0));
    }

    @Test
    @DisplayName("사람 수는 중복을 지우고, 두 종목을 다 한 사람도 전체에서는 1명이다")
    void countsPlayersOnceAcrossDrills() throws Exception {
        react(1L);
        react(1L);
        react(2L);
        captcha(2L);
        captcha(3L);
        captcha(3L);

        mockMvc.perform(get("/api/drill/activity"))
                .andExpect(jsonPath("$.players").value(3))
                .andExpect(jsonPath("$.rounds").value(6))
                .andExpect(jsonPath("$.react.players").value(2))
                .andExpect(jsonPath("$.react.rounds").value(3))
                .andExpect(jsonPath("$.captcha.players").value(2))
                .andExpect(jsonPath("$.captcha.rounds").value(3));
    }

    @Test
    @DisplayName("네 종목을 합쳐도 한 사람은 한 번만 세고, 종목별 칸도 따로 센다")
    void countsPlayersOnceAcrossFourDrills() throws Exception {
        react(1L);
        captcha(1L);
        grape(1L);
        grape(2L);
        grape(2L);
        cancel(1L);
        cancel(3L);

        mockMvc.perform(get("/api/drill/activity"))
                .andExpect(jsonPath("$.players").value(3))
                .andExpect(jsonPath("$.rounds").value(7))
                .andExpect(jsonPath("$.grape.players").value(2))
                .andExpect(jsonPath("$.grape.rounds").value(3))
                .andExpect(jsonPath("$.cancel.players").value(2))
                .andExpect(jsonPath("$.cancel.rounds").value(2));
    }

    @Test
    @DisplayName("기준 시각 이전 기록은 세지 않는다")
    void excludesBeforeStart() {
        react(1L);
        captcha(2L);

        assertThat(drillActivityQuery.since(LocalDate.now().plusDays(1).atStartOfDay()).players()).isZero();
        assertThat(drillActivityQuery.since(LocalDate.now().atStartOfDay()).players()).isEqualTo(2);
    }

    private void react(Long clientId) {
        drillReactResultRepository.save(DrillReactResult.builder()
                .clientId(clientId).form(DrillReactForm.N_TICKET).reactionMs(300).build());
    }

    private void captcha(Long clientId) {
        drillCaptchaResultRepository.save(DrillCaptchaResult.builder()
                .clientId(clientId).averageMs(4000).problems("[]").build());
    }

    private void grape(Long clientId) {
        drillGrapeResultRepository.save(DrillGrapeResult.builder()
                .clientId(clientId).form(DrillSeatForm.GENERAL).averageMs(500).totalMs(15000).seatCount(30)
                .misclicks(0).rounds("[]").build());
    }

    private void cancel(Long clientId) {
        drillCancelResultRepository.save(DrillCancelResult.builder()
                .clientId(clientId).form(DrillSeatForm.N_TICKET).averageMs(600).totalMs(3000)
                .misclicks(0).rounds("[]").build());
    }
}
