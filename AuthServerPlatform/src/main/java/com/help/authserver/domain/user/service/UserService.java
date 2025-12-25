package com.help.authserver.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.help.authserver.domain.user.dto.request.PasswordChangeRequestDto;
import com.help.authserver.domain.user.dto.request.ProfileUpdateRequestDto;
import com.help.authserver.domain.user.dto.request.SignupRequestDto;
import com.help.authserver.domain.user.entity.User;
import com.help.authserver.domain.user.repository.UserRepository;
import com.help.authserver.global.common.exception.CustomException;
import com.help.authserver.global.common.exception.ErrorCode;
import com.help.authserver.global.jwt.CustomUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public void registerUser(final SignupRequestDto userInfo) {
		final String encodedPassword = passwordEncoder.encode(userInfo.getPassword());
		validateDuplicateEmail(userInfo.getEmail());
		final User user = User.builder()
			.username(userInfo.getEmail())
			.password(encodedPassword)
			.nickname(userInfo.getNickname())
			.build();
		userRepository.save(user);
		log.info("User registered: {}", user);
	}

	@Transactional
	public void updateUserPassword(
		final CustomUser user,
		final PasswordChangeRequestDto requestDto
	) {
		final User loginUser = userRepository.findByUsername(user.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		if (!passwordEncoder.matches(requestDto.oldPassword(), loginUser.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}
		loginUser.updatePassword(passwordEncoder.encode(requestDto.newPassword()));
		userRepository.save(loginUser);
		log.info("User password updated: {}", loginUser);
	}

	@Transactional
	public void updateUserProfile(final User loginUser, final ProfileUpdateRequestDto requestDto) {
		loginUser.getUserProfile().updateNickname(requestDto.getNickname());
		requestDto.getDescription().ifPresent(loginUser.getUserProfile()::updateDescription);
		userRepository.save(loginUser);
		log.info("User profile updated: {}", loginUser);
	}

	public User findUserByUsername(final String username) {
		return userRepository.findWithProfileByUsername(username)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	public void validateDuplicateEmail(final String email) {
		if (userRepository.existsByUsername(email)) {
			throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}
	}

	@Transactional
	public void deleteUser(final CustomUser user) {
		final User loginUser = userRepository.findByUsername(user.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		userRepository.delete(loginUser);
		log.info("User deleted: {}", loginUser);
	}

	@Transactional
	public void changeUserPassword(
		final CustomUser user,
		final String newPassword
	) {
		final User loginUser = userRepository.findByUsername(user.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		loginUser.updatePassword(passwordEncoder.encode(newPassword));
		userRepository.save(loginUser);
		log.info("User password updated: {}", loginUser);
	}

	public void checkEmailOwnership(
		final CustomUser user,
		final String email
	) {
		if (!user.getUsername().equals(email)) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_PASSWORD_CHANGE);
		}
	}
}
