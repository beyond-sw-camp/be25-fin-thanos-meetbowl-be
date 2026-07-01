package com.meetbowl.application.admin;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import com.meetbowl.application.user.UserSearchReindexRequestDispatcher;
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
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.domain.user.UserStatus;

/** 관리자 회원 관리 유스케이스 회원 생성, 조회, 수정, 상태 관리 기능을 제공합니다. 모든 관리 작업은 감사 로그(Audit Log)에 기록됩니다. */
@Service
public class AdminUserManagementUseCase {
    private static final Instant DEFAULT_ACTIVE_UNTIL = Instant.parse("2999-12-31T00:00:00Z");

    private final UserRepositoryPort userRepositoryPort;
    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final ObjectMapper objectMapper;
    private final UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher;
    private final Clock clock;

    /**
     * AdminUserManagementUseCase 생성자
     *
     * @param userRepositoryPort 사용자 리포지토리
     * @param affiliateRepositoryPort 소속사 리포지토리
     * @param departmentRepositoryPort 부서 리포지토리
     * @param teamRepositoryPort 팀 리포지토리
     * @param positionRepositoryPort 직급 리포지토리
     * @param passwordEncoder 비밀번호 인코더
     * @param temporaryPasswordGenerator 임시 비밀번호 생성기
     * @param adminAuditLogRepositoryPort 관리자 감사 로그 리포지토리
     * @param tokenStateRepositoryPort 토큰 상태 리포지토리
     * @param objectMapper JSON 매핑용 ObjectMapper
     */
    public AdminUserManagementUseCase(
            UserRepositoryPort userRepositoryPort,
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            PasswordEncoder passwordEncoder,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            TokenStateRepositoryPort tokenStateRepositoryPort,
            ObjectMapper objectMapper,
            UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher,
            Clock clock) {
        this.userRepositoryPort = userRepositoryPort;
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.objectMapper = objectMapper;
        this.userSearchReindexRequestDispatcher = userSearchReindexRequestDispatcher;
        this.clock = clock;
    }

