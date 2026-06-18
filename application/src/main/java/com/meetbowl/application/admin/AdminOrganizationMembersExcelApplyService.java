package com.meetbowl.application.admin;

import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.AffiliateRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.DepartmentRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.PositionRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.TeamRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.UserRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.WorkbookRows;
import com.meetbowl.application.admin.excel.OrganizationMembersExcelWorkbookMapper;
import com.meetbowl.application.user.UserSearchReindexRequestDispatcher;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.common.response.ErrorDetail;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.domain.user.UserStatus;

@Service
public class AdminOrganizationMembersExcelApplyService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher;
    private final OrganizationMembersExcelWorkbookMapper workbookMapper;

    public AdminOrganizationMembersExcelApplyService(
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher,
            OrganizationMembersExcelWorkbookMapper workbookMapper) {
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.userSearchReindexRequestDispatcher = userSearchReindexRequestDispatcher;
        this.workbookMapper = workbookMapper;
    }

    @Transactional
    public AdminOrganizationMembersExcelUseCase.ImportResult apply(
            AdminOrganizationMembersExcelUseCase.ImportCommand command) {
        WorkbookRows rows = workbookMapper.read(command.fileBytes());
        ImportContext context = new ImportContext();

        List<Affiliate> existingAffiliates = affiliateRepositoryPort.findAll();
        List<Department> existingDepartments = departmentRepositoryPort.findAll();
        List<Team> existingTeams = teamRepositoryPort.findAll();
        List<Position> existingPositions = positionRepositoryPort.findAll();
        List<User> existingUsers = userRepositoryPort.findAll();

        preloadExistingReferences(
                existingAffiliates, existingDepartments, existingTeams, existingPositions, context);

        // 시트 순서대로 검증/매핑해두면, 뒤 시트는 앞 시트에서 새로 정의한 조직 참조값까지 함께 검증할 수 있다.
        resolveAffiliates(rows.affiliates(), existingAffiliates, context);
        resolveDepartments(rows.departments(), existingDepartments, context);
        resolveTeams(rows.teams(), existingTeams, context);
        resolvePositions(rows.positions(), existingPositions, context);
        resolveUsers(rows.users(), existingUsers, context);

        if (!context.errors().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, context.errors());
        }

        saveAll(context);

        // 검색 인덱스는 DB 커밋이 끝난 뒤에만 전체 재색인을 요청해, consumer가 최종 반영 상태만 읽게 한다.
        userSearchReindexRequestDispatcher.publishAfterCommit(
                new UserSearchReindexRequestedEvent(
                        "ORGANIZATION_MEMBERS_EXCEL_IMPORTED",
                        true,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        command.adminId()));

        AdminOrganizationMembersExcelUseCase.ImportResult result =
                new AdminOrganizationMembersExcelUseCase.ImportResult(
                        context.createdAffiliates,
                        context.updatedAffiliates,
                        context.createdDepartments,
                        context.updatedDepartments,
                        context.createdTeams,
                        context.updatedTeams,
                        context.createdPositions,
                        context.updatedPositions,
                        context.createdUsers,
                        context.updatedUsers);
        saveSuccessAudit(command, result);
        return result;
    }

    private void resolveAffiliates(
            List<AffiliateRow> rows, List<Affiliate> existingAffiliates, ImportContext context) {
        Map<UUID, Affiliate> byId =
                existingAffiliates.stream()
                        .collect(
                                LinkedHashMap::new,
                                (map, item) -> map.put(item.id(), item),
                                Map::putAll);
        Map<String, Affiliate> byName = new LinkedHashMap<>();
        Map<String, Affiliate> byCode = new LinkedHashMap<>();
        existingAffiliates.forEach(
                affiliate -> {
                    byName.put(normalizeKey(affiliate.name()), affiliate);
                    byCode.put(normalizeKey(affiliate.code()), affiliate);
                });

        for (AffiliateRow row : rows) {
            String name =
                    required(row.affiliateName(), "계열사", row.rowNumber(), "affiliateName", context);
            String code =
                    required(row.affiliateCode(), "계열사", row.rowNumber(), "affiliateCode", context);
            ReferenceStatus status =
                    parseReferenceStatus(row.status(), "계열사", row.rowNumber(), context);
            if (name == null || code == null || status == null) {
                continue;
            }

            Affiliate target = null;
            UUID affiliateId =
                    parseUuid(row.affiliateId(), "계열사", row.rowNumber(), "affiliateId", context);
            if (affiliateId != null) {
                target = byId.get(affiliateId);
                if (target == null) {
                    addError("계열사", row.rowNumber(), "affiliateId", "존재하지 않는 계열사 ID입니다.", context);
                    continue;
                }
            } else {
                target = byName.get(normalizeKey(name));
            }

            String duplicateKey =
                    target == null ? "name:" + normalizeKey(name) : "id:" + target.id().toString();
            checkDuplicate(
                    duplicateKey,
                    "계열사",
                    row.rowNumber(),
                    "affiliateName",
                    context.seenAffiliateKeys(),
                    context);

            Affiliate candidate =
                    new Affiliate(
                            target == null ? UUID.randomUUID() : target.id(),
                            name,
                            code,
                            status,
                            target == null ? null : target.sortOrder(),
                            target == null ? Instant.now() : target.createdAt(),
                            Instant.now());

            validateAffiliateUniqueness(candidate, existingAffiliates, context, row.rowNumber());
            context.resolvedAffiliatesByName().put(normalizeKey(name), candidate);
            context.affiliatesToSave().put(candidate.id(), candidate);
            if (target == null) {
                context.createdAffiliates++;
            } else {
                context.updatedAffiliates++;
            }
        }
    }

    private void resolveDepartments(
            List<DepartmentRow> rows, List<Department> existingDepartments, ImportContext context) {
        Map<UUID, Department> byId =
                existingDepartments.stream()
                        .collect(
                                LinkedHashMap::new,
                                (map, item) -> map.put(item.id(), item),
                                Map::putAll);
        Map<String, Department> byHierarchy =
                new LinkedHashMap<>(context.resolvedDepartmentsByHierarchy());

        for (DepartmentRow row : rows) {
            String affiliateName =
                    required(row.affiliateName(), "부서", row.rowNumber(), "affiliateName", context);
            String departmentName =
                    required(
                            row.departmentName(), "부서", row.rowNumber(), "departmentName", context);
            String departmentCode =
                    required(
                            row.departmentCode(), "부서", row.rowNumber(), "departmentCode", context);
            Integer sortNumber = parseSortNumber(row.sortNumber(), "부서", row.rowNumber(), context);
            ReferenceStatus status =
                    parseReferenceStatus(row.status(), "부서", row.rowNumber(), context);
            if (affiliateName == null
                    || departmentName == null
                    || departmentCode == null
                    || sortNumber == null
                    || status == null) {
                continue;
            }

            Affiliate affiliate =
                    context.resolvedAffiliatesByName().get(normalizeKey(affiliateName));
            if (affiliate == null) {
                addError("부서", row.rowNumber(), "affiliateName", "존재하지 않는 계열사 참조입니다.", context);
                continue;
            }

            Department target = null;
            UUID departmentId =
                    parseUuid(row.departmentId(), "부서", row.rowNumber(), "departmentId", context);
            if (departmentId != null) {
                target = byId.get(departmentId);
                if (target == null) {
                    addError("부서", row.rowNumber(), "departmentId", "존재하지 않는 부서 ID입니다.", context);
                    continue;
                }
            } else {
                target = byHierarchy.get(hierarchyKey(affiliateName, departmentName));
            }

            String duplicateKey =
                    target == null
                            ? hierarchyKey(affiliateName, departmentName)
                            : "id:" + target.id().toString();
            checkDuplicate(
                    duplicateKey,
                    "부서",
                    row.rowNumber(),
                    "departmentName",
                    context.seenDepartmentKeys(),
                    context);

            Department candidate =
                    new Department(
                            target == null ? UUID.randomUUID() : target.id(),
                            affiliate.id(),
                            null,
                            departmentName,
                            departmentCode,
                            status,
                            sortNumber,
                            target == null ? Instant.now() : target.createdAt(),
                            Instant.now());

            validateDepartmentUniqueness(
                    candidate, existingDepartments, context, row.rowNumber(), affiliateName);
            context.resolvedDepartmentsByHierarchy()
                    .put(hierarchyKey(affiliateName, departmentName), candidate);
            context.departmentsToSave().put(candidate.id(), candidate);
            if (target == null) {
                context.createdDepartments++;
            } else {
                context.updatedDepartments++;
            }
        }
    }

    private void resolveTeams(List<TeamRow> rows, List<Team> existingTeams, ImportContext context) {
        Map<UUID, Team> byId =
                existingTeams.stream()
                        .collect(
                                LinkedHashMap::new,
                                (map, item) -> map.put(item.id(), item),
                                Map::putAll);
        Map<String, Team> byHierarchy = new LinkedHashMap<>(context.resolvedTeamsByHierarchy());

        for (TeamRow row : rows) {
            String affiliateName =
                    required(row.affiliateName(), "팀", row.rowNumber(), "affiliateName", context);
            String departmentName =
                    required(row.departmentName(), "팀", row.rowNumber(), "departmentName", context);
            String teamName = required(row.teamName(), "팀", row.rowNumber(), "teamName", context);
            String teamCode = required(row.teamCode(), "팀", row.rowNumber(), "teamCode", context);
            Integer sortNumber = parseSortNumber(row.sortNumber(), "팀", row.rowNumber(), context);
            ReferenceStatus status =
                    parseReferenceStatus(row.status(), "팀", row.rowNumber(), context);
            if (affiliateName == null
                    || departmentName == null
                    || teamName == null
                    || teamCode == null
                    || sortNumber == null
                    || status == null) {
                continue;
            }

            Department department =
                    context.resolvedDepartmentsByHierarchy()
                            .get(hierarchyKey(affiliateName, departmentName));
            if (department == null) {
                addError("팀", row.rowNumber(), "departmentName", "존재하지 않는 부서 참조입니다.", context);
                continue;
            }
            if (!Objects.equals(
                    department.affiliateId(),
                    context.resolvedAffiliatesByName().get(normalizeKey(affiliateName)).id())) {
                addError("팀", row.rowNumber(), "departmentName", "부서가 계열사 하위가 아닙니다.", context);
                continue;
            }

            Team target = null;
            UUID teamId = parseUuid(row.teamId(), "팀", row.rowNumber(), "teamId", context);
            if (teamId != null) {
                target = byId.get(teamId);
                if (target == null) {
                    addError("팀", row.rowNumber(), "teamId", "존재하지 않는 팀 ID입니다.", context);
                    continue;
                }
            } else {
                target = byHierarchy.get(teamKey(affiliateName, departmentName, teamName));
            }

            String duplicateKey =
                    target == null
                            ? teamKey(affiliateName, departmentName, teamName)
                            : "id:" + target.id().toString();
            checkDuplicate(
                    duplicateKey,
                    "팀",
                    row.rowNumber(),
                    "teamName",
                    context.seenTeamKeys(),
                    context);

            Team candidate =
                    new Team(
                            target == null ? UUID.randomUUID() : target.id(),
                            department.id(),
                            teamName,
                            teamCode,
                            status,
                            sortNumber,
                            target == null ? Instant.now() : target.createdAt(),
                            Instant.now());

            validateTeamUniqueness(
                    candidate,
                    existingTeams,
                    context,
                    row.rowNumber(),
                    affiliateName,
                    departmentName);
            context.resolvedTeamsByHierarchy()
                    .put(teamKey(affiliateName, departmentName, teamName), candidate);
            context.teamsToSave().put(candidate.id(), candidate);
            if (target == null) {
                context.createdTeams++;
            } else {
                context.updatedTeams++;
            }
        }
    }

    private void resolvePositions(
            List<PositionRow> rows, List<Position> existingPositions, ImportContext context) {
        Map<UUID, Position> byId =
                existingPositions.stream()
                        .collect(
                                LinkedHashMap::new,
                                (map, item) -> map.put(item.id(), item),
                                Map::putAll);
        Map<String, Position> byName = new LinkedHashMap<>();
        existingPositions.forEach(position -> byName.put(normalizeKey(position.name()), position));

        for (PositionRow row : rows) {
            String name =
                    required(row.positionName(), "직급", row.rowNumber(), "positionName", context);
            String code =
                    required(row.positionCode(), "직급", row.rowNumber(), "positionCode", context);
            Integer sortNumber = parseSortNumber(row.sortNumber(), "직급", row.rowNumber(), context);
            ReferenceStatus status =
                    parseReferenceStatus(row.status(), "직급", row.rowNumber(), context);
            if (name == null || code == null || sortNumber == null || status == null) {
                continue;
            }

            Position target = null;
            UUID positionId =
                    parseUuid(row.positionId(), "직급", row.rowNumber(), "positionId", context);
            if (positionId != null) {
                target = byId.get(positionId);
                if (target == null) {
                    addError("직급", row.rowNumber(), "positionId", "존재하지 않는 직급 ID입니다.", context);
                    continue;
                }
            } else {
                target = byName.get(normalizeKey(name));
            }

            String duplicateKey =
                    target == null ? "name:" + normalizeKey(name) : "id:" + target.id().toString();
            checkDuplicate(
                    duplicateKey,
                    "직급",
                    row.rowNumber(),
                    "positionName",
                    context.seenPositionKeys(),
                    context);

            Position candidate =
                    new Position(
                            target == null ? UUID.randomUUID() : target.id(),
                            name,
                            code,
                            status,
                            sortNumber,
                            target == null ? Instant.now() : target.createdAt(),
                            Instant.now());

            validatePositionUniqueness(candidate, existingPositions, context, row.rowNumber());
            context.resolvedPositionsByName().put(normalizeKey(name), candidate);
            context.positionsToSave().put(candidate.id(), candidate);
            if (target == null) {
                context.createdPositions++;
            } else {
                context.updatedPositions++;
            }
        }
    }

    private void resolveUsers(List<UserRow> rows, List<User> existingUsers, ImportContext context) {
        Map<UUID, User> byId =
                existingUsers.stream()
                        .collect(
                                LinkedHashMap::new,
                                (map, item) -> map.put(item.id(), item),
                                Map::putAll);
        Map<String, User> byLoginId = new LinkedHashMap<>();
        existingUsers.forEach(user -> byLoginId.put(normalizeKey(user.loginId()), user));

        for (UserRow row : rows) {
            String loginId = required(row.loginId(), "회원", row.rowNumber(), "loginId", context);
            String name = required(row.name(), "회원", row.rowNumber(), "name", context);
            String email = required(row.email(), "회원", row.rowNumber(), "email", context);
            UserRole role = parseRole(row.role(), row.rowNumber(), context);
            UserStatus status = parseUserStatus(row.status(), row.rowNumber(), context);
            if (loginId == null
                    || name == null
                    || email == null
                    || role == null
                    || status == null) {
                continue;
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                addError("회원", row.rowNumber(), "email", "이메일 형식이 올바르지 않습니다.", context);
                continue;
            }

            OrganizationRefs refs = resolveUserReferences(row, context);
            if (refs == null) {
                continue;
            }

            User loginMatched = byLoginId.get(normalizeKey(loginId));
            UUID userId = parseUuid(row.userId(), "회원", row.rowNumber(), "userId", context);
            User idMatched = userId == null ? null : byId.get(userId);
            if (userId != null && idMatched == null) {
                addError("회원", row.rowNumber(), "userId", "존재하지 않는 회원 ID입니다.", context);
                continue;
            }
            if (loginMatched != null
                    && idMatched != null
                    && !Objects.equals(loginMatched.id(), idMatched.id())) {
                addError(
                        "회원",
                        row.rowNumber(),
                        "loginId",
                        "loginId와 userId가 서로 다른 회원을 가리킵니다.",
                        context);
                continue;
            }
            if (idMatched != null
                    && !normalizeKey(idMatched.loginId()).equals(normalizeKey(loginId))) {
                addError(
                        "회원", row.rowNumber(), "loginId", "기존 회원의 loginId 변경은 지원하지 않습니다.", context);
                continue;
            }

            // 회원은 loginId를 우선 식별자로 본다. userId가 함께 온 경우에는 같은 회원인지 교차 검증만 수행한다.
            User target = loginMatched != null ? loginMatched : idMatched;
            if (target != null && target.role() == UserRole.SYSTEM) {
                addError("회원", row.rowNumber(), "role", "SYSTEM 계정은 엑셀로 수정할 수 없습니다.", context);
                continue;
            }

            checkDuplicate(
                    normalizeKey(loginId),
                    "회원",
                    row.rowNumber(),
                    "loginId",
                    context.seenUserLoginIds(),
                    context);

            User candidate =
                    target == null
                            ? User.of(
                                    UUID.randomUUID(),
                                    loginId,
                                    passwordEncoder.encode(PasswordPolicy.INITIAL_PASSWORD),
                                    name,
                                    email,
                                    role,
                                    status,
                                    refs.affiliateId(),
                                    refs.departmentId(),
                                    refs.positionId(),
                                    refs.teamId(),
                                    true,
                                    null,
                                    null,
                                    Instant.now(),
                                    Instant.now())
                            : User.of(
                                    target.id(),
                                    target.loginId(),
                                    target.passwordHash(),
                                    name,
                                    email,
                                    role,
                                    status,
                                    refs.affiliateId(),
                                    refs.departmentId(),
                                    refs.positionId(),
                                    refs.teamId(),
                                    target.initialPasswordChangeRequired(),
                                    target.activeFrom(),
                                    target.activeUntil(),
                                    target.createdAt(),
                                    Instant.now());

            validateUserUniqueness(candidate, target, existingUsers, context, row.rowNumber());
            context.usersToSave().put(candidate.id(), candidate);
            if (target == null) {
                context.createdUsers++;
            } else {
                context.updatedUsers++;
            }
        }
    }

    private OrganizationRefs resolveUserReferences(UserRow row, ImportContext context) {
        String affiliateName = blankToNull(row.affiliateName());
        String departmentName = blankToNull(row.departmentName());
        String teamName = blankToNull(row.teamName());
        String positionName = blankToNull(row.positionName());

        if (teamName != null && departmentName == null) {
            addError("회원", row.rowNumber(), "departmentName", "팀이 있으면 부서가 필요합니다.", context);
            return null;
        }
        if (departmentName != null && affiliateName == null) {
            addError("회원", row.rowNumber(), "affiliateName", "부서가 있으면 계열사가 필요합니다.", context);
            return null;
        }

        Affiliate affiliate =
                affiliateName == null
                        ? null
                        : context.resolvedAffiliatesByName().get(normalizeKey(affiliateName));
        if (affiliateName != null && affiliate == null) {
            addError("회원", row.rowNumber(), "affiliateName", "존재하지 않는 계열사 참조입니다.", context);
            return null;
        }

        Department department =
                departmentName == null
                        ? null
                        : context.resolvedDepartmentsByHierarchy()
                                .get(hierarchyKey(affiliateName, departmentName));
        if (departmentName != null && department == null) {
            addError("회원", row.rowNumber(), "departmentName", "존재하지 않는 부서 참조입니다.", context);
            return null;
        }
        if (department != null
                && affiliate != null
                && !Objects.equals(department.affiliateId(), affiliate.id())) {
            addError("회원", row.rowNumber(), "departmentName", "부서가 계열사 하위가 아닙니다.", context);
            return null;
        }

        Team team =
                teamName == null
                        ? null
                        : context.resolvedTeamsByHierarchy()
                                .get(teamKey(affiliateName, departmentName, teamName));
        if (teamName != null && team == null) {
            addError("회원", row.rowNumber(), "teamName", "존재하지 않는 팀 참조입니다.", context);
            return null;
        }
        if (team != null
                && department != null
                && !Objects.equals(team.departmentId(), department.id())) {
            addError("회원", row.rowNumber(), "teamName", "팀이 부서 하위가 아닙니다.", context);
            return null;
        }

        Position position =
                positionName == null
                        ? null
                        : context.resolvedPositionsByName().get(normalizeKey(positionName));
        if (positionName != null && position == null) {
            addError("회원", row.rowNumber(), "positionName", "존재하지 않는 직급 참조입니다.", context);
            return null;
        }

        return new OrganizationRefs(
                affiliate == null ? null : affiliate.id(),
                department == null ? null : department.id(),
                team == null ? null : team.id(),
                position == null ? null : position.id());
    }

    private void saveAll(ImportContext context) {
        // validation을 모두 통과한 뒤 한 트랜잭션 안에서만 저장해 all-or-nothing 정책을 지킨다.
        context.affiliatesToSave().values().forEach(affiliateRepositoryPort::save);
        context.departmentsToSave().values().forEach(departmentRepositoryPort::save);
        context.teamsToSave().values().forEach(teamRepositoryPort::save);
        context.positionsToSave().values().forEach(positionRepositoryPort::save);
        context.usersToSave().values().forEach(userRepositoryPort::save);
    }

    private void saveSuccessAudit(
            AdminOrganizationMembersExcelUseCase.ImportCommand command,
            AdminOrganizationMembersExcelUseCase.ImportResult result) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        command.adminId(),
                        command.adminName(),
                        "ORGANIZATION_MEMBER_EXCEL",
                        null,
                        "ORGANIZATION_MEMBER_EXCEL",
                        "IMPORT",
                        AuditResult.SUCCESS,
                        null,
                        toJson(new SuccessSnapshot(command.fileName(), result)),
                        command.ipAddress(),
                        command.userAgent(),
                        Instant.now()));
    }

    private void validateAffiliateUniqueness(
            Affiliate candidate, List<Affiliate> existing, ImportContext context, int rowNumber) {
        for (Affiliate affiliate : merge(existing, context.affiliatesToSave().values())) {
            if (affiliate.id().equals(candidate.id())) {
                continue;
            }
            if (normalizeKey(affiliate.name()).equals(normalizeKey(candidate.name()))) {
                addError("계열사", rowNumber, "affiliateName", "같은 계열사명이 이미 존재합니다.", context);
            }
            if (normalizeKey(affiliate.code()).equals(normalizeKey(candidate.code()))) {
                addError("계열사", rowNumber, "affiliateCode", "같은 계열사 코드가 이미 존재합니다.", context);
            }
        }
    }

    private void validateDepartmentUniqueness(
            Department candidate,
            List<Department> existing,
            ImportContext context,
            int rowNumber,
            String affiliateName) {
        for (Department department : merge(existing, context.departmentsToSave().values())) {
            if (department.id().equals(candidate.id())) {
                continue;
            }
            if (Objects.equals(department.affiliateId(), candidate.affiliateId())
                    && normalizeKey(department.name()).equals(normalizeKey(candidate.name()))) {
                addError("부서", rowNumber, "departmentName", "같은 계열사 내 부서명이 중복됩니다.", context);
            }
            if (normalizeKey(department.code()).equals(normalizeKey(candidate.code()))) {
                addError("부서", rowNumber, "departmentCode", "같은 부서 코드가 이미 존재합니다.", context);
            }
        }
        context.resolvedAffiliatesByName()
                .putIfAbsent(
                        normalizeKey(affiliateName),
                        context.affiliatesToSave().get(candidate.affiliateId()));
    }

    private void validateTeamUniqueness(
            Team candidate,
            List<Team> existing,
            ImportContext context,
            int rowNumber,
            String affiliateName,
            String departmentName) {
        for (Team team : merge(existing, context.teamsToSave().values())) {
            if (team.id().equals(candidate.id())) {
                continue;
            }
            if (Objects.equals(team.departmentId(), candidate.departmentId())
                    && normalizeKey(team.name()).equals(normalizeKey(candidate.name()))) {
                addError("팀", rowNumber, "teamName", "같은 부서 내 팀명이 중복됩니다.", context);
            }
            if (normalizeKey(team.code()).equals(normalizeKey(candidate.code()))) {
                addError("팀", rowNumber, "teamCode", "같은 팀 코드가 이미 존재합니다.", context);
            }
        }
        context.resolvedDepartmentsByHierarchy()
                .putIfAbsent(
                        hierarchyKey(affiliateName, departmentName),
                        context.departmentsToSave().get(candidate.departmentId()));
    }

    private void validatePositionUniqueness(
            Position candidate, List<Position> existing, ImportContext context, int rowNumber) {
        for (Position position : merge(existing, context.positionsToSave().values())) {
            if (position.id().equals(candidate.id())) {
                continue;
            }
            if (normalizeKey(position.name()).equals(normalizeKey(candidate.name()))) {
                addError("직급", rowNumber, "positionName", "같은 직급명이 이미 존재합니다.", context);
            }
            if (normalizeKey(position.code()).equals(normalizeKey(candidate.code()))) {
                addError("직급", rowNumber, "positionCode", "같은 직급 코드가 이미 존재합니다.", context);
            }
        }
    }

    private void validateUserUniqueness(
            User candidate,
            User target,
            List<User> existing,
            ImportContext context,
            int rowNumber) {
        for (User user : merge(existing, context.usersToSave().values())) {
            if (target != null && user.id().equals(target.id())) {
                continue;
            }
            if (normalizeKey(user.loginId()).equals(normalizeKey(candidate.loginId()))) {
                addError("회원", rowNumber, "loginId", "같은 loginId가 이미 존재합니다.", context);
            }
            if (normalizeKey(user.email()).equals(normalizeKey(candidate.email()))) {
                addError("회원", rowNumber, "email", "같은 이메일이 이미 존재합니다.", context);
            }
        }
    }

    private <T> List<T> merge(List<T> existing, Iterable<T> pending) {
        Map<Integer, T> merged = new LinkedHashMap<>();
        existing.forEach(item -> merged.put(System.identityHashCode(item), item));
        pending.forEach(item -> merged.put(System.identityHashCode(item), item));
        return new ArrayList<>(merged.values());
    }

    private String required(
            String value, String sheetName, int rowNumber, String field, ImportContext context) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            addError(sheetName, rowNumber, field, "필수값이 비어 있습니다.", context);
        }
        return trimmed;
    }

    private Integer parseSortNumber(
            String value, String sheetName, int rowNumber, ImportContext context) {
        // 부서/팀/직급 sortNumber는 도메인 sortOrder로 그대로 반영되므로 빈값과 숫자 여부를 여기서 강제한다.
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            addError(sheetName, rowNumber, "sortNumber", "필수값이 비어 있습니다.", context);
            return null;
        }
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException exception) {
            addError(sheetName, rowNumber, "sortNumber", "숫자만 입력할 수 있습니다.", context);
            return null;
        }
    }

    private UUID parseUuid(
            String value, String sheetName, int rowNumber, String field, ImportContext context) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException exception) {
            addError(sheetName, rowNumber, field, "UUID 형식이 올바르지 않습니다.", context);
            return null;
        }
    }

    private ReferenceStatus parseReferenceStatus(
            String value, String sheetName, int rowNumber, ImportContext context) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            addError(sheetName, rowNumber, "status", "필수값이 비어 있습니다.", context);
            return null;
        }
        try {
            return ReferenceStatus.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            addError(sheetName, rowNumber, "status", "허용값은 ACTIVE, INACTIVE 입니다.", context);
            return null;
        }
    }

    private UserStatus parseUserStatus(String value, int rowNumber, ImportContext context) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            addError("회원", rowNumber, "status", "필수값이 비어 있습니다.", context);
            return null;
        }
        try {
            UserStatus status = UserStatus.valueOf(trimmed.toUpperCase(Locale.ROOT));
            if (status == UserStatus.LOCKED) {
                throw new IllegalArgumentException("LOCKED");
            }
            return status;
        } catch (IllegalArgumentException exception) {
            addError("회원", rowNumber, "status", "허용값은 ACTIVE, INACTIVE 입니다.", context);
            return null;
        }
    }

    private UserRole parseRole(String value, int rowNumber, ImportContext context) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            addError("회원", rowNumber, "role", "필수값이 비어 있습니다.", context);
            return null;
        }
        try {
            UserRole role = UserRole.valueOf(trimmed.toUpperCase(Locale.ROOT));
            if (role != UserRole.ADMIN && role != UserRole.USER) {
                throw new IllegalArgumentException("unsupported");
            }
            return role;
        } catch (IllegalArgumentException exception) {
            addError("회원", rowNumber, "role", "허용값은 ADMIN, USER 입니다.", context);
            return null;
        }
    }

    private void checkDuplicate(
            String key,
            String sheetName,
            int rowNumber,
            String field,
            Map<String, Integer> seen,
            ImportContext context) {
        Integer existing = seen.putIfAbsent(key, rowNumber);
        if (existing != null) {
            addError(sheetName, rowNumber, field, "같은 엑셀 내 중복 행입니다.", context);
        }
    }

    private void addError(
            String sheetName,
            Integer rowNumber,
            String field,
            String reason,
            ImportContext context) {
        context.errors().add(new ErrorDetail(sheetName, rowNumber, field, reason));
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void preloadExistingReferences(
            List<Affiliate> affiliates,
            List<Department> departments,
            List<Team> teams,
            List<Position> positions,
            ImportContext context) {
        affiliates.forEach(
                affiliate ->
                        context.resolvedAffiliatesByName()
                                .put(normalizeKey(affiliate.name()), affiliate));
        departments.forEach(
                department -> {
                    Affiliate affiliate =
                            affiliates.stream()
                                    .filter(item -> item.id().equals(department.affiliateId()))
                                    .findFirst()
                                    .orElse(null);
                    if (affiliate != null) {
                        context.resolvedDepartmentsByHierarchy()
                                .put(hierarchyKey(affiliate.name(), department.name()), department);
                    }
                });
        teams.forEach(
                team -> {
                    Department department =
                            departments.stream()
                                    .filter(item -> item.id().equals(team.departmentId()))
                                    .findFirst()
                                    .orElse(null);
                    if (department == null) {
                        return;
                    }
                    Affiliate affiliate =
                            affiliates.stream()
                                    .filter(item -> item.id().equals(department.affiliateId()))
                                    .findFirst()
                                    .orElse(null);
                    if (affiliate != null) {
                        context.resolvedTeamsByHierarchy()
                                .put(
                                        teamKey(affiliate.name(), department.name(), team.name()),
                                        team);
                    }
                });
        positions.forEach(
                position ->
                        context.resolvedPositionsByName()
                                .put(normalizeKey(position.name()), position));
    }

    private String hierarchyKey(String affiliateName, String departmentName) {
        return normalizeKey(affiliateName) + "|" + normalizeKey(departmentName);
    }

    private String teamKey(String affiliateName, String departmentName, String teamName) {
        return hierarchyKey(affiliateName, departmentName) + "|" + normalizeKey(teamName);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR, "Failed to serialize admin audit snapshot.");
        }
    }

    private static final class ImportContext {
        private final List<ErrorDetail> errors = new ArrayList<>();
        private final Map<String, Integer> seenAffiliateKeys = new LinkedHashMap<>();
        private final Map<String, Integer> seenDepartmentKeys = new LinkedHashMap<>();
        private final Map<String, Integer> seenTeamKeys = new LinkedHashMap<>();
        private final Map<String, Integer> seenPositionKeys = new LinkedHashMap<>();
        private final Map<String, Integer> seenUserLoginIds = new LinkedHashMap<>();
        private final Map<UUID, Affiliate> affiliatesToSave = new LinkedHashMap<>();
        private final Map<UUID, Department> departmentsToSave = new LinkedHashMap<>();
        private final Map<UUID, Team> teamsToSave = new LinkedHashMap<>();
        private final Map<UUID, Position> positionsToSave = new LinkedHashMap<>();
        private final Map<UUID, User> usersToSave = new LinkedHashMap<>();
        private final Map<String, Affiliate> resolvedAffiliatesByName = new LinkedHashMap<>();
        private final Map<String, Department> resolvedDepartmentsByHierarchy =
                new LinkedHashMap<>();
        private final Map<String, Team> resolvedTeamsByHierarchy = new LinkedHashMap<>();
        private final Map<String, Position> resolvedPositionsByName = new LinkedHashMap<>();
        private int createdAffiliates;
        private int updatedAffiliates;
        private int createdDepartments;
        private int updatedDepartments;
        private int createdTeams;
        private int updatedTeams;
        private int createdPositions;
        private int updatedPositions;
        private int createdUsers;
        private int updatedUsers;

        private List<ErrorDetail> errors() {
            return errors;
        }

        private Map<String, Integer> seenAffiliateKeys() {
            return seenAffiliateKeys;
        }

        private Map<String, Integer> seenDepartmentKeys() {
            return seenDepartmentKeys;
        }

        private Map<String, Integer> seenTeamKeys() {
            return seenTeamKeys;
        }

        private Map<String, Integer> seenPositionKeys() {
            return seenPositionKeys;
        }

        private Map<String, Integer> seenUserLoginIds() {
            return seenUserLoginIds;
        }

        private Map<UUID, Affiliate> affiliatesToSave() {
            return affiliatesToSave;
        }

        private Map<UUID, Department> departmentsToSave() {
            return departmentsToSave;
        }

        private Map<UUID, Team> teamsToSave() {
            return teamsToSave;
        }

        private Map<UUID, Position> positionsToSave() {
            return positionsToSave;
        }

        private Map<UUID, User> usersToSave() {
            return usersToSave;
        }

        private Map<String, Affiliate> resolvedAffiliatesByName() {
            return resolvedAffiliatesByName;
        }

        private Map<String, Department> resolvedDepartmentsByHierarchy() {
            return resolvedDepartmentsByHierarchy;
        }

        private Map<String, Team> resolvedTeamsByHierarchy() {
            return resolvedTeamsByHierarchy;
        }

        private Map<String, Position> resolvedPositionsByName() {
            return resolvedPositionsByName;
        }
    }

    private record OrganizationRefs(
            UUID affiliateId, UUID departmentId, UUID teamId, UUID positionId) {}

    private record SuccessSnapshot(
            String fileName, AdminOrganizationMembersExcelUseCase.ImportResult result) {}
}
