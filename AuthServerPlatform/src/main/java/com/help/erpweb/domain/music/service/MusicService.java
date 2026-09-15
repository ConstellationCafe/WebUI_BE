package com.help.erpweb.domain.music.service;

import com.help.erpweb.domain.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.content.entity.ContentEntity;
import com.help.erpweb.domain.content.repository.ContentRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.erpweb.domain.music.dto.request.repository.MusicDto;
import com.help.erpweb.domain.music.entity.MusicEntity;
import com.help.erpweb.domain.music.repository.MusicRepository;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
import com.help.global.jwt.CustomUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicService {
    private final MusicRepository musicRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getMusicList(
            String discordId,
            int page,
            int size,
            String searchColumn,
            String searchValue,
            String sortColumn,
            String sortDirection
    ) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        String sk = (String) entityManager
                .createNativeQuery("""
                SELECT Constellation_Network.search_sk(
                    :cardType,
                    :membershipId
                )
                """)
                .setParameter(
                        "cardType",
                        MembershipID.discord.name()
                )
                .setParameter(
                        "membershipId",
                        discordId
                )
                .getSingleResult();
        /*
         * metadata 조회
         */
        List<ColumnMetaDto> metadata =
                musicRepository.findColumnMetas(
                                MusicRepository.schemaName,
                                MusicRepository.tableName
                        )
                        .stream()
                        .map(v ->
                                ColumnMetaDto.builder()
                                        .colName(v.getColName())
                                        .isPrimary(v.getIsPrimary())
                                        .isNullable(v.getIsNullable())
                                        .build()
                        )
                        .toList();
        /*
         * metadata → 허용 컬럼
         */
        Set<String> allowedColumns =
                metadata.stream()
                        .map(ColumnMetaDto::getColName)
                        .collect(Collectors.toSet());

        Page<MusicEntity> musicPage =
                musicRepository.findPage(
                        sk,
                        // API page는 1-based
                        // Repository는 0-based
                        normalizedPage - 1,
                        normalizedSize,
                        searchColumn,
                        searchValue,
                        sortColumn,
                        sortDirection,
                        allowedColumns
                );
        List<MusicDto> musicList =
                musicPage
                        .getContent()
                        .stream()
                        .map(entity ->
                                MusicDto.builder()
                                        .videoId(entity.getVideoId())
                                        .recommender(entity.getRecommender())
                                        .build()
                        )
                        .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", musicList);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put("totalElements", musicPage.getTotalElements());
        body.put("totalPages", musicPage.getTotalPages());
        body.put("hasNext", musicPage.hasNext());
        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(CustomUser user, List<MusicDto> musicList) {
        if (musicList == null || musicList.isEmpty())
            return ApiResponse.success("No data to save");

        List<String> results = new ArrayList<>();
        for (MusicDto dto : musicList) {
            String result = musicRepository.callMusicProcedure(
                    MembershipID.discord.name(),
                    user.getUsername(),
                    dto.getVideoId()
            );
            results.add(dto.getVideoId()+" 추천 결과 : "+result);
        }
        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(CustomUser user, List<MusicDto> musicList) {
        if (musicList == null || musicList.isEmpty())
            return ApiResponse.success("No data to delete");

        String discordId = user.getUsername();
        String recommender = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();

        // teacher가 모두 동일하다는 전제에서만 사용 가능
        List<String> values = musicList.stream()
                .map(MusicDto::getVideoId)
                .distinct()
                .toList();

        int totalCount = values.toArray().length;
        int delCount = musicRepository.deleteByMnValue(recommender, values);
        String result = "총 "+totalCount+"행 중 "+delCount+"행 삭제됨";
        return ApiResponse.success(result);
    }
}
