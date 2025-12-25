package com.help.authserver.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.help.authserver.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	@Transactional(readOnly = true)
	Optional<User> findByUsername(String username);

	@Query("SELECT u FROM User u JOIN FETCH u.userProfile WHERE u.username = :username")
	Optional<User> findWithProfileByUsername(@Param("username") String username);

	boolean existsByUsername(String username);
}
