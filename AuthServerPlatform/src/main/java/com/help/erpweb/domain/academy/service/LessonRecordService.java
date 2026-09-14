package com.help.erpweb.domain.academy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.academy.authorization.AcademyAuthorization;
import com.help.erpweb.domain.academy.dto.request.LessonRecordCreateRequest;
import com.help.erpweb.domain.academy.dto.response.LessonRecordSummaryResponse;
import com.help.erpweb.domain.academy.entity.LessonRecord;
import com.help.erpweb.domain.academy.entity.AcademyClass;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.erpweb.domain.academy.repository.LessonRecordRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
            String teacherId,
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
        String targetTeacherId = teacherId;
        // ADMIN → 기존 조회 유지
        if (academyAuthorization.hasGlobalAccess(authentication)) {
            // targetTeacherId 그대로 사용
        }
        // ACADEMY_OWNER → 기존 조회 유지
        else if (academyAuthorization.isOwner(
                authentication,
                academyId
        )) {
            // targetTeacherId 그대로 사용
        }
        // 그 외
        else {
            String discordId = authentication.getName();
            // TEACHER는 요청 teacherId와 상관없이 자기 자신으로 강제
            targetTeacherId = academyMemberRepository
                    .findTeacherSk(
                            discordId,
                            academyId
                    )
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "수업 기록을 조회할 권한이 없습니다."
                            )
                    );
        }
        return lessonRecordRepository
                .findLessonRecordSummaries(
                        academyId,
                        className,
                        date,
                        normalize(time),
                        subjectName,
                        normalize(targetTeacherId)
                )
                .stream()
                .map(this::toResponse)
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
                request.educationDuration(),
                request.mainTeacherId(),
                objectMapper.valueToTree(
                        request.coTeacherIds()
                ),
                objectMapper.valueToTree(
                        request.memberIds()
                ),
                request.description()
        );
        lessonRecordRepository.save(lessonRecord);
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
            LessonRecordRepository.LessonRecordSummaryProjection projection
    ) {
        String teacherName =
                findTeacherName(
                        projection.getMainTeacherId()
                );

        return new LessonRecordSummaryResponse(
                projection.getId(),
                projection.getAcademyName(),
                projection.getClassName(),
                projection.getSubject(),
                projection.getEducationDate(),
                projection.getEducationDuration(),
                teacherName,
                projection.getDescription(),
                projection.getMemberCount() != null
                        ? projection.getMemberCount()
                        : 0
        );
    }

    private String findTeacherName(String teacherSk) {

        if (teacherSk == null || teacherSk.isBlank()) {
            return null;
        }

        try {
            String discordId =
                    membershipRepository
                            .findDiscordIdBySk(teacherSk);

            return discordUserRepository
                    .findByDiscordID(discordId)
                    .map(DiscordUser::getUsername)
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