package com.skillnova.backend.service;

import com.skillnova.backend.dto.ProfileResponse;
import com.skillnova.backend.dto.ProfileUpdateRequest;
import com.skillnova.backend.dto.RegisterRequest;
import com.skillnova.backend.dto.RegisterResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.EmailAlreadyExistsException;
import com.skillnova.backend.exception.UserNotFoundException;
import com.skillnova.backend.repository.UserRepository;
import com.skillnova.backend.util.SkillsCsvConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .fullName(request.fullName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User saved = userRepository.save(user);

        return new RegisterResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return toProfileResponse(user);
    }

    /**
     * Full-replace update of every profile field (see
     * {@link ProfileUpdateRequest} for the "omitted == null == clear"
     * semantics). Re-fetches a managed entity by ID rather than trusting a
     * detached principal passed in from the controller, so the save always
     * operates on the current persisted state within this transaction.
     */
    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setFullName(request.fullName().trim());
        user.setCurrentJobRole(trimOrNull(request.currentJobRole()));
        user.setYearsExperience(request.yearsExperience());
        user.setCurrentSkills(SkillsCsvConverter.toCsv(request.currentSkills()));
        user.setTargetRole(trimOrNull(request.targetRole()));
        user.setDailyStudyHours(request.dailyStudyHours());
        user.setTargetTimelineMonths(request.targetTimelineMonths());

        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCurrentJobRole(),
                user.getYearsExperience(),
                SkillsCsvConverter.toList(user.getCurrentSkills()),
                user.getTargetRole(),
                user.getDailyStudyHours(),
                user.getTargetTimelineMonths(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
