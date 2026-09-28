package com.help.erpweb.domain.notification.service;

import com.help.erpweb.domain.notification.dto.response.AdminNotificationResponse;

/**
 * @param created 이번 요청으로 새로 저장했으면 true, 같은 요청 ID의 재전송이면 false
 */
public record NotificationPublishResult(AdminNotificationResponse notification, boolean created) {
}
