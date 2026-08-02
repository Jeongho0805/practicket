package com.practicket.api.profanity;

import com.practicket.api.profanity.dto.ValidationRequest;
import com.practicket.api.profanity.dto.ValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class ProfanityApiClient {

    /**
     * 욕설 판정은 **없어도 되는 기능**이다. 외부 API 가 에러를 주는 것보다 위험한 건
     * 연결만 받아두고 응답을 안 주는 경우다 — 타임아웃이 없으면 요청 스레드가 그대로 붙잡히고,
     * 그런 요청이 쌓이면 욕설 하나 때문에 서비스 전체가 멈춘다.
     *
     * 그래서 1초에서 끊는다. 끊긴 뒤 통과시키는 처리는 {@code ProfanityValidator} 가 한다(fail-open).
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final WebClient webClient;

    public ProfanityApiClient(WebClient.Builder webClientBuilder, @Value("${api.profanity.url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<ValidationResponse> validate(String text) {
        ValidationRequest request = new ValidationRequest(text);
        return this.webClient.post()
                .uri("/")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ValidationResponse.class)
                .timeout(TIMEOUT);
    }
}
