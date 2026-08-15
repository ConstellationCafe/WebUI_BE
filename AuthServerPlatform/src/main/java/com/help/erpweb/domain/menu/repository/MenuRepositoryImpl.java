package com.help.erpweb.domain.menu.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
public class MenuRepositoryImpl implements MenuRepositoryCustom {
    @PersistenceContext
    private EntityManager em;

    @Override
    public String callMenuProcedure(String cardType,
                                    String membershipID,
                                    String mnValue) {

        StoredProcedureQuery sp = em.createStoredProcedureQuery("ChatBot.recommend_menu");

        sp.registerStoredProcedureParameter("card_type", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("membershipID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("mn_value", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("recommend_result", String.class, ParameterMode.OUT);

        sp.setParameter("card_type", cardType);
        sp.setParameter("membershipID", membershipID);
        sp.setParameter("mn_value", mnValue);

        sp.execute();

        return (String) sp.getOutputParameterValue("recommend_result");
    }
}
