package com.help.authserver.domain.user.entity;

import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import com.help.authserver.global.common.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString(exclude = "userProfile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Users")
@Entity
public class User extends BaseTimeEntity {
	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "user_id", columnDefinition = "BINARY(16)")
	private UUID userId;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, name = "user_pw")
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "user_role")
	private UserRole role = UserRole.USER;

	@OneToOne(
		mappedBy = "user",
		cascade = CascadeType.ALL,
		orphanRemoval = true, fetch = FetchType.LAZY,
		optional = false
	)
	private UserProfile userProfile;

	public static User ofId(final UUID userId) {
		return new User(userId);
	}

	private User(final UUID userId) {
		this.userId = userId;
	}

	@Builder
	public User(final String username, final String password, final String nickname) {
		this.username = username;
		this.password = password;
		userProfile = new UserProfile(this, username, nickname);
	}

	public void updatePassword(final String newPassword) {
		password = newPassword;
	}
}
