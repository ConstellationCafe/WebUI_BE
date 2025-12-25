package com.help.authserver.api;

public interface LoginAPI<T> {
    public T getUserInfo(String code);
}
