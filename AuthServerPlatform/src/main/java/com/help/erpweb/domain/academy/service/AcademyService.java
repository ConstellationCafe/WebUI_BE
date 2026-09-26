package com.help.erpweb.domain.academy.service;

import com.fasterxml.jackson.databind.JsonNode;
// authserver/erpweb 서비스 분리 준비: DiscordUser/ErpSubscriber는 로그인과
// erpweb 양쪽에서 쓰이는 공용 식별 데이터라 com.help.global.discord로 옮겼다
// (기존 FIXME였던 "AuthServer -> ERPWeb 이동"은 이 방향으로 해결됨).
import com.help.global.discord.config.ERPSubscriberRepository;
import com.help.global.discord.config.ErpSubscriber;
import com.help.global.discord.identity.DiscordUser;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.erpweb.domain.academy.authorization.AcademyAuthorization;
import com.help.erpweb.domain.academy.dto.response.*;
import com.help.erpweb.domain.academy.entity.*;
import com.help.erpweb.domain.academy.repository.*;
import com.help.erpweb.domain.config.entity.ModuleConfig;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import com.help.global.chat.ChatIdentities;
import com.help.global.guild.GuildContext;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final AcademyMemberRepository academyMemberRepository;
    // Authorization
    private final AcademyAuthorization academyAuthorization;

    public AcademyPermissionResponse getMyPermissions(
            Authentication authentication
    ) {
        // isAdmin
        boolean isAdmin = academyAuthorization
                .hasGlobalAccess(authentication);
        // academies
        // 2026-09-26: discordId(전역)가 아니라 sk(=(botId, discordId), 방 스코프)로
        // 조회한다 — 같은 사람이 다른 방에서 가진 멤버십까지 섞여 나오는 것을 막는다.
        String sk = membershipRepository.findSkByChatUser(
                ChatIdentities.fromPrincipal((CustomUser) authentication.getPrincipal())
        );
        List<AcademyMember> members = academyMemberRepository
                .findAllBySk(sk);
        Map<String, List<AcademyMember>> grouped = members
                .stream()
                .collect(
                        Collectors.groupingBy(
                                member ->
                                        member.getAcademyId()
                                                + ":"
                                                + member.getRoleName()
                        )
                );
        List<AcademyPermissionItemResponse> academies = grouped
                .values()
                .stream()
                .map(group -> {
                    AcademyMember first = group.get(0);
                    List<Integer> classIds = group
                            .stream()
                            .map(AcademyMember::getClassId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .sorted()
                            .toList();
                    return new AcademyPermissionItemResponse(
                            first.getAcademyId(),
                            first.getRoleName(),
                            classIds
                    );
                })
                .sorted(
                        Comparator.comparing(
                                AcademyPermissionItemResponse::academyId
                        )
                )
                .toList();

        return new AcademyPermissionResponse(
                isAdmin,
                academies
        );
    }

    /**
     * 2026-09-26: 기존에는 academyId 무관하게 모든 Academy를 반환했다 — 방(봇)
     * 경계가 전혀 없어서, 어떤 방으로 로그인했든 다른 방의 Academy 목록까지
     * 그대로 노출되는 문제가 있었다. ADMIN은 기존처럼 전체를 보고, 그 외에는
     * 본인 sk(=현재 방 스코프 신원)가 속한 Academy만 보이도록 스코프한다.
     */
    public List<AcademyResponse> getAcademies(
            Authentication authentication
    ) {
        final List<Academy> academies;
        if (academyAuthorization.hasGlobalAccess(authentication)) {
            academies = academyRepository.findAll();
        } else {
            String sk = membershipRepository.findSkByChatUser(
                    ChatIdentities.fromPrincipal((CustomUser) authentication.getPrincipal())
            );
            List<Integer> accessibleAcademyIds =
                    academyMemberRepository.findAccessibleAcademyIds(sk);
            academies = academyRepository.findAllByIdIn(accessibleAcademyIds);
        }

        return academies
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
        // 달빛은 로테이션만 수업
        if (academyId == 1) {
            return List.of(
                    new SubjectResponse(
                            1,
                            "로테이션"
                    )
            );
        // 별빛은 모두 수업
        } else {
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
    }

    public List<TeacherResponse> getTeachers(
            Integer academyId,
            Integer classId
    ) {
        // teachers
        List<Teacher> teachers = teacherRepository
                        .findByAcademyIdWithClass(academyId, classId);
        // discordIdBySk
        List<String> sks = teachers.stream()
                        .map(Teacher::getSk)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<String, String> discordIdBySk = membershipRepository
                        .findDiscordIdsBySk(sks);
        // discordUserById
        List<String> discordIds = discordIdBySk
                        .values()
                        .stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<String, DiscordUser> discordUserById = discordUserRepository
                        .findActiveByBotIdAndDiscordIDIn(GuildContext.requireBotId(), discordIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        DiscordUser::getDiscordID,
                                        Function.identity(),
                                        (existing, replacement) -> existing
                                )
                        );
        // List<TeacherResponse>
        return teachers
                .stream()
                .map(teacher ->
                        toTeacherResponse(
                                teacher,
                                discordIdBySk,
                                discordUserById
                        )
                )
                .flatMap(Optional::stream)
                .toList();
    }

    public List<StudentResponse> getStudents(
            Integer academyId,
            Integer classId
    ) {
        // students
        List<Student> students = studentRepository
                        .findByAcademyIdAndClassId(
                                academyId,
                                classId
                        );
        // discordIdBySk
        List<String> sks = students.stream()
                        .map(Student::getSk)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<String, String> discordIdBySk = membershipRepository
                        .findDiscordIdsBySk(sks);
        // discordUserById
        List<String> discordIds = discordIdBySk.values()
                        .stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<String, DiscordUser> discordUserById = discordUserRepository
                        .findActiveByBotIdAndDiscordIDIn(GuildContext.requireBotId(), discordIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        DiscordUser::getDiscordID,
                                        Function.identity()
                                )
                        );
        // List<StudentResponse>
        return students.stream()
                .map(student ->
                        toStudentResponse(
                                student,
                                discordIdBySk,
                                discordUserById
                        )
                )
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

        List<OptionResponse> students =
                academyId == null || classId == null
                        ? List.of()
                        : getStudents(academyId, classId)
                        .stream()
                        .map(student -> new OptionResponse(
                                student.sk(),
                                student.discordID(),
                                student.name()
                        ))
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

    private Optional<OptionResponse> toOptionResponse(
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
                        .findByBotIdAndDiscordID(GuildContext.requireBotId(), discordId)
                        .map(DiscordUser::getNickname)
                        .orElse(null);

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
                .findSkByChatUser(
                        ChatIdentities.fromPrincipal(user)
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
            Teacher teacher,
            Map<String, String> discordIdBySk,
            Map<String, DiscordUser> discordUserById
    ) {
        if (teacher.getSk() == null) {
            return Optional.empty();
        }

        String discordId = discordIdBySk.get(
                teacher.getSk()
        );
        if (discordId == null) {
            return Optional.empty();
        }

        DiscordUser discordUser = discordUserById.get(
                discordId
        );
        if (discordUser == null || discordUser.getNickname() == null) {
            return Optional.empty();
        }

        AcademyClass academyClass = teacher.getAcademyClass();
        return Optional.of(
                new TeacherResponse(
                        teacher.getSk(),
                        discordId,
                        discordUser.getNickname(),
                        academyClass != null
                                ? academyClass.getId()
                                : null,
                        academyClass != null
                                ? academyClass.getClassNumber()
                                : null,
                        teacher.getState()
                )
        );
    }

    private Optional<StudentResponse> toStudentResponse(
            Student student,
            Map<String, String> discordIdBySk,
            Map<String, DiscordUser> discordUserById
    ) {
        if (student.getSk() == null) {
            return Optional.empty();
        }

        String discordId = discordIdBySk.get(student.getSk());
        if (discordId == null) {
            return Optional.empty();
        }

        DiscordUser discordUser = discordUserById.get(discordId);
        if (discordUser == null || discordUser.getNickname() == null) {
            return Optional.empty();
        }

        AcademyClass academyClass = student.getAcademyClass();
        return Optional.of(
                new StudentResponse(
                        student.getSk(),
                        discordId,
                        discordUser.getNickname(),
                        academyClass != null
                                ? academyClass.getAcademy().getId()
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
}
