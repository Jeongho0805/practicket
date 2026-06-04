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

    private final WebClient webClient;

    private static final Duration TIMEOUT = Duration.ofMillis(500);

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
