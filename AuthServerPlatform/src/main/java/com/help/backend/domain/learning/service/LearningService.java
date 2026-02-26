package com.help.backend.domain.learning.service;

import com.help.backend.domain.metadata.response.ColumnMetaDto;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
import com.help.global.jwt.CustomUser;
import com.help.backend.domain.learning.dto.request.repository.LearningDto;
import com.help.backend.domain.learning.repository.LearningRepository;
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
public class LearningService {
    private final LearningRepository learningRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getLearningList(CustomUser user) {
        String discordId = user.getUsername();

        String sk = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();

        List<ColumnMetaDto> metadata = learningRepository.findColumnMetas(
                learningRepository.schemaName,  learningRepository.tableName)
                .stream()
                .map(v -> ColumnMetaDto.builder()
                        .colName(v.getColName())
                        .isPrimary(v.getIsPrimary())
                        .isNullable(v.getIsNullable())
                        .build())
                .toList();

        List<LearningDto> learningList = learningRepository.findByTeacher(sk)
                .stream()
                .map(entity -> LearningDto.builder()
                        .lnKey(entity.getLnKey())
                        .lnValue(entity.getLnValue())
//                        .teacher(entity.getTeacher())
                        .build())
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", learningList);
        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(CustomUser user, List<LearningDto> learningList) {
        // 애초에 저장할거 없으면 요청 자체가 안 오지만, 안전장치임
        if (learningList == null || learningList.isEmpty())
            return ApiResponse.success("No data to save");

        List<String> results = new ArrayList<>();
        for (LearningDto dto : learningList) {
            String result = learningRepository.callLearningProcedure(
                    MembershipID.discord.name(),
                    user.getUsername(),
                    dto.getLnKey(),
                    dto.getLnValue()
            );
            results.add(dto.getLnKey()+" 학습 결과 : "+result);
        }
        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(CustomUser user, List<LearningDto> deleteList) {
        // 애초에 삭제할게 없으면 요청 자체가 안 오지만, 안전장치임
        if (deleteList == null || deleteList.isEmpty())
            return ApiResponse.success("No data to delete");

        String discordId = user.getUsername();
        String teacher = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();

        // teacher가 모두 동일하다는 전제에서만 사용 가능
        List<String> keys = deleteList.stream()
                .map(LearningDto::getLnKey)
                .distinct()
                .toList();

        int totalCount = keys.toArray().length;
        int delCount = learningRepository.deleteByLnKey(teacher, keys);
        String result = "총 "+totalCount+"행 중 "+delCount+"행 삭제됨";
        return ApiResponse.success(result);
    }
}
