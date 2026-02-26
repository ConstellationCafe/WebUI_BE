package com.help.authserver.domain.user.repository;

//import com.help.authserver.domain.user.entity.DiscordUser;
//import com.help.authserver.domain.user.view.DiscordMembershipView;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;

//public interface DiscordMembershipRepository extends JpaRepository<DiscordUser, Long> {
//
//    @Query(value = "select * from vw_DiscordMembership", nativeQuery = true)
//    List<DiscordMembershipView> findAllMemberships();
//
//    @Query(value = "select * from vw_DiscordMembership where discordID = :discordId", nativeQuery = true)
//    List<DiscordMembershipView> findByDiscordID(@Param("discordId") String discordId);
//}