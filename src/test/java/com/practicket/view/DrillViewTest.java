package com.practicket.view;

import com.practicket.ticket.scheduler.TicketScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrillViewTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TicketScheduler ticketScheduler;

    @Test
    @DisplayName("옛 보안문자 주소는 반복연습 보안문자로 영구 이동한다")
    void securityMovedPermanently() throws Exception {
        mockMvc.perform(get("/security"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/drill/captcha"));
    }

    @Test
    @DisplayName("반복연습 화면은 네 종목과 허브 템플릿을 그린다")
    void drillPages() throws Exception {
        mockMvc.perform(get("/drill")).andExpect(status().isOk()).andExpect(view().name("drill/hub"));
        mockMvc.perform(get("/drill/react")).andExpect(status().isOk()).andExpect(view().name("drill/react"));
        mockMvc.perform(get("/drill/captcha")).andExpect(status().isOk()).andExpect(view().name("drill/captcha"));
        mockMvc.perform(get("/drill/grape")).andExpect(status().isOk()).andExpect(view().name("drill/grape"));
        mockMvc.perform(get("/drill/cancel")).andExpect(status().isOk()).andExpect(view().name("drill/cancel"));
    }
}
