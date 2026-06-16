package com.meetbowl.application.user;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

@Service
public class MyProfileUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;

    public MyProfileUseCase(
            UserRepositoryPort userRepositoryPort,
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
    }

    @Transactional(readOnly = true)
    public MyProfileResult get(UUID currentUserId) {
        return toResult(getUserOrThrow(currentUserId));
    }

    @Transactional
    public MyProfileResult update(UpdateMyProfileCommand command) {
        validateEmail(command.email());
        User current = getUserOrThrow(command.userId());
        ensureEmailIsUnique(command.email(), current.id());
        User saved =
                userRepositoryPort.save(current.updateMyProfile(command.name(), command.email()));
        return toResult(saved);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepositoryPort
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "Email is required.");
        }
    }

    private void ensureEmailIsUnique(String email, UUID currentUserId) {
        userRepositoryPort
                .findByEmail(email)
                .ifPresent(
                        user -> {
                            if (!Objects.equals(user.id(), currentUserId)) {
                                throw new BusinessException(
                                        ErrorCode.COMMON_CONFLICT, "Email already exists.");
                            }
                        });
    }

    private MyProfileResult toResult(User user) {
        return new MyProfileResult(
                user.id(),
                user.loginId(),
                user.name(),
                user.email(),
                user.role().name(),
                user.status().name(),
                resolveAffiliateName(user.affiliateId()),
                resolveDepartmentName(user.departmentId()),
                resolveTeamName(user.teamId()),
                resolvePositionName(user.positionId()),
                user.activeFrom(),
                user.activeUntil());
    }

    private String resolveAffiliateName(UUID affiliateId) {
        return affiliateId == null
                ? null
                : affiliateRepositoryPort.findById(affiliateId).map(Affiliate::name).orElse(null);
    }

    private String resolveDepartmentName(UUID departmentId) {
        return departmentId == null
                ? null
                : departmentRepositoryPort
                        .findById(departmentId)
                        .map(Department::name)
                        .orElse(null);
    }

    private String resolveTeamName(UUID teamId) {
        return teamId == null
                ? null
                : teamRepositoryPort.findById(teamId).map(Team::name).orElse(null);
    }

    private String resolvePositionName(UUID positionId) {
        return positionId == null
                ? null
                : positionRepositoryPort.findById(positionId).map(Position::name).orElse(null);
    }
}
