package com.help.erpweb.domain.modules.erp.point.exception;

public class ActiveMemberNotFoundException extends RuntimeException {
    public ActiveMemberNotFoundException() {
        super("재적 회원을 찾을 수 없습니다.");
    }
}
