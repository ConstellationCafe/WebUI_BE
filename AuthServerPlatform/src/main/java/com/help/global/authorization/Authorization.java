package com.help.global.authorization;

import com.help.global.data.Authority;
import com.help.global.jwt.CustomUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("authorization")
public class Authorization {
    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        Authority.ADMIN.equals(
                                authority.getAuthority()
                        )
                );
    }

    public boolean isAdmin(CustomUser user) {
        return user != null
                && user.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        Authority.ADMIN.equals(
                                authority.getAuthority()
                        )
                );
    }

    public boolean isUser(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        Authority.USER.equals(
                                authority.getAuthority()
                        )
                );
    }
}