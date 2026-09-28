package com.help.erpweb.domain.modules.chatbot.content.repository;

import com.help.erpweb.domain.modules.chatbot.content.entity.ContentEntity;
import com.help.erpweb.domain.modules.chatbot.content.projection.ContentProjection;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface ContentRepositoryCustom {
    String callContentProcedure(String cardType,
                                 String membershipID,
                                 String cnValue);

    Page<ContentProjection> findPage(
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
