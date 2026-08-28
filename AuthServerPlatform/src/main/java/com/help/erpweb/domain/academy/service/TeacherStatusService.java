package com.help.erpweb.domain.academy.service;

import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.academy.dto.response.AcademyOptionResponse;
import com.help.erpweb.domain.academy.dto.response.AcademyResponse;
import com.help.erpweb.domain.academy.dto.response.ClassOptionResponse;
import com.help.erpweb.domain.academy.dto.response.OptionResponse;
import com.help.erpweb.domain.academy.dto.response.StatusClassResponse;
import com.help.erpweb.domain.academy.dto.response.StatusItemResponse;
import com.help.erpweb.domain.academy.dto.response.StatusPaginationResponse;
import com.help.erpweb.domain.academy.dto.response.TeacherResponse;
import com.help.erpweb.domain.academy.dto.response.TeacherStatusListResponse;
import com.help.erpweb.domain.academy.dto.response.TeacherStatusResponse;
import com.help.erpweb.domain.academy.dto.response.TeacherStatusSummaryResponse;
import com.help.erpweb.domain.academy.entity.AcademyClass;
import com.help.erpweb.domain.academy.entity.Teacher;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyRepository;
import com.help.erpweb.domain.academy.repository.TeacherRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherStatusService {

    private static final String DB_STATUS_ENROLLED = "재적";
    private static final String DB_STATUS_RETIRED = "은퇴";
    private static final String DB_STATUS_DISCIPLINARY = "징계";

    private final AcademyRepository academyRepository;
    private final AcademyClassRepository academyClassRepository;
    private final TeacherRepository teacherRepository;
    private final MembershipRepository membershipRepository;
    private final DiscordUserRepository discordUserRepository;

    public TeacherStatusResponse getStatusOptions(
            Integer academyId,
            Integer classId
    ) {
        List<AcademyOptionResponse> academies =
                academyRepository.findAll()
                        .stream()
                        .map(academy ->
                                new AcademyOptionResponse(
                                        academy.getId(),
                                        academy.getName()
                                )
                        )
                        .toList();

        List<ClassOptionResponse> classes =
                academyId == null
                        ? List.of()
                        : academyClassRepository
                        .findByAcademy_Id(academyId)
                        .stream()
                        .map(academyClass ->
                                new ClassOptionResponse(
                                        academyClass.getId(),
                                        String.valueOf(
                                                academyClass.getClassNumber()
                                        ),
                                        academyClass.getState()
                                )
                        )
                        .toList();

        List<OptionResponse> teachers;

        if (academyId == null) {
            teachers = List.of();

        } else if (classId != null) {
            teachers =
                    teacherRepository
                            .findByAcademyClass_Id(
                                    classId
                            )
                            .stream()
                            .map(this::toOptionResponse)
                            .flatMap(Optional::stream)
                            .toList();

        } else {
            teachers =
                    teacherRepository
                            .findByAcademyClass_Academy_Id(
                                    academyId
                            )
                            .stream()
                            .map(this::toOptionResponse)
                            .flatMap(Optional::stream)
                            .toList();
        }

        return new TeacherStatusResponse(
                academies,
                classes,
                teachers
        );
    }

    public TeacherStatusListResponse getTeacherStatuses(
            Integer academyId,
            Integer classId,
            String academyMemberId,
            String status,
            int page,
            int size
    ) {
        int normalizedPage =
                Math.max(page, 1);

        int normalizedSize =
                Math.max(size, 1);

        String normalizedMemberId =
                normalize(academyMemberId);

        String normalizedStatus =
                toTeacherDbStatus(
                        normalize(status)
                );

        Pageable pageable =
                PageRequest.of(
                        normalizedPage - 1,
                        normalizedSize
                );

        Page<Teacher> teacherPage =
                teacherRepository.findStatusPage(
                        academyId,
                        classId,
                        normalizedMemberId,
                        normalizedStatus,
                        pageable
                );

        List<StatusItemResponse<TeacherResponse>> items =
                teacherPage
                        .getContent()
                        .stream()
                        .map(this::toStatusItemResponse)
                        .flatMap(Optional::stream)
                        .toList();

        TeacherStatusSummaryResponse summary =
                createSummary(
                        academyId,
                        classId,
                        normalizedMemberId
                );

        StatusPaginationResponse pagination =
                new StatusPaginationResponse(
                        normalizedPage,
                        normalizedSize,
                        teacherPage.getTotalPages(),
                        teacherPage.getTotalElements()
                );

        return new TeacherStatusListResponse(
                items,
                summary,
                pagination
        );
    }

    private TeacherStatusSummaryResponse createSummary(
            Integer academyId,
            Integer classId,
            String academyMemberId
    ) {
        long totalCount =
                teacherRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        null
                );

        long enrolledCount =
                teacherRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        DB_STATUS_ENROLLED
                );

        long retirementCount =
                teacherRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        DB_STATUS_RETIRED
                );

        long disciplinaryCount =
                teacherRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        DB_STATUS_DISCIPLINARY
                );

        return new TeacherStatusSummaryResponse(
                totalCount,
                enrolledCount,
                retirementCount,
                disciplinaryCount
        );
    }

    private Optional<StatusItemResponse<TeacherResponse>>
    toStatusItemResponse(
            Teacher teacher
    ) {
        Optional<TeacherResponse> member =
                toTeacherResponse(teacher);

        if (member.isEmpty()) {
            return Optional.empty();
        }

        AcademyClass academyClass =
                teacher.getAcademyClass();

        if (academyClass == null ||
                academyClass.getAcademy() == null) {
            return Optional.empty();
        }

        AcademyResponse academy =
                new AcademyResponse(
                        academyClass
                                .getAcademy()
                                .getId(),
                        academyClass
                                .getAcademy()
                                .getName()
                );

        StatusClassResponse classResponse =
                new StatusClassResponse(
                        academyClass.getId(),
                        String.valueOf(
                                academyClass.getClassNumber()
                        ),
                        academyClass.getState()
                );

        return Optional.of(
                new StatusItemResponse<>(
                        member.get(),
                        academy,
                        classResponse,
                        toTeacherApiStatus(
                                teacher.getState()
                        ),
                        teacher.getCreateAt(),
                        null
                )
        );
    }

    private Optional<OptionResponse> toOptionResponse(
            Teacher teacher
    ) {
        if (teacher.getSk() == null) {
            return Optional.empty();
        }

        String discordId =
                resolveDiscordId(
                        teacher.getSk()
                );

        String username =
                resolveUsername(
                        discordId
                );

        if (username == null) {
            return Optional.empty();
        }

        return Optional.of(
                new OptionResponse(
                        teacher.getSk(),
                        discordId,
                        username
                )
        );
    }

    private Optional<TeacherResponse> toTeacherResponse(
            Teacher teacher
    ) {
        if (teacher.getSk() == null) {
            return Optional.empty();
        }

        String discordId =
                resolveDiscordId(
                        teacher.getSk()
                );

        String username =
                resolveUsername(
                        discordId
                );

        if (username == null) {
            return Optional.empty();
        }

        AcademyClass academyClass =
                teacher.getAcademyClass();

        return Optional.of(
                new TeacherResponse(
                        teacher.getSk(),
                        discordId,
                        username,
                        academyClass != null
                                ? academyClass.getId()
                                : null,
                        academyClass != null
                                ? academyClass.getClassNumber()
                                : null
                )
        );
    }

    private String toTeacherDbStatus(
            String status
    ) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "ENROLLED" ->
                    DB_STATUS_ENROLLED;

            case "RETIRED" ->
                    DB_STATUS_RETIRED;

            case "DISCIPLINARY" ->
                    DB_STATUS_DISCIPLINARY;

            default ->
                    throw new IllegalArgumentException(
                            "지원하지 않는 교사 상태입니다: "
                                    + status
                    );
        };
    }

    private String toTeacherApiStatus(
            String state
    ) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "교사 상태가 null입니다."
            );
        }

        return switch (state) {
            case DB_STATUS_ENROLLED ->
                    "ENROLLED";

            case DB_STATUS_RETIRED ->
                    "RETIRED";

            case DB_STATUS_DISCIPLINARY ->
                    "DISCIPLINARY";

            default ->
                    throw new IllegalArgumentException(
                            "지원하지 않는 교사 DB 상태입니다: "
                                    + state
                    );
        };
    }

    private String resolveDiscordId(
            String sk
    ) {
        try {
            String discordId =
                    membershipRepository
                            .findDiscordIdBySk(sk);

            return discordId != null
                    ? discordId
                    : sk;

        } catch (Exception e) {
            return sk;
        }
    }

    private String resolveUsername(
            String discordId
    ) {
        return discordUserRepository
                .findByDiscordID(discordId)
                .map(DiscordUser::getNickname)
                .orElse(null);
    }

    private String normalize(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}