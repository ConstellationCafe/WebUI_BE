package com.help.authserver.domain.user.entity;

import java.util.UUID;

public interface User {
    public UUID getUserId();
    public String getUsername();
    public UserRole getRole();
    public String getPassword();
    public UserProfile getUserProfile();
}