    /**
     * 회원 생성 새로운 회원 계정을 생성하고 임시 비밀번호를 발급합니다.
     *
     * @param command 회원 생성 명령
     * @return 생성된 회원 ID, 임시 비밀번호, 회원 요약 정보
     */
    @Transactional
    public CreateResult create(CreateCommand command) {
        validateLoginId(command.loginId());
        validateEmail(command.email());
        UserRole role = parseRole(command.role());
        UserStatus status = parseStatus(command.status());
        UUID affiliateId = requireAdminAffiliateId(command.adminAffiliateId());
        validateOrganizationReferences(
                affiliateId,
                command.departmentId(),
                command.teamId(),
                command.positionId());
        ensureLoginIdIsUnique(command.loginId());
        ensureEmailIsUnique(command.email(), null);

        Instant now = Instant.now(clock);
        User savedUser =
                userRepositoryPort.save(
                        User.of(
                                UUID.randomUUID(),
                                command.loginId(),
                                passwordEncoder.encode(PasswordPolicy.INITIAL_PASSWORD),
                                command.name(),
                                command.email(),
                                role,
                                status,
                                affiliateId,
                                command.departmentId(),
                                command.positionId(),
                                command.teamId(),
                                true,
                                command.activeFrom(),
                                command.activeUntil() == null ? DEFAULT_ACTIVE_UNTIL : command.activeUntil(),
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
                savedUser.id(),
                new AuditTarget(savedUser.loginId(), savedUser.name()));
        // 회원 저장 이후 바로 색인을 갱신해 관리자/사용자 검색 결과가 지연되지 않게 맞춘다.
        publishUserReindex(savedUser.id(), command.adminId(), "USER_CREATED");
        return new CreateResult(savedUser.id(), PasswordPolicy.INITIAL_PASSWORD, summary);
    }

    /**
     * 회원 목록 검색 키워드로 회원을 검색하고 페이지네이션된 결과를 반환합니다.
     *
     * @param command 검색 명령 (키워드, 페이지, 크기)
     * @return 검색 결과와 페이지 정보
     */
    @Transactional(readOnly = true)
    public PageResult search(SearchCommand command) {
        return search(command, null);
    }

    @Transactional(readOnly = true)
    public PageResult search(SearchCommand command, UUID adminAffiliateId) {
        UserStatus status = parseOptionalStatus(command.status());
        Instant dayStart = todayStart();
        Instant nextDayStart = dayStart.plusSeconds(24 * 60 * 60);
        // 검색어는 앞뒤 공백만 정리하고, 빈 값이면 기존 동작처럼 null로 넘겨 전체 조회를 유지한다.
        Paged<User> page =
                userRepositoryPort.search(
                        normalizeKeyword(command.keyword()),
                        adminAffiliateId,
                        null,
                        null,
                        null,
                        status,
                        dayStart,
                        nextDayStart,
                        command.page(),
                        command.size());
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        // 관리자 목록 검색은 부분검색이므로 공백만 제거한 원문을 넘긴다.
        return keyword.trim();
    }

    private Instant todayStart() {
        return Instant.now(clock)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
    }

    /**
     * 회원 상세 조회 특정 회원의 상세 정보를 조회합니다.
     *
     * @param userId 조회할 회원 ID
     * @return 회원 요약 정보
     */
    @Transactional(readOnly = true)
    public UserSummary get(UUID userId) {
        return resolveSummary(getUserOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public UserSummary get(UUID userId, UUID adminAffiliateId) {
        User user = getUserOrThrow(userId);
        ensureManagedUser(user);
        ensureAccessibleToAdmin(user, adminAffiliateId);
        return resolveSummary(user);
    }

    /**
     * 회원 정보 수정 회원의 기본 정보를 수정합니다.
     *
     * @param command 회원 수정 명령
     * @return 수정된 회원 요약 정보
     */
    @Transactional
    public UserSummary update(UpdateCommand command) {
        return update(command, null);
    }

    @Transactional
    public UserSummary update(UpdateCommand command, UUID adminAffiliateId) {
        validateEmail(command.email());
        UserRole role = parseRole(command.role());
        User current = getUserOrThrow(command.userId());
        ensureManagedUser(current);
        ensureAccessibleToAdmin(current, adminAffiliateId);
        UUID targetAffiliateId =
                command.affiliateId() != null ? command.affiliateId() : current.affiliateId();
        validateOrganizationReferences(
                targetAffiliateId,
                command.departmentId(),
                command.teamId(),
                command.positionId());

        ensureTargetAffiliateMatchesAdminScope(targetAffiliateId, adminAffiliateId);
        ensureEmailIsUnique(command.email(), current.id());

        User saved =
                userRepositoryPort.save(
                        current.updateAdminProfile(
                                command.name(),
                                command.email(),
                                targetAffiliateId,
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
                saved.id(),
                new AuditTarget(saved.loginId(), saved.name()));
        // 관리자 수정은 이름, 이메일, 권한, 조직 표시값을 바꿀 수 있어 검색 문서도 즉시 맞춘다.
        publishUserReindex(saved.id(), command.adminId(), "USER_UPDATED");
        return summary;
    }

    /**
     * 회원 상태 변경 회원의 상태를 변경하고 모든 세션을 만료시킵니다.
     *
     * @param command 상태 변경 명령
     * @return 상태가 변경된 회원 요약 정보
     */
    @Transactional
    public UserSummary updateStatus(UpdateStatusCommand command) {
        return updateStatus(command, null);
    }

    @Transactional
    public UserSummary updateStatus(UpdateStatusCommand command, UUID adminAffiliateId) {
        User current = getUserOrThrow(command.userId());
        ensureManagedUser(current);
        ensureAccessibleToAdmin(current, adminAffiliateId);
        UserStatus status = parseManageableStatus(command.status());
        ensureNotSelfInactive(current.id(), command.adminId(), status);
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
                saved.id(),
                new AuditTarget(saved.loginId(), saved.name()));
        // 상태값은 관리자/일반 사용자 검색 필터에 바로 쓰이므로 저장 직후 재색인한다.
        publishUserReindex(saved.id(), command.adminId(), "USER_STATUS_UPDATED");
        return summary;
    }

    /** 회원 삭제 API는 데이터 무결성을 위해 hard delete 대신 INACTIVE 처리로 동작한다. */
    @Transactional
    public UserSummary delete(DeleteCommand command) {
        return delete(command, null);
    }

    @Transactional
    public UserSummary delete(DeleteCommand command, UUID adminAffiliateId) {
        User current =
                userRepositoryPort
                        .findByIdIncludingDeleted(command.userId())
                        .orElseThrow(
                                () -> {
                                    saveDeleteFailureAudit(
                                            command, command.userId(), null, "삭제할 회원을 찾을 수 없습니다.");
                                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                                });
        ensureManagedUser(current);
        ensureAccessibleToAdmin(current, adminAffiliateId);
        ensureNotSelfDelete(command, current);
        ensureUserNotAlreadyDeleted(command, current);

        // tombstone 치환 전에 원본 로그인 ID/이름을 고정해 삭제 감사 로그에 남긴다.
        AuditTarget auditTarget = new AuditTarget(current.loginId(), current.name());
        Instant deletedAt = Instant.now(clock);
        User saved =
                userRepositoryPort.save(
                        current.delete(
                                deletedAt,
                                createDeletedLoginId(current.id()),
                                createDeletedEmail(current.id())));
        tokenStateRepositoryPort.revokeUserSessions(saved.id(), deletedAt);

        UserSummary before = resolveSummary(current);
        UserSummary after = resolveSummary(saved);
        saveAudit(
                command.adminId(),
                command.adminName(),
                command.ipAddress(),
                command.userAgent(),
                "DELETE",
                snapshot(before),
                snapshot(after),
                saved.id(),
                auditTarget);
        // 삭제 API도 검색 문서에는 상태 변경으로 반영해야 목록/검색 결과가 비활성화 상태와 일치한다.
        publishUserReindex(saved.id(), command.adminId(), "USER_DELETED");
        return after;
    }

    /**
     * 회원 조회 또는 예외 발생 존재하지 않는 회원인 경우 예외를 발생시킵니다.
     *
     * @param userId 조회할 회원 ID
     * @return 회원 엔티티
     */
    private User getUserOrThrow(UUID userId) {
        return userRepositoryPort
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 로그인 ID 유효성 검사
     *
     * @param loginId 로그인 ID
     */
    private void validateLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "Login ID is required.");
        }
    }

    /**
     * 이메일 유효성 검사
     *
     * @param email 이메일
     */
    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "Email is required.");
        }
    }

    /**
     * 역할 파싱 문자열 역할을 UserRole enum으로 변환합니다. SYSTEM 역할은 허용되지 않습니다.
     *
     * @param role 역할 문자열
     * @return UserRole enum
     */
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

    /**
     * 상태 파싱 문자열 상태를 UserStatus enum으로 변환합니다.
     *
     * @param status 상태 문자열
     * @return UserStatus enum
     */
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

    /**
     * 관리 가능한 상태 파싱 LOCKED 상태는 관리자가 직접 설정할 수 없습니다.
     *
     * @param status 상태 문자열
     * @return UserStatus enum
     */
    private UserStatus parseManageableStatus(String status) {
        UserStatus parsedStatus = parseStatus(status);
        if (parsedStatus == UserStatus.LOCKED) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "LOCKED status cannot be set directly by admin.");
        }
        return parsedStatus;
    }

    private UserStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return parseManageableStatus(status.trim().toUpperCase());
    }

    /**
     * 로그인 ID 중복 확인
     *
     * @param loginId 로그인 ID
     */
    private void ensureLoginIdIsUnique(String loginId) {
        if (userRepositoryPort.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Login ID already exists.");
        }
    }

    /**
     * 이메일 중복 확인 자기 자신의 이메일인 경우 중복으로 간주하지 않습니다.
     *
     * @param email 이메일
     * @param currentUserId 현재 사용자 ID (수정 시 사용)
     */
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

    /**
     * 관리 가능한 사용자 확인 SYSTEM 역할 사용자는 관리할 수 없습니다.
     *
     * @param user 사용자 엔티티
     */
    private void ensureManagedUser(User user) {
        if (user.role() == UserRole.SYSTEM) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN,
                    "System accounts cannot be managed with the admin user API.");
        }
    }

    private void ensureNotSelfInactive(UUID targetUserId, UUID adminId, UserStatus status) {
        if (status == UserStatus.INACTIVE && Objects.equals(targetUserId, adminId)) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "자기 자신의 계정은 비활성화할 수 없습니다.");
        }
    }

    private void ensureNotSelfDelete(DeleteCommand command, User user) {
        if (Objects.equals(user.id(), command.adminId())) {
            saveDeleteFailureAudit(
                    command, user.id(), snapshot(resolveSummary(user)), "자기 자신의 계정은 삭제할 수 없습니다.");
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "자기 자신의 계정은 삭제할 수 없습니다.");
        }
    }

    private void ensureUserNotAlreadyDeleted(DeleteCommand command, User user) {
        if (user.isDeleted()) {
            saveDeleteFailureAudit(
                    command, user.id(), snapshot(resolveSummary(user)), "이미 비활성화된 회원입니다.");
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 비활성화된 회원입니다.");
        }
    }

    /**
     * 조직 참조 유효성 검사 소속사, 부서, 팀, 직급의 존재 여부와 관계를 검증합니다. 부서는 소속사에 속해야 하며, 팀은 부서에 속해야 합니다.
     *
     * @param affiliateId 소속사 ID
     * @param departmentId 부서 ID
     * @param teamId 팀 ID
     * @param positionId 직급 ID
     */
    private void validateOrganizationReferences(
            UUID affiliateId, UUID departmentId, UUID teamId, UUID positionId) {
        Affiliate affiliate = affiliateId == null ? null : loadAffiliate(affiliateId);
        Department department = departmentId == null ? null : loadDepartment(departmentId);
        Team team = teamId == null ? null : loadTeam(teamId);

        if (positionId != null) {
            Position position = loadPosition(positionId);
            if (affiliate != null && !Objects.equals(position.affiliateId(), affiliate.id())) {
                throw new BusinessException(
                        ErrorCode.COMMON_INVALID_REQUEST,
                        "Position does not belong to the selected affiliate.");
            }
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

    /**
     * 관리자 회원 생성은 현재 로그인한 관리자의 계열사 범위 안에서만 허용한다.
     * 인증 토큰에 계열사 정보가 없다면 조직 소속을 결정할 수 없으므로 요청 자체를 거절한다.
     */
    private UUID requireAdminAffiliateId(UUID adminAffiliateId) {
        if (adminAffiliateId == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "Admin affiliate is required to create user.");
        }
        loadAffiliate(adminAffiliateId);
        return adminAffiliateId;
    }

    /**
     * 소속사 로드
     *
     * @param affiliateId 소속사 ID
     * @return 소속사 엔티티
     */
    private Affiliate loadAffiliate(UUID affiliateId) {
        return affiliateRepositoryPort
                .findById(affiliateId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Affiliate not found."));
    }

    /**
     * 부서 로드
     *
     * @param departmentId 부서 ID
     * @return 부서 엔티티
     */
    private Department loadDepartment(UUID departmentId) {
        return departmentRepositoryPort
                .findById(departmentId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Department not found."));
    }

    /**
     * 팀 로드
     *
     * @param teamId 팀 ID
     * @return 팀 엔티티
     */
    private Team loadTeam(UUID teamId) {
        return teamRepositoryPort
                .findById(teamId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Team not found."));
    }

    /**
     * 직급 로드
     *
     * @param positionId 직급 ID
     * @return 직급 엔티티
     */
    private Position loadPosition(UUID positionId) {
        return positionRepositoryPort
                .findById(positionId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Position not found."));
    }

    /**
     * 회원 요약 정보 생성 사용자 엔티티를 응답용 요약 정보로 변환합니다.
     *
     * @param user 사용자 엔티티
     * @return 회원 요약 정보
     */
    private UserSummary resolveSummary(User user) {
        return new UserSummary(
                user.id(),
                user.loginId(),
                user.name(),
                user.email(),
                user.role().name(),
                user.effectiveStatusAt(Instant.now(clock)).name(),
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

    /**
     * 회원 요약 정보 생성 (이름 조회 캐시 사용) 목록 조회 시 성능 최적화를 위해 미리 로드된 이름 맵을 사용합니다.
     *
     * @param user 사용자 엔티티
     * @param lookups 이름 조회 캐시
     * @return 회원 요약 정보
     */
    private UserSummary resolveSummary(User user, NameLookups lookups) {
        return new UserSummary(
                user.id(),
                user.loginId(),
                user.name(),
                user.email(),
                user.role().name(),
                user.effectiveStatusAt(Instant.now(clock)).name(),
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

    /**
     * 이름 조회 캐시 로드 여러 사용자의 조직 이름을 일괄 조회하여 캐시합니다.
     *
     * @param users 사용자 목록
     * @return 이름 조회 캐시
     */
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

    /**
     * 이름 맵 생성 엔티티 목록에서 ID와 이름의 맵을 생성합니다.
     *
     * @param items 엔티티 목록
     * @param idExtractor ID 추출 함수
     * @param nameExtractor 이름 추출 함수
     * @return ID-이름 맵
     */
    private <T> Map<UUID, String> toNameMap(
            List<T> items, Function<T, UUID> idExtractor, Function<T, String> nameExtractor) {
        return items.stream().collect(Collectors.toMap(idExtractor, nameExtractor));
    }

    /**
     * ID 추출 사용자 목록에서 특정 필드의 ID들을 추출합니다.
     *
     * @param users 사용자 목록
     * @param idExtractor ID 추출 함수
     * @return ID 집합
     */
    private Set<UUID> extractIds(List<User> users, Function<User, UUID> idExtractor) {
        return users.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 소속사 이름 확인
     *
     * @param affiliateId 소속사 ID
     * @return 소속사 이름
     */
    private String resolveAffiliateName(UUID affiliateId) {
        return affiliateId == null
                ? null
                : affiliateRepositoryPort.findById(affiliateId).map(Affiliate::name).orElse(null);
    }

    /**
     * 부서 이름 확인
     *
     * @param departmentId 부서 ID
     * @return 부서 이름
     */
    private String resolveDepartmentName(UUID departmentId) {
        return departmentId == null
                ? null
                : departmentRepositoryPort
                        .findById(departmentId)
                        .map(Department::name)
                        .orElse(null);
    }

    /**
     * 팀 이름 확인
     *
     * @param teamId 팀 ID
     * @return 팀 이름
     */
    private String resolveTeamName(UUID teamId) {
        return teamId == null
                ? null
                : teamRepositoryPort.findById(teamId).map(Team::name).orElse(null);
    }

    /**
     * 직급 이름 확인
     *
     * @param positionId 직급 ID
     * @return 직급 이름
     */
    private String resolvePositionName(UUID positionId) {
        return positionId == null
                ? null
                : positionRepositoryPort.findById(positionId).map(Position::name).orElse(null);
    }

    /**
     * 전체 페이지 수 계산
     *
     * @param totalElements 전체 요소 수
     * @param size 페이지 크기
     * @return 전체 페이지 수
     */
    private long calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0 || size <= 0) {
            return 0;
        }
        return (totalElements + size - 1) / size;
    }

    /**
     * 감사 스냅샷 생성 회원 요약 정보를 JSON 문자열로 직렬화합니다.
     *
     * @param summary 회원 요약 정보
     * @return JSON 문자열
     */
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

    private String failureSnapshot(String message) {
        try {
            return objectMapper.writeValueAsString(new FailureSnapshot(message));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR, "Failed to serialize admin audit snapshot.");
        }
    }

    /**
     * 감사 로그 저장 관리자 작업 내용을 감사 로그에 기록합니다.
     *
     * @param adminId 관리자 ID
     * @param adminName 관리자 이름
     * @param ipAddress IP 주소
     * @param userAgent User-Agent
     * @param actionName 작업 이름 (CREATE, UPDATE, STATUS_CHANGE)
     * @param beforeValue 변경 전 값 (JSON)
     * @param afterValue 변경 후 값 (JSON)
     * @param targetId 대상 ID
     */
    private void saveAudit(
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent,
            String actionName,
            String beforeValue,
            String afterValue,
            UUID targetId,
            AuditTarget auditTarget) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        adminId,
                        adminName,
                        "USER",
                        targetId,
                        auditTarget == null ? null : auditTarget.loginId(),
                        auditTarget == null ? null : auditTarget.name(),
                        "USER_MANAGEMENT",
                        actionName,
                        AuditResult.SUCCESS,
                        beforeValue,
                        afterValue,
                        ipAddress,
                        userAgent,
                        Instant.now(clock)));
    }

    private void saveDeleteFailureAudit(
            DeleteCommand command, UUID targetId, String beforeValue, String message) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        command.adminId(),
                        command.adminName(),
                        "USER",
                        targetId,
                        null,
                        null,
                        "USER_MANAGEMENT",
                        "DELETE",
                        AuditResult.FAILURE,
                        beforeValue,
                        failureSnapshot(message),
                        command.ipAddress(),
                        command.userAgent(),
                        Instant.now(clock)));
    }

    private void publishUserReindex(UUID userId, UUID requestedByUserId, String reason) {
        // 회원 문서는 사용자명/이메일/권한/상태/조직 표시값이 섞여 있어 수정 API가 끝난 뒤 사용자 단위 비동기 upsert로 맞춘다.
        userSearchReindexRequestDispatcher.publishAfterCommit(
                new UserSearchReindexRequestedEvent(
                        reason, false, List.of(userId), null, null, null, null, requestedByUserId));
    }

    /** 회원 생성 명령 */
    private String createDeletedLoginId(UUID userId) {
        return "deleted-" + userId.toString().replace("-", "");
    }

    private String createDeletedEmail(UUID userId) {
        return "deleted+" + userId + "@deleted.local";
    }

    public record CreateCommand(
            String loginId,
            String name,
            String email,
            String role,
            String status,
            UUID adminAffiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            Instant activeFrom,
            Instant activeUntil,
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent) {}

    /** 회원 검색 명령 */
    public record SearchCommand(String keyword, String status, int page, int size) {}

    /** 회원 수정 명령 */
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

    /** 회원 상태 변경 명령 */
    public record UpdateStatusCommand(
            UUID userId,
            String status,
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent) {}

    /** ?뚯썝 ??젣 紐낅졊 */
    public record DeleteCommand(
            UUID userId, UUID adminId, String adminName, String ipAddress, String userAgent) {}

    /** 회원 생성 결과 */
    public record CreateResult(UUID userId, String temporaryPassword, UserSummary user) {}

    /** 회원 목록 검색 결과 */
    public record PageResult(
            List<UserSummary> items, long totalElements, int page, int size, long totalPages) {}

    /** 회원 요약 정보 */
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

    /** 이름 조회 캐시 목록 조회 시 성능 최적화를 위해 사용 */
    private record NameLookups(
            Map<UUID, String> affiliateNames,
            Map<UUID, String> departmentNames,
            Map<UUID, String> teamNames,
            Map<UUID, String> positionNames) {}

    /** 감사 로그용 스냅샷 */
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

    private record AuditTarget(String loginId, String name) {}

    private record FailureSnapshot(String message) {}

    private void ensureAccessibleToAdmin(User user, UUID adminAffiliateId) {
        if (adminAffiliateId == null) {
            return;
        }
        if (!Objects.equals(user.affiliateId(), adminAffiliateId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "다른 계열사의 회원은 관리할 수 없습니다.");
        }
    }

    private void ensureTargetAffiliateMatchesAdminScope(
            UUID targetAffiliateId, UUID adminAffiliateId) {
        if (adminAffiliateId == null || targetAffiliateId == null) {
            return;
        }
        if (!Objects.equals(targetAffiliateId, adminAffiliateId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "다른 계열사의 회원은 관리할 수 없습니다.");
        }
    }
}
