package com.help.erpweb.domain.learning.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
public class LearningRepositoryImpl implements LearningRepositoryCustom {
    @PersistenceContext
    private EntityManager em;

    @Override
    public String callLearningProcedure(String cardType,
                                        String membershipID,
                                        String lnKey,
                                        String lnValue) {

        StoredProcedureQuery sp = em.createStoredProcedureQuery("ChatBot.learning");

        sp.registerStoredProcedureParameter("card_type", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("membershipID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("ln_key", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("ln_value", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("recommend_result", String.class, ParameterMode.OUT);

        sp.setParameter("card_type", cardType);
        sp.setParameter("membershipID", membershipID);
        sp.setParameter("ln_key", lnKey);
        sp.setParameter("ln_value", lnValue);

        sp.execute();

        return (String) sp.getOutputParameterValue("recommend_result");
    }
}
