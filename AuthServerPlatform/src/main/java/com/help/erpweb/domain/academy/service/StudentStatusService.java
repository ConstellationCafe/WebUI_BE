package com.help.erpweb.domain.academy.service;

import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.academy.dto.request.StudentStatusQueryRequest;
import com.help.erpweb.domain.academy.dto.response.AcademyResponse;
import com.help.erpweb.domain.academy.dto.response.ClassResponse;
import com.help.erpweb.domain.academy.dto.response.StudentInfoResponse;
import com.help.erpweb.domain.academy.dto.response.StudentStatusItemResponse;
import com.help.erpweb.domain.academy.dto.response.StudentStatusListResponse;
import com.help.erpweb.domain.academy.dto.response.StudentStatusPaginationResponse;
import com.help.erpweb.domain.academy.dto.response.StudentStatusSummaryResponse;
import com.help.erpweb.domain.academy.repository.StudentRepository;
import com.help.erpweb.domain.academy.type.StudentRosterStatus;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentStatusService {

    private final StudentRepository studentRepository;

    private final MembershipRepository membershipRepository;

    private final DiscordUserRepository discordUserRepository;

    public StudentStatusListResponse getStudentStatuses(
            StudentStatusQueryRequest request
    ) {
        int page = request.normalizedPage();
        int size = request.normalizedSize();

        String status = request.status() == null
                ? null
                : request.status().name();

        Page<StudentRepository.StudentStatusProjection>
                statusPage =
                studentRepository.findStudentStatuses(
                        request.academyId(),
                        request.classId(),
                        request.studentId(),
                        status,
                        PageRequest.of(
                                page - 1,
                                size
                        )
                );

        List<StudentStatusItemResponse> items =
                statusPage
                        .getContent()
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        StudentRepository.StudentStatusSummaryProjection
                summaryProjection =
                studentRepository.findStudentStatusSummary(
                        request.academyId(),
                        request.classId(),
                        request.studentId()
                );

        StudentStatusSummaryResponse summary =
                toSummaryResponse(
                        summaryProjection
                );

        StudentStatusPaginationResponse pagination =
                new StudentStatusPaginationResponse(
                        page,
                        size,
                        statusPage.getTotalPages(),
                        statusPage.getTotalElements()
                );

        return new StudentStatusListResponse(
                items,
                summary,
                pagination
        );
    }

    private StudentStatusItemResponse toItemResponse(
            StudentRepository.StudentStatusProjection projection
    ) {
        StudentRosterStatus status =
                StudentRosterStatus.valueOf(
                        projection.getStatus()
                );

        String discordId =
                findDiscordId(
                        projection.getStudentSk()
                );

        String studentName =
                findStudentName(
                        discordId
                );

        StudentInfoResponse student =
                new StudentInfoResponse(
                        projection.getStudentSk(),
                        discordId,
                        studentName,
                        null
                );

        AcademyResponse academy =
                new AcademyResponse(
                        projection.getAcademyId(),
                        projection.getAcademyName()
                );

        ClassResponse academyClass =
                new ClassResponse(
                        projection.getClassId(),
                        projection.getClassNumber(),
                        projection.getClassState()
                );

        LocalDate statusChangedAt =
                getStatusChangedAt(
                        projection,
                        status
                );

        String reason =
                getReason(
                        projection,
                        status
                );

        return new StudentStatusItemResponse(
                student,
                academy,
                academyClass,
                status,
                statusChangedAt,
                reason
        );
    }

    private StudentStatusSummaryResponse toSummaryResponse(
            StudentRepository.StudentStatusSummaryProjection projection
    ) {
        if (projection == null) {
            return new StudentStatusSummaryResponse(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        return new StudentStatusSummaryResponse(
                valueOrZero(
                        projection.getTotalCount()
                ),
                valueOrZero(
                        projection.getEnrolledCount()
                ),
                valueOrZero(
                        projection.getGraduationCount()
                ),
                valueOrZero(
                        projection.getExpulsionCount()
                ),
                valueOrZero(
                        projection.getWithdrawalCount()
                ),
                valueOrZero(
                        projection.getRetirementCount()
                ),
                valueOrZero(
                        projection.getDisciplinaryCount()
                )
        );
    }

    private LocalDate getStatusChangedAt(
            StudentRepository.StudentStatusProjection projection,
            StudentRosterStatus status
    ) {
        return switch (status) {
            case GRADUATED ->
                    projection.getGraduateAt();

            case EXPELLED ->
                    projection.getSuspendedAt();

            case ENROLLED,
                 WITHDRAWN,
                 RETIRED,
                 DISCIPLINARY ->
                    null;
        };
    }

    private String getReason(
            StudentRepository.StudentStatusProjection projection,
            StudentRosterStatus status
    ) {
        if (status != StudentRosterStatus.EXPELLED) {
            return null;
        }

        return projection.getSuspendReason();
    }

    private String findDiscordId(
            String sk
    ) {
        try {
            return membershipRepository
                    .findDiscordIdBySk(sk);
        } catch (Exception e) {
            return sk;
        }
    }

    private String findStudentName(
            String discordId
    ) {
        return discordUserRepository
                .findByDiscordID(discordId)
                .map(DiscordUser::getNickname)
                .filter(name ->
                        !name.isBlank()
                )
                .orElse(discordId);
    }

    private long valueOrZero(
            Long value
    ) {
        return value == null
                ? 0L
                : value;
    }
}