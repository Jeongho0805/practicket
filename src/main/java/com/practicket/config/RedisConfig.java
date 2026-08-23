package com.practicket.config;

import com.practicket.captcha.component.CaptchaStatInvalidationSubscriber;
import com.practicket.chat.component.ChatMessageSubscriber;
import com.practicket.chat.component.ChatParticipantSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

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
        container.addMessageListener(chatListenerAdapter, chatTopic);
        container.addMessageListener(chatParticipantListenerAdapter, chatParticipantTopic);
        container.addMessageListener(captchaStatListenerAdapter, captchaStatTopic);
        return container;
    }
}
