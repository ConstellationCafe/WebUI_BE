package com.help.erpweb.domain.membership.exception;

public class PointBalanceLimitException extends RuntimeException {
    public PointBalanceLimitException() {
        super("포인트 잔액이 허용 범위를 벗어납니다.");
    }
}
