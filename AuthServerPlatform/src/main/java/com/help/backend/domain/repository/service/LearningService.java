package com.help.backend.domain.repository.service;

import com.help.backend.global.common.response.ApiResponse;
import com.help.backend.global.jwt.CustomUser;
import com.help.backend.domain.repository.dto.request.repository.LearningDto;
import com.help.backend.domain.repository.entity.LearningEntity;
import com.help.backend.domain.repository.repository.LearningRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {
    private final LearningRepository learningRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    public ApiResponse<?> getLearningList(CustomUser user) {
        String discordId = user.getUsername();
        // 프로시저 호출
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("Constellation_Network.search_sk")
                .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                .registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);

        query.setParameter(1, discordId);
        query.execute();

        String sk = (String) query.getOutputParameterValue(2);
        log.info("Discord ID '{}' resolved to SK '{}'", discordId, sk);

        // 엔티티 조회 후 DTO 변환
        List<LearningDto> learningList = learningRepository.findByTeacher(sk)
                                                    .stream()
                                                    .map(entity -> LearningDto.builder()
                                                            .lnKey(entity.getLnKey())
                                                            .lnValue(entity.getLnValue())
                                                            .teacher(entity.getTeacher())
                                                            .build())
                                                    .collect(Collectors.toList());
        return ApiResponse.success(learningList);
    }

    public ApiResponse<?> saveAll(List<LearningDto> learningList) {
        List<LearningEntity> entities = learningList.stream()
                .map(dto -> LearningEntity.builder()
                        .lnKey(dto.getLnKey())
                        .lnValue(dto.getLnValue())
                        .teacher(dto.getTeacher())
                        .build())
                .toList();

        learningRepository.saveAll(entities);

        log.info("{} records saved successfully", entities.size());
        return ApiResponse.success("Data saved successfully");
    }
}
