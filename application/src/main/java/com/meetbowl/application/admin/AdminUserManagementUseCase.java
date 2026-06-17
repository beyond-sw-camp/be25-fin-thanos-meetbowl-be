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

/** 관리자 회원 관리 유스케이스 회원 생성, 조회, 수정, 상태 관리 기능을 제공합니다. 모든 관리 작업은 감사 로그(Audit Log)에 기록됩니다. */
@Service
public class AdminUserManagementUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final ObjectMapper objectMapper;

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
            ObjectMapper objectMapper) {
        this.userRepositoryPort = userRepositoryPort;
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.objectMapper = objectMapper;
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
        validateOrganizationReferences(
                command.affiliateId(),
                command.departmentId(),
                command.teamId(),
                command.positionId());
        ensureLoginIdIsUnique(command.loginId());
        ensureEmailIsUnique(command.email(), null);

        Instant now = Instant.now();
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

    /**
     * 회원 정보 수정 회원의 기본 정보를 수정합니다.
     *
     * @param command 회원 수정 명령
     * @return 수정된 회원 요약 정보
     */
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

    /**
     * 회원 상태 변경 회원의 상태를 변경하고 모든 세션을 만료시킵니다.
     *
     * @param command 상태 변경 명령
     * @return 상태가 변경된 회원 요약 정보
     */
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

    /** 회원 생성 명령 */
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

    /** 회원 검색 명령 */
    public record SearchCommand(String keyword, int page, int size) {}

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
}
