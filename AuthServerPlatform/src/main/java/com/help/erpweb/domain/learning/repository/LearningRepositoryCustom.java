package com.help.erpweb.domain.learning.repository;

import com.help.erpweb.domain.content.entity.ContentEntity;
import com.help.erpweb.domain.learning.entity.LearningEntity;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface LearningRepositoryCustom {
    String callLearningProcedure(String cardType,
                                 String membershipID,
                                 String lnKey,
                                 String lnValue);

    Page<LearningEntity> findPage(
            String recommender,
            int page,
            int size,
            String searchColumn,
            String searchValue,
            String sortColumn,
            String sortDirection,
            Set<String> allowedColumns
    );
}
