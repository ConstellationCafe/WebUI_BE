package com.help.erpweb.domain.notification.realtime;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableConfigurationProperties(NotificationRealtimeProperties.class)
public class NotificationRealtimeConfig {

    /**
     * Redis가 잠시 끊기면 구독을 지수 backoff(1초에서 시작, 최대 30초 간격)로 다시 시도한다.
     * 재구독 전까지 다른 인스턴스에서 발행한 실시간 알림은 이 인스턴스에 전달되지 않는다.
     */
    @Bean
    public RedisMessageListenerContainer notificationListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisNotificationBroadcaster broadcaster,
            NotificationRealtimeProperties properties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        ExponentialBackOff backOff = new ExponentialBackOff(Duration.ofSeconds(1).toMillis(), 2.0);
        backOff.setMaxInterval(Duration.ofSeconds(30).toMillis());
        container.setRecoveryBackoff(backOff);
        container.addMessageListener(broadcaster, new ChannelTopic(properties.channel()));
        return container;
    }
}
