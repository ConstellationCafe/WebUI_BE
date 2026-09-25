package com.help.erpweb.domain.membership.exception;

public class ActiveMemberNotFoundException extends RuntimeException {
    public ActiveMemberNotFoundException() {
        super("재적 회원을 찾을 수 없습니다.");
    }
}
