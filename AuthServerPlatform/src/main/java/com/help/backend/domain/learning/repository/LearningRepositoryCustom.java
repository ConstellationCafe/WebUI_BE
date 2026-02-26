package com.help.backend.domain.learning.repository;

public interface LearningRepositoryCustom {
    String callLearningProcedure(String cardType,
                                 String membershipID,
                                 String lnKey,
                                 String lnValue);
}
