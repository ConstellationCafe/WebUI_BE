package com.help.backend.domain.repository.service;

import com.help.backend.domain.global.dto.response.ColumnMetaDto;
import com.help.global.common.response.ApiResponse;
import com.help.global.data.MembershipID;
import com.help.global.jwt.CustomUser;
import com.help.backend.domain.repository.dto.request.repository.LearningDto;
import com.help.backend.domain.repository.repository.LearningRepository;
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
                        .teacher(entity.getTeacher())
                        .build())
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("metadata", metadata);
        body.put("entities", learningList);
        return ApiResponse.success(body);
    }

    @Transactional
    public ApiResponse<?> saveAll(CustomUser user, List<LearningDto> learningList) {
        if (learningList == null || learningList.isEmpty())
            return ApiResponse.success("No data to save");

        List<Map<String, String>> results = new ArrayList<>();
        for (LearningDto dto : learningList) {
            String msg = learningRepository.callLearningProcedure(
                    MembershipID.discord.name(),
                    user.getUsername(),
                    dto.getLnKey(),
                    dto.getLnValue()
            );
            Map<String, String> item = new HashMap<>();
            item.put(dto.getLnKey(), msg);
            results.add(item);
        }
        return ApiResponse.success(results);
    }

    @Transactional
    public ApiResponse<?> deleteAll(CustomUser user, List<LearningDto> learningList) {
        if (learningList == null || learningList.isEmpty())
            return ApiResponse.success("No data to delete");

        String discordId = user.getUsername();
        String teacher = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", MembershipID.discord.name())
                .setParameter("membershipId", discordId)
                .getSingleResult();

        // teacher가 모두 동일하다는 전제에서만 사용 가능
        List<String> keys = learningList.stream()
                .map(LearningDto::getLnKey)
                .distinct()
                .toList();

        int delCount = learningRepository.deleteByLnKey(teacher, keys);
        String msg = delCount + "행 삭제됨";
        return ApiResponse.success(msg);
    }
}
