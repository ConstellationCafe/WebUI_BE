package com.help.erpweb.domain.content.repository;

import com.help.erpweb.domain.content.entity.ContentEntity;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface ContentRepositoryCustom {
    String callContentProcedure(String cardType,
                                 String membershipID,
                                 String cnValue);

    Page<ContentEntity> findPage(
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
