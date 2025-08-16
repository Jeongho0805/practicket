package com.example.ticketing.api.profanity;

import com.example.ticketing.api.profanity.dto.ValidationRequest;
import com.example.ticketing.api.profanity.dto.ValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ProfanityApiClient {

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
                .bodyToMono(ValidationResponse.class);
    }
}
