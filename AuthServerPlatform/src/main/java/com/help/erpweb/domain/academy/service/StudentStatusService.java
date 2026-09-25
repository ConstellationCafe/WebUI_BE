package com.help.erpweb.domain.academy.service;

import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.global.guild.GuildContext;
import com.help.erpweb.domain.academy.dto.response.AcademyOptionResponse;
import com.help.erpweb.domain.academy.dto.response.AcademyResponse;
import com.help.erpweb.domain.academy.dto.response.ClassOptionResponse;
import com.help.erpweb.domain.academy.dto.response.OptionResponse;
import com.help.erpweb.domain.academy.dto.response.StatusClassResponse;
import com.help.erpweb.domain.academy.dto.response.StatusItemResponse;
import com.help.erpweb.domain.academy.dto.response.StatusPaginationResponse;
import com.help.erpweb.domain.academy.dto.response.StudentResponse;
import com.help.erpweb.domain.academy.dto.response.StudentStatusListResponse;
import com.help.erpweb.domain.academy.dto.response.StudentStatusResponse;
import com.help.erpweb.domain.academy.dto.response.StudentStatusSummaryResponse;
import com.help.erpweb.domain.academy.dto.response.SubjectOptionResponse;
import com.help.erpweb.domain.academy.entity.AcademyClass;
import com.help.erpweb.domain.academy.entity.Student;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyRepository;
import com.help.erpweb.domain.academy.repository.StudentRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentStatusService {

    private static final String DB_STATUS_ENROLLED = "재적";
    private static final String DB_STATUS_GRADUATED = "졸업";
    private static final String DB_STATUS_EXPELLED = "퇴학";
    private static final String DB_STATUS_WITHDRAWN = "자퇴";

    private final AcademyRepository academyRepository;
    private final AcademyClassRepository academyClassRepository;
    private final StudentRepository studentRepository;
    private final MembershipRepository membershipRepository;
    private final DiscordUserRepository discordUserRepository;

    public StudentStatusResponse getStatusOptions(
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

        List<OptionResponse> students =
                academyId == null || classId == null
                        ? List.of()
                        : studentRepository
                        .findByAcademyIdAndClassId(
                                academyId,
                                classId
                        )
                        .stream()
                        .map(this::toOptionResponse)
                        .flatMap(Optional::stream)
                        .toList();

        List<SubjectOptionResponse> subjects =
                List.of(
                        new SubjectOptionResponse(
                                1,
                                "로테이션"
                        ),
                        new SubjectOptionResponse(
                                2,
                                "언리미티드"
                        ),
                        new SubjectOptionResponse(
                                3,
                                "스타터"
                        )
                );

        return new StudentStatusResponse(
                academies,
                classes,
                students,
                subjects
        );
    }

    public StudentStatusListResponse getStudentStatuses(
            Integer academyId,
            Integer classId,
            String academyMemberId,
            String status,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        String normalizedMemberId = normalize(academyMemberId);
        String normalizedStatus = toStudentDbStatus(normalize(status));

        Pageable pageable = PageRequest.of(
                        normalizedPage - 1,
                        normalizedSize
                );
        Page<Student> studentPage = studentRepository.findStatusPage(
                        academyId,
                        classId,
                        normalizedMemberId,
                        normalizedStatus,
                        pageable
                );

        List<StatusItemResponse<StudentResponse>> items =
                toStatusItemResponses(studentPage.getContent());

        StudentStatusSummaryResponse summary =
                createSummary(
                        academyId,
                        classId,
                        normalizedMemberId
                );

        StatusPaginationResponse pagination =
                new StatusPaginationResponse(
                        normalizedPage,
                        normalizedSize,
                        studentPage.getTotalPages(),
                        studentPage.getTotalElements()
                );

        return new StudentStatusListResponse(
                items,
                summary,
                pagination
        );
    }

    private StudentStatusSummaryResponse createSummary(
            Integer academyId,
            Integer classId,
            String academyMemberId
    ) {
        long totalCount =
                studentRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        null
                );

        long enrolledCount =
                studentRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        DB_STATUS_ENROLLED
                );

        long graduationCount =
                studentRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        DB_STATUS_GRADUATED
                );

        long expulsionCount =
                studentRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        DB_STATUS_EXPELLED
                );

        long withdrawalCount =
                studentRepository.countByStatusCondition(
                        academyId,
                        classId,
                        academyMemberId,
                        DB_STATUS_WITHDRAWN
                );

        return new StudentStatusSummaryResponse(
                totalCount,
                enrolledCount,
                graduationCount,
                expulsionCount,
                withdrawalCount
        );
    }

    private Optional<StatusItemResponse<StudentResponse>>
    toStatusItemResponse(
            Student student
    ) {
        Optional<StudentResponse> member =
                toStudentResponse(student);

        if (member.isEmpty()) {
            return Optional.empty();
        }

        AcademyClass academyClass =
                student.getAcademyClass();

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
                        toStudentApiStatus(
                                student.getState()
                        ),
                        student.getCreateAt(),
                        null
                )
        );
    }

    private Optional<OptionResponse> toOptionResponse(
            Student student
    ) {
        if (student.getSk() == null) {
            return Optional.empty();
        }

        String discordId =
                resolveDiscordId(
                        student.getSk()
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
                        student.getSk(),
                        discordId,
                        username
                )
        );
    }

    private Optional<StudentResponse> toStudentResponse(
            Student student
    ) {
        if (student.getSk() == null) {
            return Optional.empty();
        }

        String discordId =
                resolveDiscordId(
                        student.getSk()
                );

        String username =
                resolveUsername(
                        discordId
                );

        if (username == null) {
            return Optional.empty();
        }

        AcademyClass academyClass =
                student.getAcademyClass();

        return Optional.of(
                new StudentResponse(
                        student.getSk(),
                        discordId,
                        username,
                        academyClass != null &&
                                academyClass.getAcademy() != null
                                ? academyClass
                                .getAcademy()
                                .getId()
                                : null,
                        academyClass != null
                                ? academyClass.getId()
                                : null,
                        academyClass != null
                                ? academyClass.getClassNumber()
                                : null,
                        student.getState()
                )
        );
    }

    private List<StatusItemResponse<StudentResponse>> toStatusItemResponses(
            List<Student> students
    ) {
        List<String> sks = students.stream()
                .map(Student::getSk)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> discordIds = membershipRepository.findDiscordIdsBySk(sks);
        List<String> ids = discordIds.values().stream().distinct().toList();
        Map<String, String> usernames = discordUserRepository.findActiveByBotIdAndDiscordIDIn(GuildContext.requireBotId(), ids)
                .stream()
                .collect(Collectors.toMap(DiscordUser::getDiscordID, DiscordUser::getNickname,
                        (first, ignored) -> first));

        return students.stream()
                .map(student -> {
                    String discordId = discordIds.get(student.getSk());
                    String username = usernames.get(discordId);
                    AcademyClass academyClass = student.getAcademyClass();
                    if (student.getSk() == null || discordId == null || username == null
                            || academyClass == null || academyClass.getAcademy() == null) {
                        return null;
                    }
                    StudentResponse response = new StudentResponse(
                            student.getSk(), discordId, username,
                            academyClass.getAcademy().getId(), academyClass.getId(),
                            academyClass.getClassNumber(), student.getState());
                    AcademyResponse academy = new AcademyResponse(
                            academyClass.getAcademy().getId(), academyClass.getAcademy().getName());
                    StatusClassResponse classResponse = new StatusClassResponse(
                            academyClass.getId(), String.valueOf(academyClass.getClassNumber()),
                            academyClass.getState());
                    return new StatusItemResponse<>(response, academy, classResponse,
                            toStudentApiStatus(student.getState()), student.getCreateAt(), null);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String toStudentDbStatus(
            String status
    ) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "ENROLLED" ->
                    DB_STATUS_ENROLLED;

            case "GRADUATED" ->
                    DB_STATUS_GRADUATED;

            case "EXPELLED" ->
                    DB_STATUS_EXPELLED;

            case "WITHDRAWN" ->
                    DB_STATUS_WITHDRAWN;

            default ->
                    throw new IllegalArgumentException(
                            "지원하지 않는 학생 상태입니다: "
                                    + status
                    );
        };
    }

    private String toStudentApiStatus(
            String state
    ) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "학생 상태가 null입니다."
            );
        }

        return switch (state) {
            case DB_STATUS_ENROLLED ->
                    "ENROLLED";

            case DB_STATUS_GRADUATED ->
                    "GRADUATED";

            case DB_STATUS_EXPELLED ->
                    "EXPELLED";

            case DB_STATUS_WITHDRAWN ->
                    "WITHDRAWN";

            default ->
                    throw new IllegalArgumentException(
                            "지원하지 않는 학생 DB 상태입니다: "
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
                .findAllByBotIdAndDiscordID(GuildContext.requireBotId(), discordId)
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
