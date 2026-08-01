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

    // 제목에 붙일 내용 요약 길이. 지메일 목록이 잘라 보여주는 폭을 넘지 않게 짧게 둔다.
    private static final int SUBJECT_SUMMARY_LENGTH = 40;

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
            message.setSubject("[프랙티켓 문의] " + typeLabel(inquiry.getType()) + " - " + summary(inquiry.getContent()));
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

    /**
     * 제목 뒤에 붙일 한 줄 요약. 메일 목록에서 열어보지 않고도 무슨 문의인지 알 수 있게 한다.
     * 줄바꿈이 들어가면 제목이 깨지므로 공백으로 눕히고, 길면 잘라낸다.
     */
    private String summary(String content) {
        String oneLine = content.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= SUBJECT_SUMMARY_LENGTH
                ? oneLine
                : oneLine.substring(0, SUBJECT_SUMMARY_LENGTH) + "…";
    }

    // 본문은 내용을 맨 위에 둔다. 메일 목록의 미리보기가 앞부분만 보여주기 때문에,
    // 메타데이터를 위에 두면 어느 문의든 "유형: ... 회신 이메일: ..." 로만 보인다.
    private String buildBody(Inquiry inquiry) {
        return inquiry.getContent() + "\n"
                + "-----------------------------\n"
                + "유형: " + typeLabel(inquiry.getType()) + "\n"
                + "회신 이메일: " + inquiry.getEmail() + "\n"
                + "접수 시각: " + inquiry.getCreatedAt() + "\n";
    }
}
