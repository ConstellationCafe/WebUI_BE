package com.help.erpweb.domain.modules.erp.point.exception;

public class InsufficientCoinException extends RuntimeException {
    public InsufficientCoinException() {
        super("보유 포인트보다 많이 출금할 수 없습니다.");
    }
}
