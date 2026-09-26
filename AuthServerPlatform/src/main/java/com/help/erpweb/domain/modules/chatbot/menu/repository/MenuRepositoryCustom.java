package com.help.erpweb.domain.modules.chatbot.menu.repository;

import com.help.erpweb.domain.modules.chatbot.menu.entity.MenuEntity;
import com.help.erpweb.domain.modules.chatbot.menu.projection.MenuProjection;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface MenuRepositoryCustom {
    String callMenuProcedure(String cardType,
                             String membershipID,
                             String mnValue);

    Page<MenuProjection> findPage(
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
