package com.help.erpweb.domain.academy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.academy.authorization.AcademyAuthorization;
import com.help.erpweb.domain.academy.dto.request.LessonRecordCreateRequest;
import com.help.erpweb.domain.academy.dto.request.LessonRecordUpdateRequest;
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
        // ADMIN â ê¸°ì¡´ ì¡°í ì ì§
        if (academyAuthorization.hasGlobalAccess(authentication)) {
            // targetTeacherId ê·¸ëë¡ ì¬ì©
        }
        // ACADEMY_OWNER â ê¸°ì¡´ ì¡°í ì ì§
        else if (academyAuthorization.isOwner(
                authentication,
                academyId
        )) {
            // targetTeacherId ê·¸ëë¡ ì¬ì©
        }
        // ê·¸ ì¸
        else {
            String discordId = authentication.getName();
            // TEACHERë ìì²­ teacherIdì ìê´ìì´ ìê¸° ìì ì¼ë¡ ê°•ì 
            targetTeacherId = academyMemberRepository
                    .findTeacherSk(
                            discordId,
                            academyId
                    )
                    .orElse(null);

            // 교사에게 아직 수업 기록이 없으면 빈 목록으로 응답한다.
            if (targetTeacherId == null) {
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
                        normalize(targetTeacherId)
                )
                .stream()
                .map(projection -> toResponse(
                        projection,
                        canModifyLessonRecord(
                                authentication,
                                projection.getAcademyId(),
                                projection.getMainTeacherId()
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
                    "ì¬ë°ë¥´ì§ ìì ë¶ë°ìëë¤. className="
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
                                        "ì¡´ì¬íì§ ìë ë¶ë°ìëë¤. academyId="
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
                lessonRecord.getMainTeacherId()
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
            String mainTeacherId
    ) {
        if (academyAuthorization.hasGlobalAccess(authentication)
                || academyAuthorization.isOwner(authentication, academyId)) {
            return true;
        }

        if (authentication == null || authentication.getName() == null) {
            return false;
        }

        return academyMemberRepository.findTeacherSk(
                        authentication.getName(),
                        academyId
                )
                .filter(mainTeacherId::equals)
                .isPresent();
    }

    private String convertSubjectIdToName(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return null;
        }

        return switch (subjectId.trim()) {
            case "1" -> "ë¡íì´ì";
            case "2" -> "ì¸ë¦¬ë¯¸í°ë";
            case "3" -> "ì¤íí°";
            default -> subjectId.trim();
        };
    }

    private LessonRecordSummaryResponse toResponse(
            LessonRecordRepository.LessonRecordSummaryProjection projection,
            boolean canModify
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
                projection.getStartTime(),
                projection.getEndTime(),
                projection.getEducationDuration(),
                teacherName,
                projection.getDescription(),
                projection.getMemberCount() != null
                        ? projection.getMemberCount()
                        : 0,
                canModify
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
                    .map(DiscordUser::getNickname)
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
