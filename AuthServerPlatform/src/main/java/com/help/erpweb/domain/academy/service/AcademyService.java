package com.help.erpweb.domain.academy.service;

import com.fasterxml.jackson.databind.JsonNode;
// FIXME : AuthServer 패키지에서 ERPWeb 패키지로 이동 필요
import com.help.authserver.domain.user.entity.config.ErpSubscriber;
import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.config.ERPSubscriberRepository;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.academy.dto.response.*;
import com.help.erpweb.domain.academy.entity.AcademyClass;
import com.help.erpweb.domain.academy.entity.Student;
import com.help.erpweb.domain.academy.entity.Teacher;
import com.help.erpweb.domain.academy.repository.*;
import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyService {
    private static final String MODULE_ID =  "network_operations";
    private final AcademyRepository academyRepository;
    private final AcademyClassRepository academyClassRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ModuleConfigRepository moduleConfigRepository;
    private final ERPSubscriberRepository erpSubscriberRepository;
    private final MembershipRepository membershipRepository;
    private final DiscordUserRepository discordUserRepository;

    public List<AcademyResponse> getAcademies() {
        return academyRepository.findAll()
                .stream()
                .map(academy ->
                        new AcademyResponse(
                                academy.getId(),
                                academy.getName()
                        )
                )
                .toList();
    }

    public List<ClassResponse> getClasses(
            Integer academyId
    ) {
        return academyClassRepository
                .findByAcademy_Id(academyId)
                .stream()
                .map(academyClass ->
                        new ClassResponse(
                                academyClass.getId(),
                                academyClass.getClassNumber(),
                                academyClass.getState()
                        )
                )
                .toList();
    }

    public List<SubjectResponse> getSubjects(
            Integer academyId
    ) {
        return List.of(
                new SubjectResponse(
                        1,
                        "로테이션"
                ),
                new SubjectResponse(
                        2,
                        "언리미티드"
                ),
                new SubjectResponse(
                        3,
                        "스타터"
                )
        );
    }

    public List<TeacherResponse> getTeachers(
            Integer academyId
    ) {
        return teacherRepository
                .findByAcademyClass_Academy_Id(academyId)
                .stream()
                .map(this::toTeacherResponse)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<StudentResponse> getStudents(
            Integer academyId,
            Integer classId
    ) {
        return studentRepository
                .findByAcademyIdAndClassId(
                        academyId,
                        classId
                )
                .stream()
                .map(this::toStudentResponse)
                .flatMap(Optional::stream)
                .toList();
    }

    public StudentStatusResponse getStudentStatusOptions(
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
                                        String.valueOf(academyClass.getClassNumber()),
                                        academyClass.getState()
                                )
                        )
                        .toList();

        List<StudentOptionResponse> students =
                academyId == null || classId == null
                        ? List.of()
                        : studentRepository
                        .findByAcademyIdAndClassId(
                                academyId,
                                classId
                        )
                        .stream()
                        .map(this::toStudentOptionResponse)
                        .flatMap(Optional::stream)
                        .toList();

        List<SubjectOptionResponse> subjects =
                List.of(
                        new SubjectOptionResponse(1, "로테이션"),
                        new SubjectOptionResponse(2, "언리미티드"),
                        new SubjectOptionResponse(3, "스타터")
                );

        return new StudentStatusResponse(
                academies,
                classes,
                students,
                subjects
        );
    }

    private Optional<StudentOptionResponse> toStudentOptionResponse(
            Student student
    ) {
        if (student.getSk() == null) {
            return Optional.empty();
        }

        String discordId;

        try {
            discordId =
                    membershipRepository.findDiscordIdBySk(
                            student.getSk()
                    );
        } catch (Exception e) {
            discordId = student.getSk();
        }

        String username =
                discordUserRepository
                        .findByDiscordID(discordId)
                        .map(DiscordUser::getNickname)
                        .orElse(null);

        if (username == null) {
            return Optional.empty();
        }

        return Optional.of(
                new StudentOptionResponse(
                        student.getSk(),
                        discordId,
                        username
                )
        );
    }

    /**
     * 로그인 사용자의 Discord ID
     * → Constellation_Network.search_sk()
     * → SK
     * → Teachers / Students
     *
     * 현재는 SK 조회까지만 이 서비스에서 제공.
     */
    public String findUserSk(
            CustomUser user
    ) {
        return membershipRepository
                .findSkByDiscordId(
                        user.getUsername()
                );
    }

    /**
     * ConfigDB에서 network_operations의
     * academy 설정을 가져온다.
     */
    public JsonNode getAcademyConfig(
            String guildId
    ) {
        ErpSubscriber subscriber =
                erpSubscriberRepository
                        .findByGuildId(guildId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "등록된 guild가 아닙니다. guildId="
                                                + guildId
                                )
                        );

        ModuleConfig moduleConfig =
                moduleConfigRepository
                        .findByIdBotIdAndIdModuleId(
                                subscriber.getBotId(),
                                MODULE_ID
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "network_operations 설정이 없습니다."
                                )
                        );

        JsonNode config =
                moduleConfig.getConfig();

        return config
                .path("config")
                .path("add_on")
                .path("academy");
    }

    private Optional<TeacherResponse> toTeacherResponse(
            Teacher teacher
    ) {
        AcademyClass academyClass =
                teacher.getAcademyClass();

        String discordId = null;
        String username = null;

        if (teacher.getSk() != null) {
            try {
                discordId =
                        membershipRepository
                                .findDiscordIdBySk(
                                        teacher.getSk()
                                );

                username =
                        discordUserRepository
                                .findByDiscordID(discordId)
                                .map(DiscordUser::getNickname)
                                .orElse(null);

            } catch (Exception e) {
                discordId = teacher.getSk();
            }
        }

        if (username == null) {
            return Optional.empty();
        }

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

    private Optional<StudentResponse> toStudentResponse(
            Student student
    ) {
        AcademyClass academyClass =
                student.getAcademyClass();

        String discordId = null;
        String username = null;

        if (student.getSk() != null) {
            try {
                discordId =
                        membershipRepository
                                .findDiscordIdBySk(
                                        student.getSk()
                                );

                username =
                        discordUserRepository
                                .findByDiscordID(discordId)
                                .map(DiscordUser::getNickname)
                                .orElse(null);

            } catch (Exception e) {
                discordId = student.getSk();
            }
        }

        if (username == null) {
            return Optional.empty();
        }

        return Optional.of(
                new StudentResponse(
                        student.getSk(),
                        discordId,
                        username,
                        academyClass != null
                                ? academyClass
                                .getAcademy()
                                .getId()
                                : null,
                        academyClass != null
                                ? academyClass.getId()
                                : null,
                        academyClass != null
                                ? academyClass.getClassNumber()
                                : null
                )
        );
    }
}