package com.practicket.config;

import com.practicket.captcha.component.CaptchaStatInvalidationSubscriber;
import com.practicket.chat.component.ChatMessageSubscriber;
import com.practicket.chat.component.ChatParticipantSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class RedisConfig {

    private static final int LISTENER_POOL_SIZE = 8;
    private static final int LISTENER_QUEUE_CAPACITY = 1_000;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic("chat:messages");
    }

    @Bean
    public ChannelTopic chatParticipantTopic() {
        return new ChannelTopic("chat:participants:changed");
    }

    @Bean
    public ChannelTopic captchaStatTopic() {
        return new ChannelTopic("captcha:stat:invalidated");
    }

    @Bean
    public MessageListenerAdapter chatListenerAdapter(ChatMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public MessageListenerAdapter chatParticipantListenerAdapter(ChatParticipantSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public MessageListenerAdapter captchaStatListenerAdapter(CaptchaStatInvalidationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    /**
     * 지정하지 않으면 컨테이너가 메시지마다 스레드를 새로 만들고 재사용하지 않는다.
     * 전송이 막히면 그 스레드가 회수되지 않아 무한히 쌓인다(2026-08-24 사고).
     * 넘칠 때는 버린다 — 구독 스레드를 붙잡거나 예외를 흘리면 pub/sub 전체가 멈춘다.
     */
    @Bean
    public ThreadPoolTaskExecutor listenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(LISTENER_POOL_SIZE);
        executor.setMaxPoolSize(LISTENER_POOL_SIZE);
        executor.setQueueCapacity(LISTENER_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("redis-listener-");
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("Redis 리스너 대기줄이 가득 차 메시지를 버렸다 — 활성 {}, 대기 {}",
                        pool.getActiveCount(), pool.getQueue().size()));
        executor.initialize();
        return executor;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter chatListenerAdapter,
            MessageListenerAdapter chatParticipantListenerAdapter,
            MessageListenerAdapter captchaStatListenerAdapter,
            ChannelTopic chatTopic,
            ChannelTopic chatParticipantTopic,
            ChannelTopic captchaStatTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.setTaskExecutor(listenerExecutor());
        container.addMessageListener(chatListenerAdapter, chatTopic);
        container.addMessageListener(chatParticipantListenerAdapter, chatParticipantTopic);
        container.addMessageListener(captchaStatListenerAdapter, captchaStatTopic);
        return container;
    }
}
