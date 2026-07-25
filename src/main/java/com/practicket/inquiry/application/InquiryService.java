package com.practicket.inquiry.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.inquiry.component.InquiryMailSender;
import com.practicket.inquiry.component.InquiryRateLimiter;
import com.practicket.inquiry.domain.Inquiry;
import com.practicket.inquiry.domain.InquiryRepository;
import com.practicket.inquiry.dto.InquiryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final int DAILY_LIMIT = 3;

    private final InquiryRepository inquiryRepository;
    private final InquiryRateLimiter rateLimiter;
    private final InquiryMailSender mailSender;

    @Transactional
    public void submit(InquiryRequest request, ClientInfo clientInfo) {
        String token = clientInfo.getToken();

        // 1) 연타 방지 (30초 1건)
        rateLimiter.validateBurst(token);

        // 2) 하루 상한 (3건)
        long todayCount = inquiryRepository.countByClientTokenAndCreatedAtAfter(
                token, LocalDateTime.now().minusDays(1));
        if (todayCount >= DAILY_LIMIT) {
            throw new GlobalException(ErrorCode.INQUIRY_DAILY_LIMIT_EXCEEDED);
        }

        // 3) 저장
        Inquiry inquiry = inquiryRepository.save(Inquiry.builder()
                .type(request.getType())
                .email(request.getEmail())
                .content(request.getContent())
                .clientToken(token)
                .build());

        // 4) 운영자 알림 (비동기, best-effort)
        mailSender.notifyOperator(inquiry);
    }
}
