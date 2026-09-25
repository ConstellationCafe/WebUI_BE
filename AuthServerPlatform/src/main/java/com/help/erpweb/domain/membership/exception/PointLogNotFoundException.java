package com.help.erpweb.domain.membership.exception;

public class PointLogNotFoundException extends RuntimeException {
    public PointLogNotFoundException() {
        super("포인트 내역을 찾을 수 없습니다. 최신 내역을 다시 확인해 주세요.");
    }
}
