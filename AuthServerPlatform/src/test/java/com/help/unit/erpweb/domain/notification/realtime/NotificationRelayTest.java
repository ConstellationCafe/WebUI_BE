package com.help.erpweb.domain.notification.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.help.erpweb.domain.notification.dto.response.NotificationResponse;
import com.help.erpweb.domain.notification.entity.NotificationCategory;
import com.help.erpweb.domain.notification.entity.NotificationTargetType;
import com.help.erpweb.domain.notification.service.NotificationCreatedEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationRelayTest {

    private static NotificationCreatedEvent event() {
        return new NotificationCreatedEvent(new NotificationMessage(
                "bot",
                NotificationTargetType.GUILD,
                null,
                new NotificationResponse(
                        1L,
                        NotificationCategory.ANNOUNCEMENT,
                        "제목",
                        "본문",
                        null,
                        NotificationTargetType.GUILD,
                        Instant.parse("2026-09-28T00:00:00Z"),
                        false
                )
        ));
    }

    @Test
    void forwardsCommittedNotificationToBroadcaster() {
        NotificationBroadcaster broadcaster = mock(NotificationBroadcaster.class);

        new NotificationRelay(broadcaster).onCreated(event());

        verify(broadcaster).broadcast(event().message());
    }

    @Test
    void broadcastFailureDoesNotPropagateAfterCommit() {
        NotificationBroadcaster broadcaster = mock(NotificationBroadcaster.class);
        doThrow(new IllegalStateException("boom")).when(broadcaster).broadcast(any());

        new NotificationRelay(broadcaster).onCreated(event());

        verify(broadcaster).broadcast(any());
    }
}
