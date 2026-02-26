//package com.help.backend.domain.repository.service;
//
//import com.help.backend.domain.repository.dto.request.repository.MembershipDto;
//import com.help.backend.domain.repository.entity.MembershipEntity;
//import com.help.backend.domain.repository.repository.MembershipRepository;
//import com.help.global.common.exception.ErrorCode;
//import com.help.global.common.response.ApiResponse;
//import com.help.global.jwt.CustomUser;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import jakarta.persistence.StoredProcedureQuery;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MembershipService {
//    private final MembershipRepository membershipRepository;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    public ApiResponse<?> getMembership(CustomUser user) {
//        String discordId = user.getUsername();
//
//        return membershipRepository.findByDiscordID(discordId)
//                .<ApiResponse<?>>map(ApiResponse::success)
//                .orElseGet(() -> ApiResponse.error(ErrorCode.USER_NOT_FOUND));
//    }
//
//    public ApiResponse<?> save(MembershipDto membershipDto) {
//        // TODO : Dto -> Entity 변환
////        MembershipEntity entity = MembershipEntity.builder()
////                .discordId(membershipDto.getDiscordId())
////                .guildId(membershipDto.getGuildId())
////                .role(membershipDto.getRole())
////                .build();
////        membershipRepository.save(entitiy);
//
////        log.info("{} records saved successfully", entities.size());
//        return ApiResponse.success("Data saved successfully");
//    }
//}
