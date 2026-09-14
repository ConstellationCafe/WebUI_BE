package com.help.erpweb.domain.music.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
public class MusicRepositoryImpl implements MusicRepositoryCustom {
    @PersistenceContext
    private EntityManager em;

    @Override
    public String callMusicProcedure(String cardType,
                                    String membershipID,
                                    String videoId) {

        StoredProcedureQuery sp = em.createStoredProcedureQuery("ChatBot.recommend_music");

        sp.registerStoredProcedureParameter("card_type", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("membershipID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("videoId", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("recommend_result", String.class, ParameterMode.OUT);

        sp.setParameter("card_type", cardType);
        sp.setParameter("membershipID", membershipID);
        sp.setParameter("video_id", videoId);

        sp.execute();

        return (String) sp.getOutputParameterValue("recommend_result");
    }
}
