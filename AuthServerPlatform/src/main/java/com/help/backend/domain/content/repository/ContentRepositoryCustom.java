package com.help.backend.domain.content.repository;

public interface ContentRepositoryCustom {
    String callContentProcedure(String cardType,
                                 String membershipID,
                                 String cnValue);
}
