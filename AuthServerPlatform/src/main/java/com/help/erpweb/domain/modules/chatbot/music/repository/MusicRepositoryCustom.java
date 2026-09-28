package com.help.erpweb.domain.modules.chatbot.music.repository;

import com.help.erpweb.domain.modules.chatbot.music.entity.MusicEntity;
import com.help.erpweb.domain.modules.chatbot.music.projection.MusicProjection;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface MusicRepositoryCustom {
    String callMusicProcedure(String cardType,
                             String membershipID,
                             String videoId);

    Page<MusicProjection> findPage(
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
