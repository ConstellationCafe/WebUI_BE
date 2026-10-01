package com.help.global.data;

public class Authority {
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String USER = "ROLE_USER";
    /** 서비스 API Key로 인증된 외부 시스템. 사용자 권한과 섞이지 않도록 별도 역할로 둔다. */
    public static final String INTEGRATION = "ROLE_INTEGRATION";
}
