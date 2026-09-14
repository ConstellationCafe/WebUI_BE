package com.help.erpweb.domain.content.service;

import com.help.erpweb.domain.content.dto.request.repository.ContentDto;
import com.help.erpweb.domain.content.repository.ContentRepository;
import com.help.erpweb.domain.metadata.response.ColumnMetaDto;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
import com.help.global.jwt.CustomUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private final ContentRepository contentRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getContentList(CustomUser user) {
        String discordId = user.getUsername();

        String sk = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();

        List<ColumnMetaDto> metadata = contentRepository.findColumnMetas(
                contentRepository.schemaName,  contentRepository.tableName)
                .stream()
                .map(v -> ColumnMetaDto.builder()
                        .colName(v.getColName())
                        .isPrimary(v.getIsPrimary())
                        .isNullable(v.getIsNullable())
                        .build())
                .toList();

        List<ContentDto> MusicList = contentRepository.findByRecommender(sk)
                .stream()
                .map(entity -> ContentDto.builder()
                        .cnValue(entity.getCnValue())
//                        .recommender(entity.getRecommender())
                        .build())
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", MusicList);
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
