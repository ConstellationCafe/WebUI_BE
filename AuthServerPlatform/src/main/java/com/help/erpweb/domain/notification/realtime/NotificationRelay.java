package com.help.erpweb.domain.notification.realtime;

import com.help.erpweb.domain.notification.service.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 커밋이 끝난 알림만 실시간으로 내보낸다. 발행 트랜잭션이 롤백되면(예: 포인트 지급 실패)
 * 회원에게 존재하지 않는 알림이 보이지 않는다. 트랜잭션 밖에서 발행된 이벤트도 전달한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRelay {
    private final NotificationBroadcaster broadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreated(NotificationCreatedEvent event) {
        try {
            broadcaster.broadcast(event.message());
        } catch (RuntimeException ex) {
            // 커밋은 이미 끝났으므로 호출자에게 실패를 전파하지 않는다. 회원은 재조회로 복구한다.
            log.warn("알림 실시간 전달 중 오류 - notificationId={}", event.message().notification().id(), ex);
        }
    }
}
