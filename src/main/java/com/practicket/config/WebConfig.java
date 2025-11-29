package com.practicket.config;

import com.practicket.client.domain.ClientRepository;
import com.practicket.common.auth.ClientInfoArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig  {

    private final ClientRepository clientRepository;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:8080", "https://ticketing.ddns.net", "https://practicket.com", "https://stage.practicket.com")
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                // 토큰 인증
                resolvers.add(new ClientInfoArgumentResolver(clientRepository));

                // Pageable 최대 size 크기 제한
                PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
                pageableResolver.setMaxPageSize(100);
                pageableResolver.setFallbackPageable(PageRequest.of(0, 20));
                resolvers.add(pageableResolver);
            }
        };
    }
}
