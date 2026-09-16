package com.help.erpweb.domain.content.service;

import com.help.erpweb.domain.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.content.entity.ContentEntity;
import com.help.erpweb.domain.content.repository.ContentRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.global.authorization.Authorization;
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
public class ContentService {
    private final ContentRepository contentRepository;
    private final Authorization authorization;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getContentList(
            CustomUser user,
            int page,
            int size,
            String searchColumn,
            String searchValue,
            String sortColumn,
            String sortDirection
    ) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        String recommender = null;
        // 일반 사용자만 자신의 recommender를 구한다.
        // 관리자는 null → Repository에서 전체 조회
        if (!authorization.isAdmin(user)) {
            recommender = (String) entityManager
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
                            user.getUsername()
                    )
                    .getSingleResult();
        }
        /*
         * metadata 조회
         */
        List<ColumnMetaDto> metadata =
                contentRepository.findColumnMetas(
                                ContentRepository.schemaName,
                                ContentRepository.tableName
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

        Page<ContentEntity> contentPage =
                contentRepository.findPage(
                        recommender,
                        normalizedPage - 1,
                        normalizedSize,
                        searchColumn,
                        searchValue,
                        sortColumn,
                        sortDirection,
                        allowedColumns
                );
        List<ContentDto> contentList =
                contentPage
                        .getContent()
                        .stream()
                        .map(entity ->
                                ContentDto.builder()
                                        .cnValue(entity.getCnValue())
                                        .recommender(entity.getRecommender())
                                        .build()
                        )
                        .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", contentList);
        body.put("page", normalizedPage);
        body.put("size", normalizedSize);
        body.put("totalElements", contentPage.getTotalElements());
        body.put("totalPages", contentPage.getTotalPages());
        body.put("hasNext", contentPage.hasNext());
        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(CustomUser user, List<ContentDto> musicList) {
        if (musicList == null || musicList.isEmpty())
            return ApiResponse.success("No data to save");

        List<String> results = new ArrayList<>();
        for (ContentDto dto : musicList) {
            String result = contentRepository.callContentProcedure(
                    MembershipID.discord.name(),
                    user.getUsername(),
                    dto.getCnValue()
            );
            results.add(dto.getCnValue()+" 추천 결과 : "+result);
        }
        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(CustomUser user, List<ContentDto> musicList) {
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
                .map(ContentDto::getCnValue)
                .distinct()
                .toList();

        int totalCount = values.toArray().length;
        int delCount = contentRepository.deleteByMnValue(recommender, values);
        String result = "총 "+totalCount+"행 중 "+delCount+"행 삭제됨";
        return ApiResponse.success(result);
    }
}
