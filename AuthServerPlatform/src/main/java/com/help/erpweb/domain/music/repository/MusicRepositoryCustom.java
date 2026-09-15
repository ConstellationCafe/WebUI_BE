package com.help.erpweb.domain.music.repository;

import com.help.erpweb.domain.music.entity.MusicEntity;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface MusicRepositoryCustom {
    String callMusicProcedure(String cardType,
                             String membershipID,
                             String videoId);

    Page<MusicEntity> findPage(
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
