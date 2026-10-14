package com.help.erpweb.domain.academy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.global.chat.ChatIdentities;
import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;
import com.help.erpweb.domain.academy.authorization.AcademyAuthorization;
import com.help.erpweb.domain.academy.dto.request.LessonRecordCreateRequest;
import com.help.erpweb.domain.academy.dto.request.LessonRecordUpdateRequest;
import com.help.erpweb.domain.academy.dto.response.LessonRecordSummaryResponse;
import com.help.erpweb.domain.academy.entity.LessonRecord;
import com.help.erpweb.domain.academy.entity.AcademyClass;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.erpweb.domain.academy.repository.LessonRecordRepository;
import com.help.erpweb.domain.modules.erp.point.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonRecordService {
    private final LessonRecordRepository lessonRecordRepository;
    private final AcademyClassRepository academyClassRepository;
    private final MembershipRepository membershipRepository;
    private final DiscordUserRepository discordUserRepository;
    private final AcademyMemberRepository academyMemberRepository;
    // ObjectMapper
    private final ObjectMapper objectMapper;
    // Authorization
    private final AcademyAuthorization academyAuthorization;

    public List<LessonRecordSummaryResponse> getLessonRecords(
            Authentication authentication,
            LocalDate date,
            String time,
            String subject,
            String teacherSk,
            Integer academyId,
            Integer classId
    ) {
        String className = null;
        if (classId != null) {
            AcademyClass academyClass = academyClassRepository
                            .findById(classId)
                            .orElse(null);
            if (academyClass != null) {
                className = String.valueOf(
                        academyClass.getClassNumber()
                );
            }
        }
        String subjectName = convertSubjectIdToName(subject);
        String targetTeacherSk = teacherSk;
        // ADMIN → 기존 조회 유지
        if (academyAuthorization.hasGlobalAccess(authentication)) {
            // targetTeacherSk 그대로 사용
        }
        // ACADEMY_OWNER → 기존 조회 유지
        else if (academyAuthorization.isOwner(
                authentication,
                academyId
        )) {
            // targetTeacherSk 그대로 사용
        }
        // 그 외
        else {
            // 2026-09-26: discordId(전역)가 아니라 sk(방 스코프 신원)로 조회한다.
            String sk = resolveSk(authentication);
            // TEACHER는 요청 teacherId와 상관없이 자기 자신으로 강제
            targetTeacherSk = academyMemberRepository
                    .findTeacherSk(
                            sk,
                            academyId
                    )
                    .orElse(null);

            // 교사에게 아직 수업 기록이 없으면 빈 목록으로 응답한다.
            if (targetTeacherSk == null) {
                return List.of();
            }
        }
        return lessonRecordRepository
                .findLessonRecordSummaries(
                        academyId,
                        className,
                        date,
                        normalize(time),
                        subjectName,
                        normalize(targetTeacherSk)
                )
                .stream()
                .map(projection -> toResponse(
                        projection,
                        canModifyLessonRecord(
                                authentication,
                                projection.getAcademyId(),
                                projection.getMainTeacherSk()
                        )
                ))
                .toList();
    }

    @Transactional
    public void createLessonRecord(
            LessonRecordCreateRequest request
    ) {
        Integer classNumber;
        try {
            classNumber = Integer.valueOf(
                    request.className()
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "올바르지 않은 분반입니다. className="
                            + request.className()
            );
        }

        AcademyClass academyClass =
                academyClassRepository
                        .findByAcademy_IdAndClassNumber(
                                request.academyId(),
                                classNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 분반입니다. academyId="
                                                + request.academyId()
                                                + ", className="
                                                + request.className()
                                )
                        );

        String subjectName = request.subject();

        LessonRecord lessonRecord = new LessonRecord(
                request.academyId(),
                String.valueOf(
                        academyClass.getClassNumber()
                ),
                subjectName,
                request.educationDate(),
                request.startTime(),
                request.endTime(),
                request.educationDuration(),
                request.mainTeacherSk(),
                objectMapper.valueToTree(
                        request.coTeacherSks()
                ),
                objectMapper.valueToTree(
                        request.memberSks()
                ),
                request.description()
        );
        lessonRecordRepository.save(lessonRecord);
    }

    @Transactional
    public void updateLessonRecord(
            Authentication authentication,
            Long id,
            LessonRecordUpdateRequest request
    ) {
        LessonRecord lessonRecord = getAuthorizedLessonRecord(authentication, id);
        lessonRecord.updateDetails(
                request.subject(),
                request.educationDate(),
                request.startTime(),
                request.endTime(),
                request.educationDuration(),
                request.description()
        );
    }

    @Transactional
    public void deleteLessonRecord(Authentication authentication, Long id) {
        lessonRecordRepository.delete(
                getAuthorizedLessonRecord(authentication, id)
        );
    }

    private LessonRecord getAuthorizedLessonRecord(
            Authentication authentication,
            Long id
    ) {
        LessonRecord lessonRecord = lessonRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lesson record not found"
                ));

        if (!canModifyLessonRecord(
                authentication,
                lessonRecord.getAcademyId(),
                lessonRecord.getMainTeacherSk()
        )) {
            throw new AccessDeniedException(
                    "You do not have permission to modify this lesson record"
            );
        }

        return lessonRecord;
    }

    private boolean canModifyLessonRecord(
            Authentication authentication,
            Integer academyId,
            String mainTeacherSk
    ) {
        if (academyAuthorization.hasGlobalAccess(authentication)
                || academyAuthorization.isOwner(authentication, academyId)) {
            return true;
        }

        if (authentication == null || authentication.getName() == null) {
            return false;
        }

        return academyMemberRepository.findTeacherSk(
                        resolveSk(authentication),
                        academyId
                )
                .filter(mainTeacherSk::equals)
                .isPresent();
    }

    private String resolveSk(final Authentication authentication) {
        final CustomUser user = (CustomUser) authentication.getPrincipal();
        return membershipRepository.findSkByChatUser(
                ChatIdentities.fromPrincipal(user)
        );
    }

    private String convertSubjectIdToName(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return null;
        }

        return switch (subjectId.trim()) {
            case "1" -> "로테이션";
            case "2" -> "언리미티드";
            case "3" -> "스타터";
            default -> subjectId.trim();
        };
    }

    private LessonRecordSummaryResponse toResponse(
            LessonRecordRepository.LessonRecordSummaryProjection projection,
            boolean canModify
    ) {
        String teacherDisplayName =
                findTeacherDisplayName(
                        projection.getMainTeacherSk()
                );

        return new LessonRecordSummaryResponse(
                projection.getId(),
                projection.getAcademyName(),
                projection.getClassName(),
                projection.getSubject(),
                projection.getEducationDate(),
                projection.getStartTime(),
                projection.getEndTime(),
                projection.getEducationDuration(),
                teacherDisplayName,
                projection.getDescription(),
                projection.getMemberCount() != null
                        ? projection.getMemberCount()
                        : 0,
                canModify
        );
    }

    private String findTeacherDisplayName(String teacherSk) {

        if (teacherSk == null || teacherSk.isBlank()) {
            return null;
        }

        try {
            String discordId =
                    membershipRepository
                            .findDiscordIdBySk(teacherSk);

            return discordUserRepository
                    .findByBotIdAndDiscordId(GuildContext.requireBotId(), discordId)
                    .map(DiscordUser::getDisplayName)
                    .orElse(null);

        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
