package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.common.Paged;
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
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@Service
public class AdminUserManagementUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final ObjectMapper objectMapper;

    public AdminUserManagementUseCase(
            UserRepositoryPort userRepositoryPort,
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            PasswordEncoder passwordEncoder,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            TokenStateRepositoryPort tokenStateRepositoryPort,
            ObjectMapper objectMapper) {
        this.userRepositoryPort = userRepositoryPort;
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateResult create(CreateCommand command) {
        validateLoginId(command.loginId());
        validateEmail(command.email());
        UserRole role = parseRole(command.role());
        UserStatus status = parseStatus(command.status());
        validateOrganizationReferences(
                command.affiliateId(),
                command.departmentId(),
                command.teamId(),
                command.positionId());
        ensureLoginIdIsUnique(command.loginId());
        ensureEmailIsUnique(command.email(), null);

        String temporaryPassword = temporaryPasswordGenerator.generate();
        Instant now = Instant.now();
        User savedUser =
                userRepositoryPort.save(
                        User.of(
                                UUID.randomUUID(),
                                command.loginId(),
                                passwordEncoder.encode(temporaryPassword),
                                command.name(),
                                command.email(),
                                role,
                                status,
                                command.affiliateId(),
                                command.departmentId(),
                                command.positionId(),
                                command.teamId(),
                                true,
                                command.activeFrom(),
                                command.activeUntil(),
                                now,
                                now));

        UserSummary summary = resolveSummary(savedUser);
        saveAudit(
                command.adminId(),
                command.adminName(),
                command.ipAddress(),
                command.userAgent(),
                "CREATE",
                null,
                snapshot(summary),
                savedUser.id());
        return new CreateResult(savedUser.id(), temporaryPassword, summary);
    }

    @Transactional(readOnly = true)
    public PageResult search(SearchCommand command) {
        Paged<User> page =
                userRepositoryPort.findPage(command.keyword(), command.page(), command.size());
        NameLookups lookups = loadNameLookups(page.content());
        List<UserSummary> items =
                page.content().stream().map(user -> resolveSummary(user, lookups)).toList();
        return new PageResult(
                items,
                page.totalElements(),
                command.page(),
                command.size(),
                calculateTotalPages(page.totalElements(), command.size()));
    }

    @Transactional(readOnly = true)
    public UserSummary get(UUID userId) {
        return resolveSummary(getUserOrThrow(userId));
    }

    @Transactional
    public UserSummary update(UpdateCommand command) {
        validateEmail(command.email());
        UserRole role = parseRole(command.role());
        validateOrganizationReferences(
                command.affiliateId(),
                command.departmentId(),
                command.teamId(),
                command.positionId());

        User current = getUserOrThrow(command.userId());
        ensureManagedUser(current);
        ensureEmailIsUnique(command.email(), current.id());

        User saved =
                userRepositoryPort.save(
                        current.updateAdminProfile(
                                command.name(),
                                command.email(),
                                command.affiliateId(),
                                command.departmentId(),
                                command.positionId(),
                                command.teamId(),
                                role,
                                command.activeFrom(),
                                command.activeUntil()));

        UserSummary summary = resolveSummary(saved);
        saveAudit(
                command.adminId(),
                command.adminName(),
                command.ipAddress(),
                command.userAgent(),
                "UPDATE",
                snapshot(resolveSummary(current)),
                snapshot(summary),
                saved.id());
        return summary;
    }

    @Transactional
    public UserSummary updateStatus(UpdateStatusCommand command) {
        User current = getUserOrThrow(command.userId());
        ensureManagedUser(current);
        UserStatus status = parseManageableStatus(command.status());
        User saved = userRepositoryPort.save(current.changeStatus(status));
        tokenStateRepositoryPort.revokeUserSessions(saved.id(), Instant.now());

        UserSummary summary = resolveSummary(saved);
        saveAudit(
                command.adminId(),
                command.adminName(),
                command.ipAddress(),
                command.userAgent(),
                "STATUS_CHANGE",
                snapshot(resolveSummary(current)),
                snapshot(summary),
                saved.id());
        return summary;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepositoryPort
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "Login ID is required.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "Email is required.");
        }
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "User role is required.");
        }
        try {
            UserRole parsedRole = UserRole.valueOf(role);
            if (parsedRole == UserRole.SYSTEM) {
                throw new IllegalArgumentException("SYSTEM is not supported");
            }
            return parsedRole;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Only ADMIN or USER role is allowed.");
        }
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "User status is required.");
        }
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Unsupported user status.");
        }
    }

    private UserStatus parseManageableStatus(String status) {
        UserStatus parsedStatus = parseStatus(status);
        if (parsedStatus == UserStatus.LOCKED) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "LOCKED status cannot be set directly by admin.");
        }
        return parsedStatus;
    }

    private void ensureLoginIdIsUnique(String loginId) {
        if (userRepositoryPort.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Login ID already exists.");
        }
    }

    private void ensureEmailIsUnique(String email, UUID currentUserId) {
        userRepositoryPort
                .findByEmail(email)
                .ifPresent(
                        user -> {
                            if (currentUserId == null
                                    || !Objects.equals(user.id(), currentUserId)) {
                                throw new BusinessException(
                                        ErrorCode.COMMON_CONFLICT, "Email already exists.");
                            }
                        });
    }

    private void ensureManagedUser(User user) {
        if (user.role() == UserRole.SYSTEM) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN,
                    "System accounts cannot be managed with the admin user API.");
        }
    }

    private void validateOrganizationReferences(
            UUID affiliateId, UUID departmentId, UUID teamId, UUID positionId) {
        Affiliate affiliate = affiliateId == null ? null : loadAffiliate(affiliateId);
        Department department = departmentId == null ? null : loadDepartment(departmentId);
        Team team = teamId == null ? null : loadTeam(teamId);

        if (positionId != null) {
            loadPosition(positionId);
        }
        if (department != null
                && affiliate != null
                && !Objects.equals(department.affiliateId(), affiliate.id())) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "Department does not belong to the selected affiliate.");
        }
        if (team != null
                && department != null
                && !Objects.equals(team.departmentId(), department.id())) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "Team does not belong to the selected department.");
        }
    }

    private Affiliate loadAffiliate(UUID affiliateId) {
        return affiliateRepositoryPort
                .findById(affiliateId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Affiliate not found."));
    }

    private Department loadDepartment(UUID departmentId) {
        return departmentRepositoryPort
                .findById(departmentId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Department not found."));
    }

    private Team loadTeam(UUID teamId) {
        return teamRepositoryPort
                .findById(teamId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Team not found."));
    }

    private Position loadPosition(UUID positionId) {
        return positionRepositoryPort
                .findById(positionId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Position not found."));
    }

    private UserSummary resolveSummary(User user) {
        return new UserSummary(
                user.id(),
                user.loginId(),
                user.name(),
                user.email(),
                user.role().name(),
                user.status().name(),
                user.affiliateId(),
                resolveAffiliateName(user.affiliateId()),
                user.departmentId(),
                resolveDepartmentName(user.departmentId()),
                user.teamId(),
                resolveTeamName(user.teamId()),
                user.positionId(),
                resolvePositionName(user.positionId()),
                user.activeFrom(),
                user.activeUntil(),
                user.createdAt(),
                user.updatedAt(),
                user.initialPasswordChangeRequired());
    }

    private UserSummary resolveSummary(User user, NameLookups lookups) {
        return new UserSummary(
                user.id(),
                user.loginId(),
                user.name(),
                user.email(),
                user.role().name(),
                user.status().name(),
                user.affiliateId(),
                lookups.affiliateNames().get(user.affiliateId()),
                user.departmentId(),
                lookups.departmentNames().get(user.departmentId()),
                user.teamId(),
                lookups.teamNames().get(user.teamId()),
                user.positionId(),
                lookups.positionNames().get(user.positionId()),
                user.activeFrom(),
                user.activeUntil(),
                user.createdAt(),
                user.updatedAt(),
                user.initialPasswordChangeRequired());
    }

    private NameLookups loadNameLookups(List<User> users) {
        return new NameLookups(
                toNameMap(
                        affiliateRepositoryPort.findAllByIds(extractIds(users, User::affiliateId)),
                        Affiliate::id,
                        Affiliate::name),
                toNameMap(
                        departmentRepositoryPort.findAllByIds(
                                extractIds(users, User::departmentId)),
                        Department::id,
                        Department::name),
                toNameMap(
                        teamRepositoryPort.findAllByIds(extractIds(users, User::teamId)),
                        Team::id,
                        Team::name),
                toNameMap(
                        positionRepositoryPort.findAllByIds(extractIds(users, User::positionId)),
                        Position::id,
                        Position::name));
    }

    private <T> Map<UUID, String> toNameMap(
            List<T> items, Function<T, UUID> idExtractor, Function<T, String> nameExtractor) {
        return items.stream().collect(Collectors.toMap(idExtractor, nameExtractor));
    }

    private Set<UUID> extractIds(List<User> users, Function<User, UUID> idExtractor) {
        return users.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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

    private long calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0 || size <= 0) {
            return 0;
        }
        return (totalElements + size - 1) / size;
    }

    private String snapshot(UserSummary summary) {
        try {
            return objectMapper.writeValueAsString(
                    new AuditSnapshot(
                            summary.userId(),
                            summary.loginId(),
                            summary.name(),
                            summary.email(),
                            summary.role(),
                            summary.status(),
                            summary.affiliateId(),
                            summary.departmentId(),
                            summary.teamId(),
                            summary.positionId(),
                            summary.activeFrom(),
                            summary.activeUntil()));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR, "Failed to serialize admin audit snapshot.");
        }
    }

    private void saveAudit(
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent,
            String actionName,
            String beforeValue,
            String afterValue,
            UUID targetId) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        adminId,
                        adminName,
                        "USER",
                        targetId,
                        "USER_MANAGEMENT",
                        actionName,
                        AuditResult.SUCCESS,
                        beforeValue,
                        afterValue,
                        ipAddress,
                        userAgent,
                        Instant.now()));
    }

    public record CreateCommand(
            String loginId,
            String name,
            String email,
            String role,
            String status,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            Instant activeFrom,
            Instant activeUntil,
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent) {}

    public record SearchCommand(String keyword, int page, int size) {}

    public record UpdateCommand(
            UUID userId,
            String name,
            String email,
            String role,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            Instant activeFrom,
            Instant activeUntil,
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent) {}

    public record UpdateStatusCommand(
            UUID userId,
            String status,
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent) {}

    public record CreateResult(UUID userId, String temporaryPassword, UserSummary user) {}

    public record PageResult(
            List<UserSummary> items, long totalElements, int page, int size, long totalPages) {}

    public record UserSummary(
            UUID userId,
            String loginId,
            String name,
            String email,
            String role,
            String status,
            UUID affiliateId,
            String affiliate,
            UUID departmentId,
            String department,
            UUID teamId,
            String team,
            UUID positionId,
            String position,
            Instant activeFrom,
            Instant activeUntil,
            Instant createdAt,
            Instant updatedAt,
            boolean initialPasswordChangeRequired) {}

    private record NameLookups(
            Map<UUID, String> affiliateNames,
            Map<UUID, String> departmentNames,
            Map<UUID, String> teamNames,
            Map<UUID, String> positionNames) {}

    private record AuditSnapshot(
            UUID userId,
            String loginId,
            String name,
            String email,
            String role,
            String status,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            Instant activeFrom,
            Instant activeUntil) {}
}
