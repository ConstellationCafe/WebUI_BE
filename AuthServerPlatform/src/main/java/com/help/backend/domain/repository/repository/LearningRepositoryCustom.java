package com.help.backend.domain.repository.repository;

import java.util.Map;

public interface LearningRepositoryCustom {
    String callLearningProcedure(String cardType,
                                 String membershipID,
                                 String lnKey,
                                 String lnValue);
}
