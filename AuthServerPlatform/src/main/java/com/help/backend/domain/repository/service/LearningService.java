package com.help.backend.domain.repository.service;

import com.help.global.common.response.ApiResponse;
import com.help.global.jwt.CustomUser;
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

        String sk = (String) entityManager
                .createNativeQuery("SELECT Constellation_Network.search_sk(:cardType, :membershipId)")
                .setParameter("cardType", "discord")
                .setParameter("membershipId", discordId)
                .getSingleResult();

        log.info("Discord ID '{}' resolved to SK '{}'", discordId, sk);

        List<LearningDto> learningList = learningRepository.findByTeacher(sk)
                .stream()
                .map(entity -> LearningDto.builder()
                        .lnKey(entity.getLnKey())
                        .lnValue(entity.getLnValue())
                        .teacher(entity.getTeacher())
                        .build())
                .toList();

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
