package com.help.authserver.domain.user.view;

import java.time.LocalDateTime;

public interface DiscordMembershipView {
    String getDiscordID();
    String getUsername();
    String getState();
    LocalDateTime getJoin_at();
    LocalDateTime getLeft_at();
    String getUid1();
    String getUid2();
    String getRole();
    String getS1_title();
    LocalDateTime getS1_get();
    String getS2_title();
    LocalDateTime getS2_get();
    String getCoin();
    String getGuild();
}