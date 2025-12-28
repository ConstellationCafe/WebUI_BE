package com.help.authserver.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.help.authserver.domain.user.dto.request.PasswordChangeRequestDto;
import com.help.authserver.domain.user.dto.request.ProfileUpdateRequestDto;
import com.help.authserver.domain.user.dto.request.SignupRequestDto;
import com.help.authserver.domain.user.entity.EmailUser;
import com.help.authserver.domain.user.repository.UserRepository;
import com.help.global.common.exception.CustomException;
import com.help.global.common.exception.ErrorCode;
import com.help.global.jwt.CustomUser;

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
		final EmailUser emailUser = EmailUser.builder()
			.username(userInfo.getEmail())
			.password(encodedPassword)
			.nickname(userInfo.getNickname())
			.build();
		userRepository.save(emailUser);
		log.info("User registered: {}", emailUser);
	}

	@Transactional
	public void updateUserPassword(
		final CustomUser user,
		final PasswordChangeRequestDto requestDto
	) {
		final EmailUser loginEmailUser = userRepository.findByUsername(user.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		if (!passwordEncoder.matches(requestDto.oldPassword(), loginEmailUser.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}
		loginEmailUser.updatePassword(passwordEncoder.encode(requestDto.newPassword()));
		userRepository.save(loginEmailUser);
		log.info("User password updated: {}", loginEmailUser);
	}

	@Transactional
	public void updateUserProfile(final EmailUser loginEmailUser, final ProfileUpdateRequestDto requestDto) {
		loginEmailUser.getUserProfile().updateNickname(requestDto.getNickname());
		requestDto.getDescription().ifPresent(loginEmailUser.getUserProfile()::updateDescription);
		userRepository.save(loginEmailUser);
		log.info("User profile updated: {}", loginEmailUser);
	}

	public EmailUser findUserByUsername(final String username) {
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
		final EmailUser loginEmailUser = userRepository.findByUsername(user.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		userRepository.delete(loginEmailUser);
		log.info("User deleted: {}", loginEmailUser);
	}

	@Transactional
	public void changeUserPassword(
		final CustomUser user,
		final String newPassword
	) {
		final EmailUser loginEmailUser = userRepository.findByUsername(user.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		loginEmailUser.updatePassword(passwordEncoder.encode(newPassword));
		userRepository.save(loginEmailUser);
		log.info("User password updated: {}", loginEmailUser);
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
