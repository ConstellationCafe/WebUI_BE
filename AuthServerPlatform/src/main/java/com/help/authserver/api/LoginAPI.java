package com.help.authserver.api;

public interface LoginAPI<T> {
    public String exchangeCodeForToken(String code);
    public T getUserInfo(String accessToken);
}
