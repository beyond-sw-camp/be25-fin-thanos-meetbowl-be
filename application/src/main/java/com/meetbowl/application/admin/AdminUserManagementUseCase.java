package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
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

    public AdminUserManagementUseCase(
            UserRepositoryPort userRepositoryPort,
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            PasswordEncoder passwordEncoder,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
    }

    @Transactional
    public CreateResult create(CreateCommand command) {
        validateLoginId(command.loginId());
        validateEmail(command.email());
        UserRole role = parseRole(command.role());
        UserStatus status = parseStatus(command.status());
        // 조직 기준정보는 기존 저장소 포트를 통해서만 존재 여부를 확인한다.
        validateOrganizationReferences(
                command.affiliateId(),
                command.departmentId(),
                command.teamId(),
                command.positionId());
        ensureLoginIdIsUnique(command.loginId());
        ensureEmailIsUnique(command.email(), null);

        // 생성 시에는 임시 비밀번호 원문을 한 번만 만들고, 저장은 해시 값으로만 남긴다.
        String temporaryPassword = temporaryPasswordGenerator.generate();
        Instant now = Instant.now();
        User user =
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
                        now);
        User savedUser = userRepositoryPort.save(user);
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
        Paged<User> page = userRepositoryPort.findPage(command.keyword(), command.page(), command.size());
        List<UserSummary> items = page.content().stream().map(this::resolveSummary).toList();
        return new PageResult(items, page.totalElements(), command.page(), command.size());
    }

    @Transactional(readOnly = true)
    public UserSummary get(UUID userId) {
        User user = getUserOrThrow(userId);
        return resolveSummary(user);
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
        ensureEmailIsUnique(command.email(), current.id());

        User updated =
                current.updateAdminProfile(
                        command.name(),
                        command.email(),
                        command.affiliateId(),
                        command.departmentId(),
                        command.positionId(),
                        command.teamId(),
                        role,
                        command.activeFrom(),
                        command.activeUntil());
        User saved = userRepositoryPort.save(updated);
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
        UserStatus status = parseStatus(command.status());
        User saved = userRepositoryPort.save(current.changeStatus(status));
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
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "로그인 ID는 필수입니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "이메일은 필수입니다.");
        }
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "권한은 필수입니다.");
        }
        try {
            UserRole parsed = UserRole.valueOf(role);
            if (parsed == UserRole.SYSTEM) {
                throw new IllegalArgumentException();
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "권한은 ADMIN 또는 USER만 사용할 수 있습니다.");
        }
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자 상태는 필수입니다.");
        }
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자 상태가 올바르지 않습니다.");
        }
    }

    private void ensureLoginIdIsUnique(String loginId) {
        if (userRepositoryPort.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 사용 중인 로그인 ID입니다.");
        }
    }

    private void ensureEmailIsUnique(String email, UUID currentUserId) {
        userRepositoryPort.findByEmail(email).ifPresent(user -> {
            if (currentUserId == null || !Objects.equals(user.id(), currentUserId)) {
                throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 사용 중인 이메일입니다.");
            }
        });
    }

    private void validateOrganizationReferences(
            UUID affiliateId, UUID departmentId, UUID teamId, UUID positionId) {
        if (affiliateId != null) {
            loadAffiliate(affiliateId);
        }
        if (departmentId != null) {
            loadDepartment(departmentId);
        }
        if (teamId != null) {
            loadTeam(teamId);
        }
        if (positionId != null) {
            loadPosition(positionId);
        }
    }

    private Affiliate loadAffiliate(UUID affiliateId) {
        return affiliateRepositoryPort
                .findById(affiliateId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "계열사 정보를 찾을 수 없습니다."));
    }

    private Department loadDepartment(UUID departmentId) {
        return departmentRepositoryPort
                .findById(departmentId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "부서 정보를 찾을 수 없습니다."));
    }

    private Team loadTeam(UUID teamId) {
        return teamRepositoryPort
                .findById(teamId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "팀 정보를 찾을 수 없습니다."));
    }

    private Position loadPosition(UUID positionId) {
        return positionRepositoryPort
                .findById(positionId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "직급 정보를 찾을 수 없습니다."));
    }

    private UserSummary resolveSummary(User user) {
        // 응답은 도메인 객체가 아니라 화면용 스냅샷만 돌려준다.
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

    private String resolveAffiliateName(UUID affiliateId) {
        return affiliateId == null
                ? null
                : affiliateRepositoryPort.findById(affiliateId).map(Affiliate::name).orElse(null);
    }

    private String resolveDepartmentName(UUID departmentId) {
        return departmentId == null
                ? null
                : departmentRepositoryPort.findById(departmentId).map(Department::name).orElse(null);
    }

    private String resolveTeamName(UUID teamId) {
        return teamId == null ? null : teamRepositoryPort.findById(teamId).map(Team::name).orElse(null);
    }

    private String resolvePositionName(UUID positionId) {
        return positionId == null
                ? null
                : positionRepositoryPort.findById(positionId).map(Position::name).orElse(null);
    }

    private String snapshot(UserSummary summary) {
        return String.format(
                """
                {"userId":"%s","loginId":"%s","name":"%s","email":"%s","role":"%s","status":"%s","affiliateId":"%s","departmentId":"%s","teamId":"%s","positionId":"%s","activeFrom":"%s","activeUntil":"%s"}
                """,
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
                summary.activeUntil()).trim();
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
        // 감사 로그에는 비밀번호 원문이나 임시 비밀번호를 절대 남기지 않는다.
        AdminAuditLog log =
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
                        Instant.now());
        adminAuditLogRepositoryPort.save(log);
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

    public record PageResult(List<UserSummary> items, long totalElements, int page, int size) {}

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
}
