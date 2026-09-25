package com.help.erpweb.domain.membership.exception;

public class PointLogConflictException extends RuntimeException {
    public PointLogConflictException() {
        super("포인트 내역이 변경되었습니다. 최신 내역을 다시 확인해 주세요.");
    }
}
