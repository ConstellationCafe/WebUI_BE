package com.help.authserver.domain.user.repository.legacy;

//public interface UserRepository extends JpaRepository<EmailUser, Long> {
//	@Transactional(readOnly = true)
//	Optional<EmailUser> findByUsername(String username);
//
//	@Query("SELECT u FROM EmailUser u JOIN FETCH u.userProfile WHERE u.username = :username")
//	Optional<EmailUser> findWithProfileByUsername(@Param("username") String username);
//
//	boolean existsByUsername(String username);
//}
