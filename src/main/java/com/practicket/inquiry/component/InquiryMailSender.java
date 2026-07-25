package com.practicket.inquiry.component;

import com.practicket.inquiry.domain.Inquiry;
import com.practicket.inquiry.domain.InquiryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 문의 접수 시 운영자에게 알림 메일을 보낸다(비동기, best-effort).
 * 메일 발송이 실패해도 문의 저장에는 영향을 주지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryMailSender {

    private final JavaMailSender mailSender;

    @Value("${app.inquiry.notify-to:}")
    private String notifyTo;

    @Value("${spring.mail.username:}")
    private String from;

    @Async
    public void notifyOperator(Inquiry inquiry) {
        if (notifyTo == null || notifyTo.isBlank()) {
            log.warn("[Inquiry] app.inquiry.notify-to 미설정 - 알림 메일 생략 (id={})", inquiry.getId());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(notifyTo);
            message.setReplyTo(inquiry.getEmail());
            message.setSubject("[프랙티켓 문의] " + typeLabel(inquiry.getType()));
            message.setText(buildBody(inquiry));
            mailSender.send(message);
            log.info("[Inquiry] 알림 메일 발송 완료 (id={})", inquiry.getId());
        } catch (Exception e) {
            log.error("[Inquiry] 알림 메일 발송 실패 (id={})", inquiry.getId(), e);
        }
    }

    private String typeLabel(InquiryType type) {
        return type == InquiryType.AD ? "광고·제휴" : "불편·건의";
    }

    private String buildBody(Inquiry inquiry) {
        return "유형: " + typeLabel(inquiry.getType()) + "\n"
                + "회신 이메일: " + inquiry.getEmail() + "\n"
                + "접수 시각: " + inquiry.getCreatedAt() + "\n"
                + "-----------------------------\n"
                + inquiry.getContent();
    }
}
