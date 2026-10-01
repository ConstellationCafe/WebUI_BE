package com.help.erpweb.domain.modules.erp.point.exception;

public class PointBalanceLimitException extends RuntimeException {
    public PointBalanceLimitException() {
        super("포인트 잔액이 허용 범위를 벗어납니다.");
    }
}
