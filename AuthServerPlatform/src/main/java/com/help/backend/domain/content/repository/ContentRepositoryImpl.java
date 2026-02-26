package com.help.backend.domain.content.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ContentRepositoryImpl implements ContentRepositoryCustom {
    @PersistenceContext
    private EntityManager em;

    @Override
    public String callContentProcedure(String cardType,
                                       String membershipID,
                                       String cnValue) {

        StoredProcedureQuery sp = em.createStoredProcedureQuery("ChatBot.recommend_content");

        sp.registerStoredProcedureParameter("card_type", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("membershipID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("cn_value", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("recommend_result", String.class, ParameterMode.OUT);

        sp.setParameter("card_type", cardType);
        sp.setParameter("membershipID", membershipID);
        sp.setParameter("cn_value", cnValue);

        sp.execute();

        return (String) sp.getOutputParameterValue("recommend_result");
    }
}
