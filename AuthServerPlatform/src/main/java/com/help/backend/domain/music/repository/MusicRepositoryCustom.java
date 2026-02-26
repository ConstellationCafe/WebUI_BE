package com.help.backend.domain.music.repository;

public interface MusicRepositoryCustom {
    String callMusicProcedure(String cardType,
                             String membershipID,
                             String videoId);
}
