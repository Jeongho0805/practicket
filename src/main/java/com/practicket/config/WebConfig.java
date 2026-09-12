package com.practicket.config;

import com.practicket.ad.admin.AdminAuthInterceptor;
import com.practicket.client.domain.ClientRepository;
import com.practicket.common.auth.ClientInfoArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig  {

    private final ClientRepository clientRepository;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Value("${app.ad.image-dir:/data/practicket/ad-images}")
    private String adImageDir;

    @Value("${app.blog.image-dir:/data/practicket/blog-images}")
    private String blogImageDir;

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

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // 어드민 페이지 세션 인증 가드
                registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/admin-hoya/**");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // 업로드된 배너 이미지 서빙 (BannerImageStorage 저장 경로 → /ad-images/**)
                registry.addResourceHandler("/ad-images/**")
                        .addResourceLocations("file:" + adImageDir + "/");

                // 블로그 썸네일·본문 이미지 서빙. 기존 15건 이미지도 static 이 아니라
                // 이쪽에 있다 — 어드민 업로드분과 저장 위치를 나누지 않기 위해서다
                registry.addResourceHandler("/blog-images/**")
                        .addResourceLocations("file:" + blogImageDir + "/");
            }
        };
    }
}
