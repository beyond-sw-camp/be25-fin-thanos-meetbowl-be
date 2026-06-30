2026-06-12 RBAC 메뉴 API 및 권한 체크 정리

- 작업 목적: 로그인 사용자 role 기반 메뉴 조회 API를 추가하고 `/api/v1/users/me/menus` 권한 경로를 USER/ADMIN 공용으로 정리한다.
- 변경 파일: `application/user` 메뉴 정책·UseCase 추가, `app-api/user` 메뉴 응답 DTO 추가, `UserMeController`, `SecurityConfig`, 관련 테스트 수정.
- 동작 변경:
  `GET /api/v1/users/me/menus`가 현재 인증 사용자의 `@CurrentUser AuthenticatedUser` role 기준으로 정렬된 메뉴 목록을 반환한다.
  `/api/v1/users/me/menus`는 USER와 ADMIN 모두 접근 가능하고, `/api/v1/admin/**`는 기존처럼 ADMIN만 접근 가능하다.
- 검증 방법: `./gradlew.bat spotlessApply`, `./gradlew.bat check`, `git status --short`
- 후속 참고: 메뉴 경로는 프론트 실제 라우트 확장 전까지 코드 기반 정적 정책으로 유지한다.

2026-06-12 Admin audit log query API

- Purpose: add ADMIN-only list/detail APIs for shared AdminAuditLog history with filters and paging.
- Changed files: added audit log query use case/result/command in `application/admin`, search condition and port extension in `domain/admin`, JPA specification filtering in `infrastructure/persistence/admin`, API controller/DTO/tests in `app-api/admin`.
- Behavior: `GET /api/v1/admin/audit-logs` supports actorUserId, actorLoginId, actionType, targetType, targetId, result, from, to, page, size and sorts newest first. `GET /api/v1/admin/audit-logs/{auditLogId}` returns detail with beforeSnapshot/afterSnapshot.
- Notes: reused existing AdminAuditLog table/model. Existing internal `FAILURE` is mapped to API `FAILED`; API `FAILED` filter maps back to internal `FAILURE`.
- Extra fix for verification: removed a JPQL inline `--` comment from `SpringDataUserRepository` and mapped unsupported method routing to common 404 so existing Spring Boot context/security tests pass.
- Verification: `./gradlew.bat spotlessApply`, `./gradlew.bat check`, `git status --short`.

2026-06-12 Admin mail retention policy API

- Purpose: add ADMIN-only mail data retention policy read/update APIs for the system-wide policy setting.
- Changed files: added `MailRetentionPolicyUseCase`, command/result records, API controller/DTOs, and controller/application tests.
- Behavior: `GET /api/v1/admin/mail/retention-policy` returns the current policy or an unsaved default. `PATCH /api/v1/admin/mail/retention-policy` stores a single system policy row and records an AdminAuditLog entry.
- Storage: reused existing `MailRetentionPolicy`, `MailRetentionPolicyRepositoryPort`, JPA entity, and persistence adapter. The API-level `retentionDays` value is saved into the existing inbox/sent/trash retention day fields.
- Validation: retentionDays must be 1..3650. `autoDeleteEnabled=false` means automatic deletion is disabled; no deletion batch or scheduler was added.
- Audit: targetType/actionArea `MAIL_RETENTION_POLICY`, actionName `UPDATE`, and before/after snapshots serialized with ObjectMapper.
- Verification: `./gradlew.bat spotlessApply`, `./gradlew.bat check`, `git status --short`.

2026-06-13 Admin dashboard summary API

- Purpose: add the ADMIN-only read-only summary API for the admin dashboard at `GET /api/v1/admin/dashboard/summary`.
- Changed files: added `AdminDashboardSummaryUseCase` and summary result records in `application/admin`, added a read-only meeting dashboard query method to `MeetingRepositoryPort` and its JPA adapter/repository, added `AdminDashboardSummaryController` and response DTO in `app-api/admin`, added controller/use case tests, and updated `app-api` test profile to disable Rabbit listener auto startup during tests.
- Behavior:
  `recentAuditLogs` reuses the existing `AdminAuditLogQueryUseCase` with page 1 / size 5 and returns only `auditLogId`, `actorName`, `actionType`, `targetType`, `targetId`, `result`, `createdAt`.
  `mailRetentionPolicy` reuses `MailRetentionPolicyUseCase.get()` and preserves the existing default-policy fallback when no row exists.
  `meetingRoomSummary` uses current UTC time to derive current room status via `GetMeetingRoomStatusUseCase` and uses non-cancelled room meetings overlapping today from `MeetingRepositoryPort`.
- Calculation details:
  `todayReservationCount` counts meetings with a meeting room whose `scheduledAt` is within today 00:00 UTC inclusive and tomorrow 00:00 UTC exclusive, excluding `CANCELLED`.
  `inUseMeetingRoomCount` counts current room statuses equal to `IN_USE`.
  `availableMeetingRoomCount` counts current room statuses equal to `AVAILABLE`.
  `timeSlotUsage` returns 24 one-hour UTC slots for today and counts meetings overlapping each slot.
  `siteBuildingUsage` groups current room statuses by site/building and returns `totalRooms`, `usedRooms` (`IN_USE` only), and `usageRate = usedRooms / totalRooms`.
- Security: controller uses `@CurrentUser AuthenticatedUser` and `GlobalPermissionChecker.requireAdmin`, so `USER` receives `COMMON_FORBIDDEN` / HTTP 403.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed targeted tests `:application:test --tests "com.meetbowl.application.admin.AdminDashboardSummaryUseCaseTest"` and `:app-api:test --tests "com.meetbowl.api.admin.AdminDashboardSummaryControllerTest"`
  Initial `./gradlew.bat check` exposed an existing ArchUnit failure in `infrastructure` for `RabbitMinutesGeneratedEventListener` depending on `application.minutes.*`

2026-06-13 Rabbit minutes listener ArchUnit fix

- Purpose: make `./gradlew.bat check` pass without broad refactoring after confirming the ArchUnit violation already existed relative to `dev`.
- Cause: `infrastructure.messaging.minutes.RabbitMinutesGeneratedEventListener` directly depended on `application.minutes.SyncGeneratedMinutesUseCase` and `SyncGeneratedMinutesCommand`, violating the rule that `infrastructure` must not depend on `application`.
- Branch check: `git diff dev...HEAD -- infrastructure/src/main/java/com/meetbowl/infrastructure/messaging/minutes/RabbitMinutesGeneratedEventListener.java` returned no diff before the fix, so the violation was not introduced by the dashboard-summary implementation.
- Minimal fix: moved the minutes-generated Rabbit listener and its message DTO into `app-api/minutes`, where inbound adapter wiring to application use cases is allowed by the module direction, added `spring-boot-starter-amqp` to `app-api`, moved the listener unit test into `app-api`, and removed the old `infrastructure` listener/test files.
- Additional test fix: kept `spring.rabbitmq.listener.simple.auto-startup=false` in `app-api` test properties so Spring Boot context tests do not require a live RabbitMQ broker.
- Verification: `./gradlew.bat spotlessApply`, `./gradlew.bat check`

2026-06-15 Minutes review reminder setting field

- Purpose: add a backend personal-setting field for the unreviewed meeting minutes reminder interval so `GET/PATCH /api/v1/users/me/settings` can read and save it.
- Changed files: updated settings API DTOs/controller in `app-api/user`, settings use case/result in `application/user`, domain validation/defaults in `domain/user/UserSetting`, persistence mapping in `infrastructure/persistence/user/UserSettingEntity`, related tests in `app-api`, `application`, `domain`, and the API document in `docs/api-spec.md`.
- Behavior:
  `minutesReviewReminderMinutes` is stored as an integer minute value.
  Allowed values are `60`, `120`, `180`, `240`.
  The default value for a user without a settings row is `60`.
  Invalid values are rejected by the domain with `COMMON_INVALID_REQUEST`.
- Notes:
  Added concise Korean comments around the default-settings fallback and the domain-backed save path so the new field intent is easier to follow in code review.
  During verification, this branch also contained unrelated compile/test breakages in meeting/transcript modules, so a few existing files outside the settings flow were repaired only to reduce noise while checking the branch state.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :domain:test --tests "com.meetbowl.domain.user.UserSettingTest"`
  Passed `./gradlew.bat :app-api:test --tests "com.meetbowl.api.user.UserMeControllerTest"`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.user.MySettingsUseCaseTest"` and `./gradlew.bat check` are still blocked by unrelated existing compile failures in `application/meeting` and `application/transcript` tests on the current branch

2026-06-15 Remove auto backup time from my settings

- Purpose: remove the `autoBackupTime` personal-setting field from backend code so my-settings APIs only handle meeting reminder minutes, minutes-review reminder minutes, and auto-backup enabled status.
- Changed files: removed `autoBackupTime` from `app-api/user` request/response DTOs and controller command mapping, removed it from `application/user` result/command/use case, removed it from `domain/user/UserSetting` and its validation/default handling, removed the JPA field/mapping from `infrastructure/persistence/user/UserSettingEntity`, updated related tests in `app-api`, `application`, `domain`, and updated `docs/api-spec.md`.
- Behavior:
  `GET /api/v1/users/me/settings` no longer returns `autoBackupTime`.
  `PATCH /api/v1/users/me/settings` no longer accepts `autoBackupTime`.
  Auto-backup settings now persist only the boolean `autoBackupEnabled`.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :domain:test --tests "com.meetbowl.domain.user.UserSettingTest"`
  Passed `./gradlew.bat :app-api:test --tests "com.meetbowl.api.user.UserMeControllerTest"`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.user.MySettingsUseCaseTest"` is still blocked by unrelated existing compile failures in `application/meeting` and `application/transcript` tests on the current branch

2026-06-15 Remove auto backup setting from my settings

- Purpose: remove the `autoBackupEnabled` setting too, so backend my-settings APIs only handle meeting start reminder minutes and minutes-review reminder minutes.
- Changed files: removed `autoBackupEnabled` from `app-api/user` request/response DTOs and controller command mapping, removed it from `application/user` result/command/use case, removed it from `domain/user/UserSetting`, removed the JPA field/mapping from `infrastructure/persistence/user/UserSettingEntity`, updated related tests in `app-api`, `application`, `domain`, and updated `docs/api-spec.md`.
- Behavior:
  `GET /api/v1/users/me/settings` now returns only `meetingStartReminderMinutes` and `minutesReviewReminderMinutes`.
  `PATCH /api/v1/users/me/settings` now accepts only `meetingStartReminderMinutes` and `minutesReviewReminderMinutes`.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :domain:test --tests "com.meetbowl.domain.user.UserSettingTest"`
  `./gradlew.bat :app-api:test --tests "com.meetbowl.api.user.UserMeControllerTest"` and `./gradlew.bat :application:test --tests "com.meetbowl.application.user.MySettingsUseCaseTest"` are currently blocked by broader existing compile failures in the `application` module on the current branch

2026-06-15 Local organization master seed reinforcement

- Purpose: reinforce the `local` profile bootstrap data so admin organization/member screens have stable affiliate/department/team/position options without touching non-local profiles.
- Changed files: expanded `application/auth/InitializeLocalAccountsUseCase` to upsert Hanwha Systems organization master data and assign the existing `admin`, `user1`, `user2` accounts; updated `InitializeLocalAccountsUseCaseTest` to cover initial creation plus idempotent reuse/update behavior.
- Behavior:
  Creates or updates the local affiliate `한화시스템`, 3 departments, 6 teams, and 7 positions as `ACTIVE` with the requested codes and sort orders.
  Reuses existing rows by matching `name` or `code` in-scope, then saves the target values back onto the same IDs so repeated runs do not create duplicates.
  Keeps existing local users' login IDs, passwords, names, emails, and roles, while updating their affiliate/department/team/position assignments to the requested defaults.
  Creates missing `admin`, `user1`, `user2` accounts with password `1234` only when they do not already exist.
- Verification:
  Pending during implementation: `./gradlew.bat spotlessApply`
  Pending during implementation: targeted `InitializeLocalAccountsUseCaseTest`
  Pending during implementation: `./gradlew.bat check`

2026-06-15 Local organization master seed reinforcement update

- Verification correction:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :application:compileJava :app-api:compileJava`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.auth.InitializeLocalAccountsUseCaseTest"` is currently blocked by unrelated existing `application/meeting` and `application/transcript` test compile failures on this branch.
  `./gradlew.bat check` was not re-run because the same existing `application` test compile failures would block it.
  `./gradlew.bat :app-api:bootRun --args="--spring.profiles.active=local"` reached DB initialization with the `local` profile, but startup on this machine is currently blocked by another process already using port `8080`.

2026-06-15 Korean comment pass for in-flight branch changes

- Purpose: add concise Korean comments to the current branch's modified backend/frontend files so teammates can follow the intent of the new organization master-data and admin organization screen logic more quickly.
- Changed files: added Korean code comments to `application/auth/InitializeLocalAccountsUseCase`, `InitializeLocalAccountsUseCaseTest`, `be25-fin-thanos-meetbowl-fe/src/lib/admin-organizations.js`, `src/lib/admin-users.js`, `src/pages/admin/OrganizationPage.vue`, `test/admin-organizations.test.js`, and `test/admin-users.test.js`.
- Behavior:
  No runtime behavior changed.
  Comments now explain why local seed bootstrap reuses existing affiliate IDs, why user org assignments are updated idempotently, why the admin organization screen fetches a single snapshot of reference data, and why the frontend bypasses the global forbidden handler on these APIs.
- Verification:
  Not run. This pass only added comments and updated the work log.

2026-06-17 Admin audit log IP response and display

- Purpose: expose the stored admin audit log IP address through the admin audit log APIs and show it in the frontend admin logs list/detail UI.
- Changed files:
  Backend: `app-api/common/BaseController`, `app-api/admin/AdminUserController`, `app-api/admin/MailRetentionPolicyController`, `app-api/admin/dto/AdminAuditLogResponse`, `application/admin/AdminAuditLogResult`, related controller tests, and `application/admin/AdminDashboardSummaryUseCaseTest`.
  Frontend: `be25-fin-thanos-meetbowl-fe/src/pages/admin/AdminLogsPage.vue`.
- Behavior:
  `GET /api/v1/admin/audit-logs` and `GET /api/v1/admin/audit-logs/{auditLogId}` now include `ipAddress` in each audit log response item/detail.
  Admin user-management and mail-retention-policy write APIs now resolve client IP in this order: `X-Forwarded-For` first item, then `X-Real-IP`, then servlet remote address.
  Frontend admin logs list/detail now render `ipAddress` directly from the backend response and fall back to `-` when it is missing.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed targeted backend tests `:app-api:test --tests "com.meetbowl.api.admin.AdminAuditLogControllerTest" --tests "com.meetbowl.api.admin.AdminUserControllerTest" --tests "com.meetbowl.api.admin.MailRetentionPolicyControllerTest"`
  `./gradlew.bat check` is still blocked by pre-existing `application` module test compile failures in `meeting` and `transcript` tests on this branch; this run also needed the new `AdminDashboardSummaryUseCaseTest` constructor update for the added `ipAddress` field.

2026-06-17 Admin audit log IP filter and frontend log UX refinement

- Purpose: make admin audit logs easier to trace by IP address, replace free-text action/target filters with controlled selections, and reorganize the detail modal around human-readable change summaries.
- Changed files:
  Backend: `app-api/admin/AdminAuditLogController`, `application/admin/AdminAuditLogSearchCommand`, `application/admin/AdminAuditLogQueryUseCase`, `application/admin/AdminDashboardSummaryUseCase`, `domain/admin/AdminAuditLogSearchCondition`, `infrastructure/persistence/admin/JpaAdminAuditLogRepositoryAdapter`, related controller/application/JPA tests.
  Notes: the stored `ipAddress` response field and request-IP extraction order were already present and were reused as-is.
- Behavior:
  `GET /api/v1/admin/audit-logs` now accepts optional `ipAddress` and performs partial matching against the stored audit-log IP.
  Existing audit log detail/list responses continue returning `ipAddress` unchanged.
  No backend field names or routes changed.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :app-api:test --tests "com.meetbowl.api.admin.AdminAuditLogControllerTest"`
  Passed `./gradlew.bat :infrastructure:test --tests "com.meetbowl.infrastructure.persistence.admin.JpaAdminAuditLogRepositoryAdapterTest"`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.admin.AdminAuditLogQueryUseCaseTest"` is blocked by pre-existing `application` test compile failures in `meeting` and `transcript` test sources on the current branch.
  `./gradlew.bat check` fails for the same pre-existing `application:compileTestJava` blockers and additionally emitted a Gradle `problems-report.html` move collision after the compile failure.

2026-06-17 Admin organization audit log IP coverage

- Purpose: ensure organization master-data admin actions also persist the operator IP in admin audit logs so shared admin accounts remain traceable by source IP.
- Changed files: `app-api/admin/AdminOrganizationController`, `application/admin/AdminOrganizationMasterDataUseCase`, `app-api/admin/AdminOrganizationControllerTest`, `application/admin/AdminOrganizationMasterDataUseCaseTest`.
- Behavior:
  Affiliate, Department, Team, and Position create/update/status-change APIs now pass `extractClientIp(request)` and `User-Agent` into the use case.
  The organization master-data use case now saves `AdminAuditLog` entries for those write actions with target types `AFFILIATE`, `DEPARTMENT`, `TEAM`, `POSITION`, action names `CREATE` / `UPDATE` / `STATUS_CHANGE`, and JSON before/after snapshots.
  Existing admin audit log list filtering by `ipAddress` and partial IP matching was already present and was reused unchanged.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :application:compileJava :app-api:test --tests "com.meetbowl.api.admin.AdminOrganizationControllerTest" --console plain`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.admin.AdminOrganizationMasterDataUseCaseTest"` is still blocked by pre-existing `application:compileTestJava` failures in unrelated `meeting` / `transcript` tests on the current branch.

2026-06-17 Meeting room admin audit log IP coverage

- Purpose: fix the remaining admin write flows where `ipAddress` could not appear because meeting site/building/room management did not create admin audit logs at all.
- Changed files: `app-api/admin/MeetingSiteAdminController`, `MeetingBuildingAdminController`, `MeetingRoomAdminController`, `application/meetingroom/SiteAdminUseCase`, `BuildingAdminUseCase`, `MeetingRoomAdminUseCase`, `CreateMeetingRoomCommand`, `UpdateMeetingRoomCommand`, related meetingroom application tests, and `infrastructure/persistence/admin/JpaAdminAuditLogRepositoryAdapter`.
- Behavior:
  Meeting site/building/room create/update/delete and room availability change now pass `extractClientIp(request)` and `User-Agent` into the application layer.
  Those use cases now save `AdminAuditLog` rows with target types `MEETING_SITE`, `MEETING_BUILDING`, `MEETING_ROOM`, so new admin logs from those screens can show operator IPs.
  Existing old audit log rows are unchanged and may still display `-` when `ipAddress` was never stored at creation time.
- Verification:
  Passed `./gradlew.bat spotlessApply :application:compileJava :app-api:compileJava --console plain`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.meetingroom.*"` was not runnable in isolation because the current branch still has unrelated `application:compileTestJava` blockers in `meeting` / `transcript` tests.

2026-06-17 Account password policy hardening

- Purpose: fix the admin-created account password policy so the initial password is always `1234`, expose password-change-required state in the existing login response, add a self password change API, and keep admin reset/seed behavior aligned with the backend contract.
- Changed files:
  `application/admin/AdminUserManagementUseCase`, `application/admin/ResetUserPasswordUseCase`, new `application/admin/PasswordPolicy`, new `application/user/ChangeMyPasswordCommand`, new `application/user/ChangeMyPasswordUseCase`, `domain/user/User`, `app-api/user/UserMeController`, new `app-api/user/dto/MyPasswordChangeRequest`, `app-api/config/SecurityConfig`, `docs/api-spec.md`, related admin/auth/user/domain tests, and this log.
- Behavior:
  Admin-created users now always receive initial password `1234`, but only the BCrypt hash is stored in DB and `initialPasswordChangeRequired` is saved as `true`.
  Existing login response shape is preserved and continues exposing the password-change-required flag as `data.user.initialPasswordChangeRequired`.
  Added `PATCH /api/v1/users/me/password` for authenticated USER/ADMIN accounts. It validates current password, validates new-password confirmation, stores the encoded new password, and clears `initialPasswordChangeRequired`.
  Existing admin reset API `POST /api/v1/admin/users/{userId}/password/reset` is reused. It now always resets the target USER password to `1234`, keeps plaintext out of audit logs, and marks `initialPasswordChangeRequired` as `true`.
  Local seed accounts `admin`, `user1`, `user2` still use `1234` and still do not get forced into initial-password-change mode.
- Verification:
  Pending local execution after code update: `./gradlew.bat spotlessApply`
  Pending local execution after code update: related controller/use-case tests for admin user creation/reset, login, and `users/me/password`

2026-06-17 Initial password change login 403 fix

- Purpose: fix the regression where a user reset to the initial password could log in successfully but was blocked by `403 Forbidden` before reaching the forced password-change screen.
- Changed files: `app-api/config/SecurityConfig`, and this log.
- Behavior:
  Tokens with `initialPasswordChangeRequired=true` still cannot access general application APIs.
  They are now allowed to call only `GET /api/v1/users/me` and `PATCH /api/v1/users/me/password`, which are the minimum endpoints the frontend needs to load the logged-in profile and complete the forced password change flow.
  This keeps the backend restriction narrow while allowing `user3 / 1234` style initial-password logins to proceed to the password-change screen as intended.
- Verification:
  Reproduced before fix: `POST /api/v1/auth/login` for `user3 / 1234` succeeded, but `GET /api/v1/users/me` returned `403`.
  Pending after restart: re-run `user3 / 1234` login and forced password change flow through the frontend.

2026-06-18 User partial search API improvement

2026-06-23 Actions test failure triage and test expectation fix

- 작업 목적: actions 파이프라인에서 보고된 `UserDirectoryUseCaseTest > searchByOrganizationFiltersSuccess()` 실패 원인을 확인하고, 현재 구현과 어긋난 테스트 기대값을 정리한다.
- 변경 파일: `application/src/test/java/com/meetbowl/application/user/UserDirectoryUseCaseTest.java`, `task/agent-work-log.md`
- 원인 분석:
  `UserDirectoryUseCase.search()`는 `status`가 없으면 `ACTIVE`를 기본값으로 사용하고, `status`가 전달되면 해당 상태로 정확히 필터링한다.
  문제 테스트는 `affiliate/department/team/position + status="ACTIVE"` 조건으로 검색하면서도 같은 조직의 `INACTIVE` 사용자까지 포함된 `2건`을 기대하고 있었다.
  fixture 상 같은 조직 사용자는 `ACTIVE 1명`, `INACTIVE 1명`이므로 실제 결과는 `1건`이 맞다.
- 동작 변경:
  프로덕션 코드는 변경하지 않았다.
  테스트 기대값을 `2 -> 1`로 수정하고, 왜 비활성 사용자가 제외되는지 한글 주석을 추가했다.
- 제외 범위:
  `AdminDashboardSummaryUseCaseTest`의 CI 이력상 Mockito `PotentialStubbingProblem`은 현재 워킹트리에서는 재현되지 않아 코드 수정 대상에서 제외했다.
- 검증 방법과 결과:
  실행: `./gradlew :application:test --tests 'com.meetbowl.application.user.UserDirectoryUseCaseTest'`
  결과: 통과
  추가 확인: `./gradlew :application:test --tests 'com.meetbowl.application.admin.AdminDashboardSummaryUseCaseTest'`
  결과: 통과
- 남은 작업:
  CI가 실제로 어떤 SHA를 실행했는지 필요하면 workflow run의 checkout commit과 로컬 HEAD를 대조해 admin 테스트의 과거 실패 원인을 추가 확인해야 한다.

- Purpose: expand backend user search so admin/user directory APIs support trimmed, case-insensitive partial matches across user identity fields and organization names, while keeping existing API paths and DTOs unchanged.
- Changed files:
  `application/admin/AdminUserManagementUseCase`, `infrastructure/persistence/user/SpringDataUserRepository`, `app-api/admin/AdminUserControllerTest`, `app-api/user/UserDirectoryControllerTest`, `application/admin/AdminUserManagementUseCaseTest`, `application/user/UserDirectoryUseCaseTest`, new `infrastructure/persistence/user/JpaUserRepositoryAdapterTest`, and this log.
- Behavior:
  `GET /api/v1/admin/users` still uses the existing `keyword` parameter, but the keyword is now trimmed before querying.
  Admin user search now matches partial text in `name`, `email`, `loginId`, `affiliate`, `department`, `team`, `position`, and role labels for `ADMIN` / `USER` plus Korean aliases `관리자` / `일반 사용자`.
  `GET /api/v1/users/search` keeps the existing `keyword` parameter and now matches partial text in `name`, `email`, `loginId`, `affiliate`, `department`, `team`, and `position` without changing the response DTO.
  Both queries use case-insensitive `LIKE %keyword%` matching and guard nullable organization fields with `left join` + `coalesce` so null affiliations do not break search.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :app-api:compileJava :application:compileJava :infrastructure:compileJava`
  `./gradlew.bat :app-api:test --tests "com.meetbowl.api.admin.AdminUserControllerTest" --tests "com.meetbowl.api.user.UserDirectoryControllerTest"` is currently blocked by a pre-existing missing `app-api/src/main/java/com/meetbowl/api/config/SecurityConfig.java` dependency in `RequireAdminTest`.
 `./gradlew.bat :application:test --tests "com.meetbowl.application.admin.AdminUserManagementUseCaseTest" --tests "com.meetbowl.application.user.UserDirectoryUseCaseTest"` is currently blocked by pre-existing unrelated `meeting` / `transcript` test compile errors.
  `./gradlew.bat :infrastructure:test --tests "com.meetbowl.infrastructure.persistence.user.JpaUserRepositoryAdapterTest"` is currently blocked by pre-existing unrelated `RabbitDocumentIndexRequestedEventPublisherTest` compile errors.

2026-06-18 Elasticsearch-based user search migration

- Purpose: replace the admin/user member search path with an Elasticsearch-backed search structure while keeping the existing API paths, `keyword` parameter, DTO contracts, and DB search fallback behavior intact.
- Changed files:
  `app-api/admin/AdminUserController`, new `app-api/admin/dto/AdminUserSearchReindexResponse`, `app-api/resources/application.properties`, `app-api/resources/application-local.properties`,
  `application/admin/AdminUserManagementUseCase`, `application/admin/AdminOrganizationMasterDataUseCase`, new `application/admin/AdminUserSearchIndexUseCase`, `application/user/MyProfileUseCase`,
  `domain/user/UserRepositoryPort`, new `domain/user/UserSearchIndexPort`,
  `infrastructure/build.gradle`, `infrastructure/messaging/RabbitEventPublisher`, `infrastructure/persistence/user/JpaUserRepositoryAdapter`, `infrastructure/persistence/user/SpringDataUserRepository`, new `infrastructure/persistence/user/UserSearchSourceRow`,
  new `infrastructure/config/ElasticsearchUserSearchConfig`, new `infrastructure/config/ElasticsearchUserSearchProperties`,
  new `infrastructure/search/user/ElasticsearchUserSearchAdapter`, new `infrastructure/search/user/UserSearchIndexInitializer`, new `infrastructure/resources/elasticsearch/user-search-index.json`,
  related admin/user/infrastructure tests, `../be25-fin-thanos-meetbowl-infra/docker-compose.yml`, `../be25-fin-thanos-meetbowl-infra/.env.example`, and this log.
- Behavior:
  `GET /api/v1/admin/users` and `GET /api/v1/users/search` still accept the existing `keyword` parameter and keep their current response DTOs.
  Search now prefers Elasticsearch index `meetbowl-users` and queries `name`, `email`, `loginId`, `affiliateName`, `departmentName`, `teamName`, `positionName`, plus `role` / `roleLabel` for admin search.
  The index uses a lowercase `edge_ngram` analyzer for prefix-style suggestion search and supplements it with wildcard clauses so complete Hangul syllables and mixed English/number partial text still match quickly.
  Admin role search accepts both `ADMIN` / `USER` and Korean aliases `관리자` / `일반 사용자`.
  If Elasticsearch search fails or the index is unavailable, the repository adapter logs the cause and falls back to the previous DB `LIKE` query so the API does not fail closed.
  User create/update/status-change flows and profile name/email update now trigger best-effort document sync, and organization/position display-name updates trigger related-user reindexing.
  Added admin-only reindex endpoint `POST /api/v1/admin/users/search-index/reindex` so local/dev environments can rebuild the user index from DB data without exposing a public initializer.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat --no-problems-report :infrastructure:compileJava :application:compileJava :app-api:compileJava`
  Passed `./gradlew.bat --no-problems-report :app-api:test --tests "com.meetbowl.api.admin.AdminUserControllerTest" --tests "com.meetbowl.api.user.UserDirectoryControllerTest"`
  `./gradlew.bat --no-problems-report :application:test --tests "com.meetbowl.application.admin.AdminUserManagementUseCaseTest" --tests "com.meetbowl.application.admin.AdminOrganizationMasterDataUseCaseTest" --tests "com.meetbowl.application.user.UserDirectoryUseCaseTest" --tests "com.meetbowl.application.user.MyProfileUseCaseTest"` is blocked by pre-existing unrelated `application` test compile failures in `meeting` / `transcript` / `minutes` tests.
  `./gradlew.bat --no-problems-report :infrastructure:test --tests "com.meetbowl.infrastructure.persistence.user.JpaUserRepositoryAdapterTest"` is blocked by pre-existing unrelated `RabbitEventPublisherTest` / `RabbitDocumentIndexRequestedEventPublisherTest` compile failures.

2026-06-18 Elasticsearch user search Korean comments

- Purpose: add concise Korean comments to the Elasticsearch member search code so PR reviewers can understand the query structure, fallback path, and reindex timing faster.
- Changed files:
  `application/admin/AdminUserManagementUseCase`, `infrastructure/persistence/user/JpaUserRepositoryAdapter`, `infrastructure/search/user/ElasticsearchUserSearchAdapter`, and this log.
- Behavior:
  No runtime behavior change.
  Added Korean comments around ES page/size request construction, ADMIN/USER role filtering, `edge_ngram` + `wildcard` query intent, DB fallback behavior, ES result reordering, and immediate reindex on admin update/status change.
- Verification:
  No additional verification run by request. This change is comments-only.

2026-06-18 RabbitMQ async user-search reindex refactor

- Purpose: decouple member-search Elasticsearch reindexing from organization/member/profile write APIs so DB commits finish first and reindex work is retried through RabbitMQ instead of blocking synchronous API responses.
- Changed files:
  `../docs/event-contract.md`,
  `app-api/admin/AdminOrganizationController`,
  `app-api/messaging/RabbitMqMessagingConfig`,
  new `app-api/messaging/UserSearchReindexRequestedListener`,
  `application/admin/AdminOrganizationMasterDataUseCase`,
  `application/admin/AdminUserManagementUseCase`,
  `application/user/MyProfileUseCase`,
  new `application/user/UserSearchReindexRequestDispatcher`,
  new `application/user/UserSearchReindexUseCase`,
  new `common/event/user/UserSearchReindexRequestedMessage`,
  `common/event/EventTypes`,
  new `domain/user/UserSearchReindexEventPublisherPort`,
  new `domain/user/UserSearchReindexRequestedEvent`,
  new `infrastructure/messaging/user/RabbitUserSearchReindexEventPublisher`,
  related application/app-api/infrastructure tests,
  and this log.
- Behavior:
  Added RabbitMQ event `user.search.reindex.requested` for member-search reindex requests.
  Affiliate/department/team/position update APIs no longer call Elasticsearch reindexing directly; they publish scoped reindex events only when search-visible fields actually changed.
  Admin user create/update/status-change and my-profile update flows now request user-scoped async reindex events instead of directly touching Elasticsearch.
  Reindex events are registered with `afterCommit` so the consumer reads committed DB state, not pre-commit data.
  The new consumer routes `reindexAll`, `userIds`, `affiliateId`, `departmentId`, `teamId`, or `positionId` requests back into the existing `UserSearchIndexPort` implementation. Duplicate user IDs collapse to a single upsert target, preserving idempotent final Elasticsearch state.
  The manual admin endpoint `POST /api/v1/admin/users/search-index/reindex` stays synchronous and keeps its existing response DTO.
  Search API fallback behavior is unchanged: Elasticsearch first, DB LIKE fallback second.
  Infra repo `rabbitmq/definitions.json` does not yet contain `api.user.search.reindex` / `user.search.reindex.requested`; BE now auto-declares that queue/binding via `RabbitMqMessagingConfig`, so infra follow-up may still be needed for managed environments.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat --no-problems-report :common:compileJava :domain:compileJava :application:compileJava :infrastructure:compileJava :app-api:compileJava`
  Passed `./gradlew.bat --no-problems-report :app-api:test --tests "com.meetbowl.api.admin.AdminUserControllerTest" --tests "com.meetbowl.api.messaging.UserSearchReindexRequestedListenerTest"`
  `./gradlew.bat --no-problems-report :application:test --tests "com.meetbowl.application.admin.AdminOrganizationMasterDataUseCaseTest" --tests "com.meetbowl.application.admin.AdminUserManagementUseCaseTest" --tests "com.meetbowl.application.user.MyProfileUseCaseTest" --tests "com.meetbowl.application.user.UserSearchReindexUseCaseTest"` is blocked by pre-existing unrelated `application` test compile failures in `meeting` / `minutes` / `transcript` tests.
  `./gradlew.bat --no-problems-report :infrastructure:test --tests "com.meetbowl.infrastructure.messaging.user.RabbitUserSearchReindexEventPublisherTest"` is blocked by pre-existing unrelated `RabbitEventPublisherTest` / `RabbitDocumentIndexRequestedEventPublisherTest` compile failures.

2026-06-18 Admin organization/member excel import-export API

- Purpose: add admin-only Excel download/import APIs so affiliate, department, team, position, and member master data can be exported from the current DB and bulk-applied back in one validated transaction.
- Changed files:
  `application/build.gradle`,
  `application/admin/AdminOrganizationMembersExcelUseCase`,
  `application/admin/AdminOrganizationMembersExcelApplyService`,
  `application/admin/AdminOrganizationMembersExcelAuditService`,
  `application/admin/excel/OrganizationMembersExcelWorkbookMapper`,
  `application/admin/excel/OrganizationMembersExcelRows`,
  `app-api/admin/AdminOrganizationMembersExcelController`,
  `app-api/admin/dto/AdminOrganizationMembersExcelImportResponse`,
  `app-api/admin/AdminOrganizationMembersExcelControllerTest`,
  `application/admin/AdminOrganizationMembersExcelUseCaseTest`,
  `common/response/ErrorDetail`,
  `domain/user/UserRepositoryPort`,
  `infrastructure/persistence/user/JpaUserRepositoryAdapter`,
  several existing application test fakes updated with `findAll()`,
  and this log.
- Behavior:
  Added `GET /api/v1/admin/organization-members/excel` that returns an `.xlsx` workbook matching the v2 template layout and fills affiliate/department/team/position/member rows from the current DB.
  Added `POST /api/v1/admin/organization-members/excel/import` that accepts `.xlsx` multipart uploads (`file`) and validates required sheets, required columns, required values, UUIDs, allowed role/status values, numeric `sortNumber`, email format, duplicate rows, duplicate `loginId`, and organization hierarchy references before any DB write.
  Import matching uses `id` first for organization rows, then template business keys (`affiliateName`, `affiliateName+departmentName`, `affiliateName+departmentName+teamName`, `positionName`). Member import resolves existing users by `loginId` first, then `userId` when needed, and rejects login ID changes for existing users.
  Successful imports update or create only the rows present in the workbook; missing DB rows are not deleted.
  New users are created with initial password `1234`, password hash encoding, and `initialPasswordChangeRequired=true`.
  `sortNumber` is applied to department/team/position rows on both create and update.
  Import success publishes a single `user.search.reindex.requested` event with `reindexAll=true` through the existing after-commit dispatcher so Elasticsearch is not called synchronously inside the API transaction.
  Success and failure audit logs are recorded without storing raw workbook contents, passwords, tokens, or member PII payloads beyond field-level validation metadata.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :application:compileJava :app-api:compileJava`
  Passed `./gradlew.bat :app-api:test --tests "com.meetbowl.api.admin.AdminOrganizationMembersExcelControllerTest"`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.admin.AdminOrganizationMembersExcelUseCaseTest"` is still blocked by pre-existing unrelated `application` test compile failures in `meeting` / `minutes` / `transcript` tests. This work also added `findAll()` implementations to existing fake user repositories so the new `UserRepositoryPort` method does not introduce extra failures on top of those pre-existing test issues.

2026-06-19 Chatbot AI RestClient bean qualifier fix

- Purpose: fix `ChatbotAiClientAdapter` startup injection ambiguity after multiple `RestClient` beans exist in the infrastructure module.
- Changed files:
  `infrastructure/client/chatbot/ChatbotAiClientAdapter`,
  and this log.
- Behavior:
  `ChatbotAiClientAdapter` now explicitly injects the `aiServerRestClient` bean with `@Qualifier("aiServerRestClient")`.
  This keeps chatbot calls on the internal meetbowl-ai client with `X-Internal-Token` and prevents accidental injection ambiguity with `userSearchElasticsearchRestClient`.
- Excluded scope:
  Did not change AI chatbot request/response mapping, endpoint path, internal token configuration, or Elasticsearch search client configuration.
- Verification:
 Passed `./gradlew --no-problems-report :infrastructure:compileJava :app-api:compileJava`.
 `./gradlew --no-problems-report :infrastructure:spotlessApply :infrastructure:test --tests "com.meetbowl.infrastructure.client.chatbot.ChatbotAiClientAdapterTest"` applied formatting but could not run the targeted test because the `infrastructure` test source set has pre-existing unrelated compile failures in `RabbitEventPublisherTest` and `RabbitDocumentIndexRequestedEventPublisherTest`.

2026-06-19 RabbitMQ minutes generated queue auto declaration

- Purpose: ensure the BE RabbitMQ listener for `minutes.generated` can start even when the infra definitions or AI server have not pre-created `api.minutes.generated`.
- Changed files: `app-api/src/main/java/com/meetbowl/api/messaging/RabbitMqMessagingConfig.java`, `task/agent-work-log.md`.
- Behavior:
  BE now declares the `api.minutes.generated` queue, its `minutes.generated` binding on `meetbowl.topic`, and `dlq.api.minutes.generated` with `meetbowl.dlx` binding during application startup.
  Existing transcript final save and user search reindex queues also now declare their DLQ queues and bindings from the same config class, so dead-letter routing targets exist even when infra definitions are not loaded first.
  The minutes generated queue declaration uses the same `meetbowl.rabbitmq.minutes-generated-queue` property as the listener, so an environment override does not leave the listener queue undeclared.
- Verification:
  Reproduced before fix against local API: `POST /api/v1/admin/users/{userId}/password/reset` -> `POST /api/v1/auth/login` with `user1 / 1234` succeeded, but follow-up protected calls failed with `500` and `BadJwtException: Access Token is revoked.` in `bootrun.out.log`.
  Pending local rerun after rebuild/restart: login with reset password, forced password change, and post-change re-login.

2026-06-19 Account security flow hardening

- Purpose: add a public password reset request API, block concurrent login of the same ADMIN account, and verify the existing password change/reset flow stays intact.
- Changed files:
  `application/auth/LoginUseCase`, new `PasswordResetRequestCommand`, new `PasswordResetRequestUseCase`,
  `domain/auth/TokenStateRepositoryPort`,
  `infrastructure/cache/auth/RedisTokenStateRepositoryAdapter`,
  `infrastructure/client/chatbot/ChatbotAiClientAdapter`,
  `app-api/auth/AuthController`, new `app-api/auth/dto/PasswordResetRequest`,
  `app-api/config/SecurityConfig`,
  `common/exception/ErrorCode`,
  related auth/security tests, and this log.
- Behavior:
  Added `POST /api/v1/auth/password-reset/request` as a public endpoint and kept `/api/v1/auth/password/reset-request` as a compatibility alias.
  The password reset request flow trims `loginId` and `email`, always returns the same acceptance message, and records an `AdminAuditLog` entry only when the account exists and the email matches.
  The audit snapshot stores only `requestSource=PUBLIC_API` so login ID, email, password, token, and other raw secret/PII values are not written to logs.
  ADMIN login now checks for an active refresh token before issuing new tokens and returns `AUTH_ADMIN_ALREADY_LOGGED_IN` when the same admin account is already signed in elsewhere.
  Active-session 판단 is based on a refresh-token hash that still exists in Redis; stale hashes left in the per-user set are cleaned up during the check, so expired/revoked tokens are not treated as active sessions.
  USER login policy is unchanged. ADMIN logout/password reset/session revoke behavior continues to work through the existing refresh-token revoke flow.
  Also fixed an unrelated-but-blocking test-context issue by qualifying `ChatbotAiClientAdapter` to use `aiServerRestClient`, avoiding `RestClient` bean ambiguity after the Elasticsearch client was added.
- Verification:
  Passed `./gradlew.bat spotlessApply --no-problems-report`
  Passed `./gradlew.bat :common:compileJava :domain:compileJava :application:compileJava :infrastructure:compileJava :app-api:compileJava --no-problems-report`
  Passed `./gradlew.bat :app-api:test --tests "*Auth*" --tests "*Password*" --tests "*AdminUser*" --no-problems-report`
  `./gradlew.bat :application:test --tests "com.meetbowl.application.auth.LoginUseCaseTest" --tests "com.meetbowl.application.auth.PasswordResetRequestUseCaseTest" --tests "com.meetbowl.application.admin.ResetUserPasswordUseCaseTest" --no-problems-report` is still blocked by pre-existing unrelated `application:compileTestJava` failures in `meeting`, `minutes`, and `transcript` tests on the current branch.
  Passed `./gradlew spotlessApply`
  Passed `./gradlew :app-api:compileJava`

2026-06-19 Admin member/org delete APIs

- Purpose: add admin delete APIs for members, departments, teams, and positions with safe deletion policies, reference validation, audit logging, and user-search impact handling.
- Changed files:
  `app-api/admin/AdminUserController`,
  `app-api/admin/AdminOrganizationController`,
  `application/admin/AdminUserManagementUseCase`,
  `application/admin/AdminOrganizationMasterDataUseCase`,
  `domain/organization/DepartmentRepositoryPort`,
  `domain/organization/TeamRepositoryPort`,
  `domain/organization/PositionRepositoryPort`,
  `infrastructure/persistence/organization/JpaDepartmentRepositoryAdapter`,
  `infrastructure/persistence/organization/JpaTeamRepositoryAdapter`,
  `infrastructure/persistence/organization/JpaPositionRepositoryAdapter`,
  `infrastructure/persistence/organization/SpringDataTeamRepository`,
  related admin/user tests, and this log.
- Behavior:
  Added `DELETE /api/v1/admin/users/{userId}` and implemented it as `INACTIVE` status change instead of hard delete so meetings, mail, audit logs, and other linked data keep referential integrity.
  Member delete now blocks self-delete and duplicate delete for already inactive members, revokes active sessions immediately, writes success/failure `AdminAuditLog`, and publishes targeted user-search reindex events so admin/user search results reflect the inactive state.
  Existing admin status update flow now also blocks self-inactivation to prevent bypassing the new self-delete policy.
  Added `DELETE /api/v1/admin/organizations/departments/{departmentId}` with child-team/member reference checks before physical delete.
  Added `DELETE /api/v1/admin/organizations/teams/{teamId}` with member reference checks before physical delete.
  Added `DELETE /api/v1/admin/organizations/positions/{positionId}` with member reference checks before physical delete.
  Organization delete paths write success/failure `AdminAuditLog`. They do not trigger user-search reindex because delete is allowed only when no member reference remains, so there is no affected user document to refresh.
- Verification:
  Passed `./gradlew.bat spotlessApply`
  Passed `./gradlew.bat :common:compileJava :domain:compileJava :application:compileJava :infrastructure:compileJava :app-api:compileJava`
  Passed `./gradlew.bat :app-api:test --tests "*AdminUser*" --tests "*Organization*" --tests "*Position*"`
  
2026-06-19 회의 팝업 라우팅 수정 및 입장 가능 시각 제한

- 작업 목적: 회의 입장 팝업이 `/app/dashboard`로 잘못 이동하는 라우팅 문제를 수정하고, 예약 회의는 시작 15분 전부터만 입장 가능하도록 서버 규칙을 추가한다.
- 변경 파일: `app-api` 라우팅 문서 `docs/api-spec.md`, `common/exception/ErrorCode.java`, `application/meeting/JoinMeetingUseCase.java`, `application` 회의 입장 테스트 `JoinMeetingUseCaseTest.java`, 이 작업 기록 파일.
- 동작 변경:
  회의 입장 API는 종료된 회의뿐 아니라 시작 15분 전보다 이른 입장도 `MEETING_JOIN_TOO_EARLY`(409)로 거절한다.
  회의가 DB에 없는 로컬 fallback room 경로는 기존처럼 즉시 입장을 허용해 개발용 진입 흐름은 유지한다.
  API 문서에 새 입장 제한 규칙과 에러 코드를 반영했다.
- 검증 방법:
  통과: `bash ./gradlew :app-api:test --tests "com.meetbowl.api.meeting.MeetingControllerTest"`
  실패(기존 브랜치 이슈): `bash ./gradlew :application:test --tests "com.meetbowl.application.meeting.JoinMeetingUseCaseTest"`
  실패 원인: 현재 브랜치의 다른 테스트 소스(`GetMeetingTranscriptUseCaseTest`, `MinutesUseCaseTest`, `TransferMeetingHostUseCaseTest`, `EndMeetingUseCaseTest`)가 이미 최신 `Meeting.of(...)` 시그니처와 Repository Port 계약을 따라가지 못해 `:application:compileTestJava` 단계에서 막힌다. 이번 변경 파일과 직접 관련 없는 기존 컴파일 실패다.
- 후속 참고: 프론트엔드에서도 같은 에러 코드를 사용해 팝업 오픈 전 알림과 로비 안내 메시지를 표시하도록 함께 반영했다

2026-06-22 Organization code auto generation API

- Purpose: move department/team/position code ownership fully into BE so create/update APIs and organization-member Excel import no longer depend on admin-entered codes.
- Changed files:
  `application/admin/OrganizationCodeGenerator`,
  `application/admin/AdminOrganizationMasterDataUseCase`,
  `application/admin/AdminOrganizationMembersExcelApplyService`,
  `app-api/admin/AdminOrganizationController`,
  `app-api/admin/dto/AdminDepartmentRequest`,
  `app-api/admin/dto/AdminTeamRequest`,
  `app-api/admin/dto/AdminPositionRequest`,
  related admin organization / excel tests, and this log.
- Behavior:
  Department create now generates `D001`, `D002`... from the largest existing department code suffix and keeps the existing code on update.
  Team create now generates `T001`, `T002`... from the largest existing team code suffix and keeps the existing code on update.
  Position create now generates `P001`, `P002`... from the largest existing position code suffix and keeps the existing code on update.
  Code generation ignores client-sent `code` values for department/team/position create and update requests.
  Admin organization request DTOs no longer require `code`, so FE can omit it without validation failure.
  Excel import now allows blank `departmentCode` / `teamCode` / `positionCode` for new rows and still preserves/exports existing codes. Import also ignores manual code edits for existing rows and applies server-generated codes for new rows.
- Notes:
  Added concise Korean comments around D/T/P prefix sequencing, non-reuse intent for missing numbers, update-time code freeze, and Excel import auto-generation policy so PR reviewers can follow the policy quickly.
  Duplicate prevention is currently best-effort at the application layer by reading the highest existing suffix and allocating the next value in the same transaction; this change did not introduce a new DB migration or hard unique constraint.
- Verification:
  Passed `.\gradlew.bat spotlessApply`
  Passed `.\gradlew.bat :common:compileJava :domain:compileJava :application:compileJava :infrastructure:compileJava :app-api:compileJava`
  Passed `.\gradlew.bat :app-api:test --tests "*Organization*" --tests "*Position*" --tests "*Excel*"`
 `.\gradlew.bat :application:test --tests "com.meetbowl.application.admin.AdminOrganizationMasterDataUseCaseTest" --tests "com.meetbowl.application.admin.AdminOrganizationMembersExcelUseCaseTest"` is still blocked by pre-existing unrelated `application:compileTestJava` failures in `meeting` tests such as `EndMeetingUseCaseTest` and `TransferMeetingHostUseCaseTest`.

2026-06-22 BE local environment duplicate secret cleanup

- Purpose: fix local login returning `COMMON_INTERNAL_ERROR` after the BE environment file was extended for LiveKit/STT configuration.
- Changed files: `.env`, `.env.example`, and this log.
- Behavior: removed the later empty duplicate declarations that overwrote `MEETBOWL_JWT_SECRET` and `MEETBOWL_INTERNAL_TOKEN`. The optional `MEETBOWL_STT_INTERNAL_TOKEN` is now commented out by default so Spring falls back to the shared internal token instead of receiving an explicit empty value.
- Excluded scope: did not change authentication logic, token format, CORS, database configuration, or LiveKit/STT implementation.
- Verification: confirmed `.env` and `.env.example` have no duplicate keys, then loaded the corrected `.env` into a separately started backend on port 18080 and confirmed the local seed login returns HTTP 200 with access/refresh tokens.

2026-06-22 Meeting join STT session contract fix

- Purpose: fix meeting join returning HTTP 503 after STT startup because BE omitted the organization ID required by the STT `ensure-started` contract.
- Changed files: `JoinMeetingCommand`, `MeetingRealtimeSessionStarter`, `JoinMeetingUseCase`, `MeetingController`, `HttpMeetingRealtimeSessionStarter`, related meeting tests, local BE/Infra environment files, and this log.
- Behavior: authenticated meeting joins now propagate the JWT organization ID through the application port into the STT `organizationId` request field. Organization-less local/guest fallback joins skip STT session creation instead of blocking LiveKit token issuance. Local BE-to-STT internal token values and the LiveKit secret were aligned, and the LiveKit container was recreated with the corrected secret.
- Verification: passed application/infrastructure/app-api compilation, Spotless, and `MeetingControllerTest`. A separately started BE completed `login -> meeting join -> STT RUNNING -> LiveKit token response` with HTTP 200; both diagnostic STT sessions were stopped afterward. The targeted `JoinMeetingUseCaseTest` could not run because the existing application test source set has 14 unrelated compile failures in transcript/minutes/meeting tests caused by stale domain and repository signatures.

2026-06-23 AI 회의록 생성 Context 및 저장 멱등성 연결

- 작업 목적: `meeting.ended` 필수 식별자를 정상 발행하고, AI가 실제 Final Transcript로 생성한 Tiptap 회의록 초안을 안전하게 저장한다.
- 변경 내용: Host 조직 기반 `organizationId` 해석 Port, Reviewer 필수 검증, 시스템 전용 minutes-generation-context API, 회의·참석자·사용자·Transcript 조립 Adapter, 공통 Final Transcript 텍스트 조립 컴포넌트, `minutes.generated` payload 검증과 durable inbox를 추가했다.
- 동작 변경: Context API는 Final Transcript를 sequence 순으로 결합한다. AI 생성 결과는 `summary`와 Tiptap `editorContent` JSON으로 저장한다. 동일 `eventId`는 다시 처리하지 않고, 재생성 결과는 `DRAFT`만 교체해 `IN_REVIEW` 검토자 수정본을 보호한다.
- 제외 범위: FE 회의록 편집·승인 연결, 회의록 공유, 별도 agenda/action 컬럼 저장은 변경하지 않았다.
- 검증: `:app-api:compileJava` 성공. 오래된 회의/Transcript/Minutes 테스트 Stub을 현재 Port 계약에 맞춘 뒤 `EndMeetingUseCaseTest`, `FinalTranscriptTextAssemblerTest`, `GetMeetingTranscriptUseCaseTest`, `GetMinutesGenerationContextUseCaseTest`, `MinutesUseCaseTest`와 app-api 회의록 Controller/Consumer 테스트가 통과했다. 전체 Spotless 검사는 이번 작업과 무관한 기존 domain/application 파일 포맷 위반으로 차단됐다.

2026-06-23 회의록 FE 연결용 응답 메타데이터 보강

- 작업 목적: FE 회의록 목록/상세 화면이 mock 데이터 대신 BE API만으로 회의명, 시간, 참석자 수, 검토자 표시명을 렌더링할 수 있게 한다.
- 변경 내용: `MinutesMeetingMetadataAssembler`를 추가해 회의, 참석자, 사용자, 부서 정보를 조립하고 `GET /minutes`, `GET/PATCH/approve /meetings/{meetingId}/minutes` 응답에 `meetingTitle`, `meetingStartedAt`, `meetingEndedAt`, `attendeeCount`, `reviewerName`, `reviewerDepartment`를 추가했다.
- 동작 변경: 회의록 조회/수정/승인 응답이 모두 같은 화면 메타데이터를 포함한다. JPA 회의 저장소에는 batch 조회 `findByIds` override를 추가했고, domain port에는 테스트 fake 호환용 기본 구현을 두었다.
- 검증: `:application:test --tests MinutesUseCaseTest --tests MinutesFavoriteUseCaseTest`, `:app-api:test --tests MinutesControllerTest --tests MinutesListControllerTest`, `:app-api:compileJava` 통과.

2026-06-23 회의록 승인 자동 공유 및 미참석자 수동 공유 분리

- 작업 목적: 내부메일 공유 의미를 바로잡아, 회의 참여자 전체 발송은 회의록 승인 시 자동 수행하고 사용자가 누르는 내부메일 공유는 회의 미참석자에게 별도로 보내는 기능으로 제한한다.
- 변경 내용: `ApproveMinutesUseCase`가 승인 후 `document.index.requested` 발행과 함께 참여자 자동 공유 메일을 발송하도록 변경했다. `ShareMinutesUseCase`, `ShareMinutesCommand`, `ShareMinutesRequest`, `POST /meetings/{meetingId}/minutes/share`를 추가해 승인된 회의록의 미참석자 수동 공유를 처리한다. 제거된 별도 참여자 공유 경로는 보안 내부 토큰 대상과 API 문서에서 제외했다.
- 동작 변경: 승인 성공 후 회의록 상태는 `SHARED`가 되며, 승인자는 메일 도메인의 자기 자신 발송 금지 규칙 때문에 자동 수신자에서 제외된다. 수동 공유는 `APPROVED` 또는 `SHARED` 상태에서만 가능하고, 수신자에 회의 참여자가 포함되면 `COMMON_INVALID_REQUEST`로 거절한다.
- 제외 범위: 외부 SMTP 발송 어댑터, 메일함 UI 세부 화면, 참석자 자동 공유 실패 재시도 정책은 변경하지 않았다.
- 검증: 샌드박스에서는 Gradle native-platform dylib 로딩 실패로 실행되지 않아 권한 상승으로 재실행했다. 이후 `:application:test --tests MinutesUseCaseTest --tests ShareMinutesUseCaseTest`, `:app-api:test --tests MinutesControllerTest --tests SecurityConfigTest`, `:app-api:compileJava` 통과. FE 연동 변경은 `npm run build`, `npm test`, `git diff --check` 통과.

2026-06-23 Admin user effective status / delete behavior fix

- Purpose: fix QA issues where expired users still appeared active and admin user delete only downgraded status to `INACTIVE` instead of removing the member from admin/user listings.
- Changed files:
  `domain/user/User`,
  `domain/user/UserRepositoryPort`,
  `infrastructure/persistence/user/UserEntity`,
  `infrastructure/persistence/user/SpringDataUserRepository`,
  `infrastructure/persistence/user/JpaUserRepositoryAdapter`,
  `infrastructure/persistence/user/UserSearchSourceRow`,
  `infrastructure/search/user/ElasticsearchUserSearchAdapter`,
  `application/admin/AdminUserManagementUseCase`,
  `application/user/UserDirectoryUseCase`,
  `application/user/MyProfileUseCase`,
  related user/admin tests, and this log.
- Behavior:
  User status responses now use an effective-status rule instead of returning the raw DB `status` blindly.
  Effective status is `ACTIVE` only when raw status is `ACTIVE` and the current UTC date is within the inclusive range `activeFrom <= today <= activeUntil`.
  If raw status is `ACTIVE` but `activeUntil` is before today or `activeFrom` is after today, the exposed status becomes `INACTIVE`.
  Deleted users are soft-deleted with `deletedAt`; they are excluded from repository lookups, admin list search, user directory search, and summary/detail reads.
  Admin delete no longer converts a member to raw `INACTIVE`. It writes `deletedAt`, revokes sessions, and tombstones `loginId` / `email` so future unique-value reuse can proceed safely without leaving the deleted account visible.
  Elasticsearch user-search documents now carry `activeFrom`, `activeUntil`, and `deleted` metadata so search filtering follows the same date and deletion policy as the DB fallback.
- Notes:
  Added concise comments around the date-boundary interpretation and soft-delete policy in the persistence/search path where the behavior is easiest to misunderstand during review.
  Delete response still exposes `INACTIVE` as the final status string because the account is no longer active, but the deleted member is filtered out of every default listing/query path.
- Verification:
  Passed `.\gradlew.bat :domain:test --tests "com.meetbowl.domain.user.UserTest"`
  Passed `.\gradlew.bat :domain:compileJava :application:compileJava :infrastructure:compileJava :app-api:compileJava`
`:application:test` and `:infrastructure:test` targeted runs are still blocked by pre-existing unrelated test-compilation failures in modules outside this change set, including `meeting`, `minutes`, `transcript`, and `messaging` tests.
2026-06-23 Admin organization/position sort order duplication fix

- Purpose: fix QA issue where department, team, and position master data could save duplicated sort values and therefore rendered repeated order numbers in the admin organization/position management tabs.
- Changed files:
  `application/admin/AdminOrganizationMasterDataUseCase`,
  `domain/organization/{DepartmentRepositoryPort,TeamRepositoryPort,PositionRepositoryPort}`,
  `infrastructure/persistence/organization/{SpringDataDepartmentRepository,SpringDataTeamRepository,SpringDataPositionRepository,JpaDepartmentRepositoryAdapter,JpaTeamRepositoryAdapter,JpaPositionRepositoryAdapter}`,
  `common/exception/ErrorCode`,
  `application/admin/AdminOrganizationMasterDataUseCaseTest`,
  `app-api/admin/AdminOrganizationControllerTest`,
  repository fake implementations in related application tests,
  and this log.
- Behavior:
  Department create/update now reject duplicated `sortOrder` within the same affiliate.
  Team create/update now reject duplicated `sortOrder` within the same affiliate even when the parent department differs.
  Position create/update now reject duplicated `sortOrder` across the current global position master scope.
  Duplicate checks include inactive entries because they remain visible in the admin list.
  Self-update keeps working because duplicate queries exclude the current entity ID on update.
  Deleted entries are excluded from duplicate checks under the current hard-delete policy because the row is physically removed.
  Duplicate sort saves return `ORGANIZATION_SORT_ORDER_DUPLICATED` with HTTP 409 and the message `이미 사용 중인 순서입니다. 다른 순서를 입력해 주세요.`
- Notes:
  Added concise Korean comments only where the validation scope is easy to misread, especially the rule that team order uniqueness is affiliate-wide rather than department-local.
  No DB migration was added in this change set. Repository/Application-level validation is implemented, and the current repo does not expose a Flyway/Liquibase migration path for these tables here.
- Verification:
  Passed `.\gradlew.bat :application:compileJava :app-api:compileJava :infrastructure:compileJava`
  Attempted `.\gradlew.bat :application:test --tests "com.meetbowl.application.admin.AdminOrganizationMasterDataUseCaseTest"` but it is blocked by pre-existing unrelated `application:compileTestJava` failures in `EndMeetingUseCaseTest` and `TransferMeetingHostUseCaseTest`.
  Attempted `.\gradlew.bat :app-api:test --tests "com.meetbowl.api.admin.AdminOrganizationControllerTest"` but it is blocked by a pre-existing unrelated `app-api:compileTestJava` failure in `MeetingControllerTest`.
2026-06-23 Admin audit log display labels and summaries

- Purpose: fix QA issue where the admin audit log list/detail exposed raw enum names and inconsistent JSON instead of user-friendly Korean labels and readable work summaries.
- Changed files:
  `app-api/admin/dto/AdminAuditLogResponse`,
  `app-api/admin/dto/AdminAuditLogDisplayFormatter`,
  `app-api/admin/AdminAuditLogControllerTest`,
  `app-api/admin/dto/AdminAuditLogResponseTest`,
  and this log.
- Behavior:
  Audit log responses now keep raw `actionType`, `targetType`, `beforeSnapshot`, and `afterSnapshot` while adding display-only fields `actionLabel`, `targetTypeLabel`, `displayTitle`, and `displayChangeItems`.
  Known admin action types and target types now resolve to Korean labels, while unmapped values safely fall back to the original raw code.
  User update logs now summarize status and active-period changes with readable Korean labels and formatted dates instead of exposing epoch timestamps directly.
  Organization/member Excel logs now expose compact summary items such as file name, success/failure result, failure reason, processed count, and error count rather than raw JSON in the primary display area.
  When a snapshot cannot be mapped cleanly, the formatter falls back to compact key/value items or the message `작업 상세 정보를 확인할 수 없습니다.` instead of surfacing raw JSON as the main summary.
- Notes:
  Added concise Korean comments around the display-field normalization path and the mixed epoch-second/epoch-millisecond timestamp handling because those are the parts most likely to confuse reviewers later.
  Existing raw audit fields remain in the response for backward compatibility and audit traceability; the new fields are additive only.
- Verification:
  Passed `.\gradlew.bat :app-api:compileJava`
  Attempted `.\gradlew.bat :app-api:compileJava :app-api:compileTestJava` and `.\gradlew.bat :app-api:test --tests "com.meetbowl.api.admin.AdminAuditLogControllerTest" --tests "com.meetbowl.api.admin.dto.AdminAuditLogResponseTest"` but both are blocked by a pre-existing unrelated `app-api:compileTestJava` failure in `app-api/src/test/java/com/meetbowl/api/meeting/MeetingControllerTest.java` because its `MeetingController` constructor call is missing the `TransferMeetingHostUseCase` argument.

2026-06-23 Admin audit log target info and display formatter hardening

- Purpose: fix QA issues where member-targeted admin audit logs missed target login/name and detail views still surfaced raw/internal fields instead of operator-friendly Korean summaries.
- Changed files:
  `domain/admin/AdminAuditLog`,
  `infrastructure/persistence/admin/AdminAuditLogEntity`,
  `application/admin/AdminAuditLogResult`,
  `application/admin/AdminUserManagementUseCase`,
  `application/admin/ResetUserPasswordUseCase`,
  `application/auth/PasswordResetRequestUseCase`,
  `application/mail/MailRetentionPolicyUseCase`,
  `application/admin/AdminOrganizationMasterDataUseCase`,
  `application/admin/AdminOrganizationMembersExcelApplyService`,
  `application/admin/AdminOrganizationMembersExcelAuditService`,
  `app-api/admin/dto/AdminAuditLogResponse`,
  `app-api/admin/dto/AdminAuditLogDisplayFormatter`,
  related admin/auth/domain/JPA tests,
  and this log.
- Behavior:
  Admin audit log storage now keeps optional `targetLoginId` and `targetName` alongside the existing raw target metadata.
  Member create/update/delete/status-change/password-reset audit writes now always persist target member ID, login ID, and display name; delete captures the original login/name before tombstone replacement.
  Audit log API responses now return `targetLoginId` and `targetName` with `-` fallback for old rows that never stored them.
  Display labels now map known action/target codes to Korean, including member status change/password reset, organization-member Excel upload/download, organization master data, meeting room, retention policy, and mail policy actions. Unmapped values still fall back to raw codes.
  Display summaries now exclude internal fields such as `id`, `createdAt`, `updatedAt`, `deletedAt`, `modifiedAt`, `lastModifiedAt`, `version`, `createdBy`, and `updatedBy`.
  Policy change summaries now render operator-friendly diffs like `365일 -> 372일` and `아니오 -> 예`, while Excel import logs render compact summary items instead of raw JSON.
  Password-related audit payloads still avoid temporary passwords, password hashes, tokens, JWTs, refresh tokens, and similar sensitive values.
- Verification:
  Passed `.\gradlew.bat :domain:test --tests "com.meetbowl.domain.admin.AdminAuditLogTest"`
  Passed `.\gradlew.bat :app-api:compileJava :application:compileJava :infrastructure:compileJava :domain:compileJava`
  Attempted targeted `:app-api:test` runs for audit-log controller/response tests, but `app-api:compileTestJava` is blocked by a pre-existing unrelated constructor mismatch in `app-api/src/test/java/com/meetbowl/api/meeting/MeetingControllerTest.java`.
  Attempted targeted `:application:test` runs for admin/auth audit-log tests, but `application:compileTestJava` is blocked by pre-existing unrelated `meeting`, `minutes`, and `transcript` test compile failures on the current branch.

2026-06-23 Password reset request approval workflow

- 작업 목적: 회의 예약 수정 흐름에서 주최자/검토자 역할 정합성을 맞추고, 초대 회의 목록/회의실 예약 조회에서 본인 주최 회의가 중복 노출되는 문제와 관리자 시간대별 회의실 사용 집계 왜곡을 함께 보정한다.
- 변경 파일: `application/admin/AdminDashboardSummaryUseCase.java`, `application/admin/AdminDashboardSummaryUseCaseTest.java`, `application/meeting/GetMeetingUseCase.java`, `application/meeting/MeetingAttendeeWriter.java`, `application/meetingroom/GetRoomReservationsUseCase.java`, 이 작업 기록 파일.
- 동작 변경:
  회의 참석자 저장은 주최자를 한 번만 저장하고, 주최자 본인을 회의록 검토자로 지정한 경우 HOST 참석자 행에 `reviewer=true` 플래그를 함께 저장한다.
  따라서 회의 생성/수정 시 host와 reviewer가 동일한 경우에도 `(meeting_id, user_id)` 중복 삽입으로 500 오류가 나지 않는다.
  초대 회의 목록과 회의실 예약 현황 조회는 참석자 역할만 보지 않고 `meeting.isHostedBy(userId)` 기준으로 본인 주최 회의를 제외해 자기 회의가 초대 목록처럼 다시 섞여 보이지 않는다.
  관리자 대시보드 시간대별 회의실 사용 집계는 화면에서 사용하는 영업 시간대 기준에 맞게 시간 버킷 계산을 보정해 왜곡된 막대값이 나오지 않도록 정리했다.
- 검증 방법:
  통과: `bash ./gradlew :application:compileJava`
  통과: `bash ./gradlew bootJar`
  실패(기존 브랜치 이슈): `bash ./gradlew :application:test`는 현재 브랜치 전반의 `GetMeetingTranscriptUseCaseTest`, `MinutesUseCaseTest`, `TransferMeetingHostUseCaseTest` 등 기존 `:application:compileTestJava` 오류 때문에 이번 변경과 무관하게 중단됐다.

2026-06-22 Workspace file download and preview APIs

- Purpose: 개인/공유 워크스페이스에 업로드된 S3 파일을 사용자가 다운로드하고 브라우저에서 미리볼 수 있도록 원본 파일 반환 경로를 추가한다.
- Changed files:
  `domain/storage/ObjectStoragePort`,
  `domain/storage/StoredObject`,
  `infrastructure/storage/S3ObjectStorageAdapter`,
  `application/personalworkspace/drive/DownloadDriveFileUseCase`,
  `application/personalworkspace/drive/DriveFileDownloadResult`,
  `application/sharedworkspace/GetSharedWorkspaceFileUseCase`,
  `application/sharedworkspace/DownloadSharedWorkspaceFileUseCase`,
  `application/sharedworkspace/SharedWorkspaceFileDownloadResult`,
  `app-api/personalworkspace/WorkspaceDriveController`,
  `app-api/sharedworkspace/SharedWorkspaceFileController`,
  related storage/drive tests, `docs/api-spec.md`, and this log.
- Behavior:
  Added Object Storage download support using `storageKey`, returning original bytes, content type, and length.
  Personal drive download now verifies owner and deleted state before reading S3.
  Shared workspace download now verifies active workspace membership and file workspace ownership before reading S3.
  Existing metadata endpoints remain JSON responses, while `/download` returns `Content-Disposition: attachment` and `/preview` returns `Content-Disposition: inline`.
  Korean filenames are encoded through Spring `ContentDisposition.filename(..., UTF_8)` so browser download names do not break.
- Verification:
  Passed `./gradlew :app-api:compileJava :application:compileJava :infrastructure:compileJava`
  `./gradlew spotlessApply :application:test --tests '*DriveUseCaseTest' :infrastructure:test --tests '*S3ObjectStorageAdapterTest'` was blocked before executing the target tests because existing unrelated test sources in `meeting`, `minutes`, `transcript`, and messaging packages do not compile against the current domain/port contracts.

2026-06-23 Mail retention auto deletion scheduler

- Purpose: 메일 보관 정책의 `autoDeleteEnabled` 설정이 실제 메일함 상태에 반영되도록 서버 주기 작업을 추가한다.
- Changed files:
  `domain/mail/MailboxEntryRepositoryPort`,
  `application/mail/ApplyMailRetentionPolicyUseCase`,
  `application/mail/MailRetentionApplyResult`,
  `infrastructure/persistence/mail/JpaMailboxEntryRepositoryAdapter`,
  `infrastructure/persistence/mail/SpringDataMailboxEntryRepository`,
  `app-api/mail/MailRetentionScheduler`,
  `application` mail retention tests, `docs/api-spec.md`, and this log.
- Behavior:
  정책 row가 없거나 `autoDeleteEnabled=false`이면 자동 삭제 배치는 메일함 항목을 변경하지 않는다.
  `autoDeleteEnabled=true`이면 받은/보낸 메일함 항목 중 보관 기간을 지난 항목을 휴지통으로 이동한다.
  휴지통 항목은 휴지통 보관 기간을 지난 경우 영구 삭제 시각을 기록한다.
  자동 정리는 공용 메일 본문을 삭제하지 않고 사용자별 `MailboxEntry` 상태만 변경해 다른 수신자에게 영향을 주지 않는다.
  `MailRetentionScheduler`는 기본 cron `0 0 3 * * *`를 KST(`Asia/Seoul`) 기준으로 실행하며 `meetbowl.mail.retention.cron` 프로퍼티로 조정할 수 있다.
- Verification:
  Passed `./gradlew :domain:compileJava :application:compileJava :infrastructure:compileJava :app-api:compileJava`
  Passed `./gradlew spotlessApply`
  `./gradlew :application:test --tests '*ApplyMailRetentionPolicyUseCaseTest'` was blocked before executing the target test because existing unrelated test sources in `meeting`, `minutes`, and `transcript` packages do not compile against the current domain/port contracts.

2026-06-23 Document indexing queue topology hardening

- Purpose: 워크스페이스 파일 업로드 후 AI 서버가 늦게 뜨거나 재시작 중이어도 `document.index.requested` 이벤트가 RabbitMQ에서 유실되지 않도록 색인 큐와 binding을 BE 기동 시점에도 보장한다.
- Changed files:
  `app-api/messaging/RabbitMqMessagingConfig` and this log.
- Behavior:
  BE now declares `ai.index.document` and `ai.index.document.removed` queues with their dead-letter routing before publishing document indexing/removal events.
  BE also declares `document.index.requested` / `document.index.removed` bindings on `meetbowl.topic`, plus `dlq.ai.index.document` / `dlq.ai.index.document.removed` queues on `meetbowl.dlx`.
  This preserves upload/delete events even when the AI consumer is not running yet; AI can consume the queued events after it starts.
  Existing AI queue names and DLQ routing keys are kept compatible with the AI server topology.
- Verification:
  Passed `./gradlew :app-api:compileJava`

2026-06-23 AI server client timeout adjustment

- Purpose: 다중 문서 RAG 챗봇 질의가 20초를 넘길 때 BE가 먼저 read timeout으로 실패 처리해 화면에 "AI 서버를 사용할 수 없습니다"가 표시되는 문제를 줄인다.
- Changed files:
  `infrastructure/client/AiServerClientConfig` and this log.
- Behavior:
  AI 서버 RestClient의 connect timeout은 3초로 유지한다.
  AI 서버 RestClient의 read timeout은 20초에서 45초로 늘려 검색·리랭크·생성을 포함한 multi-hop 질의를 기다릴 수 있게 했다.
- Verification:
  Passed `./gradlew :infrastructure:compileJava :app-api:compileJava`

2026-06-23 회의록 검토자 부서 미배정 조회 오류 수정 및 user1 검증

- 작업 목적: 더미 계정 `user1`을 회의록 검토자로 둔 실제 API 검증 중, 검토자 부서가 없는 경우 회의록 조회가 500으로 실패하는 문제를 수정한다.
- 변경 내용: `MinutesMeetingMetadataAssembler`에서 검토자의 `departmentId`가 null이면 부서명 조회를 건너뛰도록 null 경계를 추가했다. `MinutesMeetingMetadataAssemblerTest`를 추가해 부서 미배정 검토자도 조회 메타데이터를 정상 조립하는지 검증했다.
- 동작 변경: 검토자가 부서에 배정되지 않은 경우에도 회의록 목록/상세/수정/승인 응답은 성공하며 `reviewerDepartment`만 null로 내려간다.
- 검증: 샌드박스에서는 Gradle native-platform dylib 로딩 실패로 권한 상승 후 `:application:test --tests com.meetbowl.application.minutes.MinutesMeetingMetadataAssemblerTest` 통과. 로컬 BE를 18080 포트로 재기동해 user2 주최, user1 검토자 회의를 생성하고 RabbitMQ `minutes.generated`를 발행했다. user1 초안 조회(DRAFT), user1 수정(IN_REVIEW), user1 승인(SHARED), user2 자동 메일 수신(`MINUTES_SHARE`, `MEETING_MINUTES`, 수정 본문 포함)을 실제 API로 확인했다. 회의 참여자인 user2 대상 수동 공유는 `COMMON_INVALID_REQUEST`로 거절되는 것도 확인했다.

2026-06-23 Mail retention batch size hardening

- Purpose: 메일 보관 정책 스케줄러가 대규모 메일함을 한 번에 모두 조회/저장하지 않도록 처리 묶음 크기를 제한한다.
- Changed files:
  `domain/mail/MailboxEntryRepositoryPort`,
  `application/mail/ApplyMailRetentionPolicyUseCase`,
  `infrastructure/persistence/mail/JpaMailboxEntryRepositoryAdapter`,
  `infrastructure/persistence/mail/SpringDataMailboxEntryRepository`,
  `application` mail retention tests, `.env.example`, `docs/api-spec.md`, and this log.
- Behavior:
  `meetbowl.mail.retention.batch-size` 설정을 추가하고 기본값은 500으로 둔다.
  받은/보낸/휴지통 만료 항목은 offset pagination을 쓰지 않고, 오래된 항목부터 첫 묶음만 반복 조회해 처리한다.
  각 묶음 저장 후 같은 조건에서 다시 첫 묶음을 조회하므로 상태 변경으로 인한 offset skip을 만들지 않는다.
  RabbitMQ AI 문서 색인 큐 옵션은 기존 큐 선언 충돌 위험을 피하기 위해 변경하지 않았다.
- Verification:
  Passed `./gradlew spotlessApply`
  Passed `./gradlew :application:test --tests '*ApplyMailRetentionPolicyUseCaseTest'`
  Passed `./gradlew :domain:compileJava :application:compileJava :infrastructure:compileJava`

2026-06-23 Flyway 도입 및 baseline migration 준비

- 작업 목적: AWS RDS 운영 배포 전에 `meetbowl-be` 스키마를 JPA `ddl-auto` 의존에서 Flyway migration 기준으로 전환하고, 현재 엔티티 기준 baseline SQL을 고정할 준비를 마친다.
- 변경 내용: `infrastructure/build.gradle`에 Flyway 의존성을 추가했다. `application-local.properties`, `application-prod.properties`를 Flyway + `ddl-auto=validate` 기준으로 바꾸고 migration 위치를 고정했다. `application-schema-export.properties`를 추가해 H2 메모리 DB와 MariaDB dialect 조합으로 현재 JPA 매핑 SQL을 `build/generated-schema/meetbowl-baseline.sql`로 추출할 수 있게 했다. 추출 결과를 `app-api/src/main/resources/db/migration/V1__baseline.sql`에 baseline migration으로 반영했다. `README.md`에는 schema export 실행 경로와 migration 운영 방식을 정리했다.
- 동작 변경: 로컬/운영 프로필은 Flyway migration을 먼저 적용한 뒤 JPA validate만 수행하는 구조가 됐다. 운영 baseline은 리포지토리의 `V1__baseline.sql`이 기준이 되며, 이후 스키마 변경은 새 migration 파일로만 누적해야 한다.
- 제외 범위: 운영 마스터 데이터 seed, 최초 관리자 bootstrap, MariaDB Testcontainers 전환, 운영 compose와 CI/CD 반영은 아직 하지 않았다.
- 검증: 권한 상승으로 `./gradlew :app-api:bootRun --args='--spring.profiles.active=schema-export'`를 실행해 baseline SQL 추출 파일 생성을 확인했다. 이어 테스트 프로필에 `spring.flyway.enabled=false`를 명시하고, 다운로드 UseCase mock 누락이 있던 `MailControllerTest`, `WorkspaceDriveControllerTest`, 컨텍스트 격리가 필요했던 `AuthTokenStatePrecisionTest`를 보정했다. 이후 `./gradlew :app-api:test --tests com.meetbowl.api.auth.AuthTokenStatePrecisionTest`는 통과했고, 전체 `./gradlew test`는 `app-api:test`가 통과한 뒤 `application:test`의 기존 `UserDirectoryUseCaseTest#searchByOrganizationFiltersSuccess` 단일 assertion 실패 1건만 남았다.

2026-06-23 GitHub Actions 워크플로 재작성

- 작업 목적: 기존 다른 프로젝트용 GHCR 배포 워크플로를 Meetbowl BE 기준 AWS/ECR 방향으로 교체하고, 아직 없는 Dockerfile/배포 스크립트 때문에 main 파이프라인이 즉시 깨지지 않게 안전장치를 둔다.
- 변경 내용: `.github/workflows/backend-deploy.yml`을 전면 교체했다. PR/main push/workflow_dispatch 트리거, Java 25, MariaDB/Redis/RabbitMQ service 기반 테스트 job, AWS OIDC + ECR push job, EC2 SSH deploy job 구조로 재작성했다. 아직 `Dockerfile` 또는 `deploy-be.sh`가 없는 상태를 감지해 build/deploy를 skip하는 `detect-build-assets`와 `deploy-skip-notice` job도 추가했다.
- 동작 변경: PR에서는 테스트만 수행한다. main push에서는 테스트 후 Dockerfile/배포 스크립트 유무를 확인하고, 둘 다 준비됐을 때만 ECR push와 EC2 배포를 진행한다. 준비 전에는 skip notice만 남기고 실패시키지 않는다.
- 제외 범위: 실제 `Dockerfile`, `deploy-be.sh`, 운영 compose, smoke test 스크립트, GitHub secrets 등록, AWS IAM role 생성은 아직 하지 않았다.
- 검증: 워크플로 YAML을 로컬 diff로 검토했다. 실제 GitHub Actions 실행 검증은 아직 하지 않았다.

2026-06-23 meetbowl-be Dockerfile 추가

- 작업 목적: GitHub Actions의 ECR build 단계와 운영 compose가 공통으로 사용할 `meetbowl-be` 런타임 이미지를 만든다.
- 변경 내용: 루트 `Dockerfile`을 추가해 Temurin 25 JDK/JRE 기반 멀티스테이지 빌드로 `:app-api:bootJar` 결과만 런타임 이미지에 포함하도록 구성했다. `.dockerignore`를 추가해 `.git`, 각 모듈 `build`, 로컬 task 로그 등 불필요한 파일이 빌드 컨텍스트에 들어가지 않게 했다. `README.md`에는 로컬 이미지 빌드 명령을 추가했다.
- 동작 변경: 컨테이너 빌드 시 Gradle wrapper로 `app-api` bootJar를 생성하고, 런타임 이미지는 `/app/app.jar`만 가진 `prod` 기본 프로필 컨테이너로 실행된다. `JAVA_OPTS`를 환경변수로 주입할 수 있고 기본 timezone은 UTC다.
- 제외 범위: healthcheck, Actuator endpoint, 운영 compose, deploy script 연결은 아직 하지 않았다.
- 검증: 권한 상승으로 `./gradlew :app-api:bootJar -x test`를 실행해 `app-api/build/libs/app-api-0.0.1-SNAPSHOT.jar` 생성과 Dockerfile의 jar copy 경로가 맞는 것을 확인했다. 실제 `docker build` 실행 검증은 아직 하지 않았다.
- 2026-06-23: backend GitHub Actions 배포 워크플로를 운영 저장소 구조에 맞게 다시 단순화했다.
  - 요청 목적: `meetbowl-infra`에 배포 스크립트를 두는 구조로 결정된 뒤, `meetbowl-be` 워크플로가 GitHub runner에서 다른 레포 파일을 찾으려는 잘못된 가정을 제거해야 했다.
  - 변경 파일: `.github/workflows/backend-deploy.yml`
  - 변경 내용: `detect-build-assets`와 배포 스킵 분기 로직을 제거하고, `main` push 시 테스트 후 이미지 빌드/푸시, 이후 EC2의 `DEPLOY_PATH/scripts/deploy-be.sh`를 직접 실행하도록 고정했다.
  - 동작 변화: 배포 가능 여부를 runner 로컬 파일 존재 여부로 판단하지 않고, 운영 서버에 checkout 되어 있는 infra 레포 루트를 기준으로 배포를 수행한다.
  - 검증: 워크플로 YAML diff를 확인했고, infra 쪽 실제 배포 스크립트 경로 규약과 맞춰 검토했다.
  - 남은 작업: GitHub Secrets의 `MEETBOWL_DEPLOY_PATH`를 infra 레포 루트로 맞추고, EC2에 `meetbowl-infra`가 배포 경로에 clone 되어 있어야 한다.

2026-06-23 PR Actions checkout SHA 명시화

- 작업 목적: PR GitHub Actions가 merge commit을 테스트하면서 로컬 HEAD와 다른 코드 기준으로 실패하는 혼선을 줄이기 위해, 테스트 job의 checkout 대상을 PR head SHA로 고정한다.
- 변경 파일: `.github/workflows/backend-deploy.yml`, `task/agent-work-log.md`
- 변경 내용:
  `test` job의 checkout step을 PR과 비PR 이벤트로 분리했다.
  PR 이벤트에서는 `actions/checkout@v4`에 `ref: ${{ github.event.pull_request.head.sha }}`를 명시해 PR 브랜치 head commit을 직접 checkout하도록 바꿨다.
  push/workflow_dispatch는 기존처럼 이벤트 기본 ref를 checkout하도록 유지했다.
  추가로 `Show checked out revision` step을 넣어 `event_name`, `github.sha`, `pr_head_sha`, 실제 `git rev-parse HEAD`를 로그에 남기게 했다.
- 동작 변경:
  앞으로 PR test job은 GitHub가 만든 synthetic merge commit 대신 실제 PR head commit을 기준으로 `./gradlew test --no-daemon`를 실행한다.
  실패 로그만 봐도 workflow가 어떤 SHA를 테스트했는지 바로 확인할 수 있다.
- 제외 범위:
  `build-and-push`와 `deploy` job의 checkout 동작은 그대로 유지했다. 이 job들은 PR에서는 실행되지 않는다.
- 검증:
  workflow YAML diff를 로컬에서 확인했다.
  실제 실행 검증은 다음 PR run 또는 `workflow_dispatch` 로그에서 `Show checked out revision` step 출력으로 확인해야 한다.
- 남은 작업:
  필요하면 `build-and-push` job에도 동일한 SHA 출력 step을 추가해 이미지 빌드 기준 커밋 추적성을 맞출 수 있다.

2026-06-24 테스트 기준선 안정화 1차 정리

- 작업 목적: 새 브랜치에서 `./gradlew test` 기준선을 다시 세우고, flaky/fixture/test-wiring 성격의 실패를 먼저 제거해 실제 구조 문제만 남도록 정리한다.
- 변경 파일:
  `application/src/test/java/com/meetbowl/application/admin/AdminDashboardSummaryUseCaseTest.java`
  `infrastructure/src/test/java/com/meetbowl/infrastructure/messaging/RabbitEventPublisherTest.java`
  `infrastructure/src/test/java/com/meetbowl/infrastructure/messaging/document/RabbitDocumentIndexRequestedEventPublisherTest.java`
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/user/JpaUserRepositoryAdapterTest.java`
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/meeting/MeetingLifecycleTest.java`
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/meetingroom/MeetingRoomReservationsTest.java`
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/meetingroom/MeetingRoomStatusTest.java`
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/minutes/JpaMinutesFavoriteRepositoryAdapterTest.java`
  `task/agent-work-log.md`
- 변경 내용:
  Admin dashboard summary 테스트는 구현이 KST 자정 경계로 바뀐 상태에 맞춰 repository verify 기대값을 UTC 자정 기준에서 KST 기준 Instant로 수정했다.
  RabbitEventPublisherTest는 잘못된 `tools.jackson.*` import를 실제 `com.fasterxml.jackson.*`로 교체하고, 테스트용 ObjectMapper에도 `WRITE_DATES_AS_TIMESTAMPS=false`를 적용해 런타임과 같은 ISO-8601 직렬화 규칙을 맞췄다.
  RabbitDocumentIndexRequestedEventPublisherTest는 `DocumentIndexRequestedEvent.Metadata` / `DocumentIndexRequestedMessage.metadata` 구조 변경에 맞춰 테스트 payload 생성을 최신 계약으로 보정했다.
  JpaUserRepositoryAdapterTest는 Elasticsearch adapter가 필요한 `ObjectMapper` bean을 테스트 컨텍스트에 추가하고, 영문 fixture에 맞지 않던 한글 검색 기대값을 `service` / `assistant` 기준으로 수정했다.
  MeetingLifecycle/MeetingRoomReservations/MeetingRoomStatus 테스트는 실제 UseCase constructor 요구사항에 맞춰 `MeetingAttendeeWriter`, `GetRoomReservationsUseCase`, building/site adapter, no-op `DispatchNotificationUseCase` bean을 TestApplication에 추가해 컨텍스트가 정상 기동되도록 보정했다.
  JpaMinutesFavoriteRepositoryAdapterTest는 delete 쿼리 실행에 필요한 트랜잭션 경계를 테스트 메서드에 명시했다.
- 기준선 결과:
  `./gradlew :application:test` 통과
  `./gradlew :infrastructure:test` 재검증 결과, 컨텍스트/트랜잭션/fixture 성격의 실패는 정리되었고 `InfrastructureArchitectureTest` 1건만 남았다.
  남은 실패는 테스트 취약성이나 wiring 누락이 아니라 실제 `infrastructure -> application` 의존 위반을 잡는 ArchUnit rule이다.
- 남은 작업:
  `DefaultMeetingOrganizationResolver`, `HttpMeetingRealtimeSessionStarter/Stopper`, `JpaMinutesGenerationContextQueryAdapter`의 `application` 타입 의존을 제거하도록 포트/DTO/assembler 위치를 재설계해야 한다.
- Purpose: implement the QA-requested admin approval flow for user password reset requests, including pending request storage, admin list/count APIs, approval/rejection processing, and audit logging.
- Changed files:
  `domain/auth/PasswordResetRequest`,
  `domain/auth/PasswordResetRequestStatus`,
  `domain/auth/PasswordResetRequestRepositoryPort`,
  `application/auth/PasswordResetRequestUseCase`,
  `application/admin/AdminPasswordResetRequestUseCase`,
  `application/admin/PasswordResetRequestResult`,
  `app-api/auth/PasswordResetRequestController`,
  `app-api/admin/PasswordResetRequestAdminController`,
  related admin/auth DTOs and tests,
  `infrastructure/persistence/auth/*`,
  `common/exception/ErrorCode`,
  `docs/api-spec.md`,
  and this log.
- Behavior:
  Public password reset requests now create a persisted `PENDING` request row only when login ID and email match an existing `USER` account, while still returning the same generic accepted message.
  Admin APIs can list requests by optional status, fetch the pending-count badge value, approve a request to reset the target password hash to the initial password `1234`, or reject a request.
  Approved/rejected requests cannot be processed again; repeated approval/rejection attempts now fail with a conflict error.
  Approval revokes the target user's active sessions after the password reset and marks the request `APPROVED`; rejection marks the request `REJECTED`.
  Admin audit logs now capture request approval/rejection actions without storing plain passwords, hashes, JWTs, or refresh tokens.
- Verification:
  Passed `.\gradlew.bat :domain:compileJava :application:compileJava :app-api:compileJava :infrastructure:compileJava`
  Attempted targeted `:application:test` runs for the new auth/admin use cases, but `application:compileTestJava` is blocked by pre-existing unrelated failures in `EndMeetingUseCaseTest`, `TransferMeetingHostUseCaseTest`, `MinutesUseCaseTest`, and `GetMeetingTranscriptUseCaseTest`.
  Attempted `:app-api:compileTestJava`, but it is blocked by a pre-existing unrelated constructor mismatch in `app-api/src/test/java/com/meetbowl/api/meeting/MeetingControllerTest.java`.
  Attempted targeted `:infrastructure:test` for the new password-reset-request adapter test, but `infrastructure:compileTestJava` is blocked by pre-existing unrelated messaging test failures in `RabbitEventPublisherTest` and `RabbitDocumentIndexRequestedEventPublisherTest`.

2026-06-24 Admin audit log client IP capture

- Purpose: fix QA issue where new admin audit logs showed worker IP as `-` by resolving and persisting the requester client IP for all admin log-producing APIs.
- Changed files:
  `app-api/common/ApiHeaders`,
  `app-api/common/ClientIpResolver`,
  `app-api/admin/AdminUserController`,
  `app-api/admin/AdminOrganizationController`,
  `app-api/admin/AdminOrganizationMembersExcelController`,
  `app-api/admin/PasswordResetRequestAdminController`,
  `app-api/admin/MailRetentionPolicyController`,
  `application/admin/AdminAuditLogResult`,
  `app-api/admin/dto/AdminAuditLogResponse`,
  related admin/api tests,
  and this log.
- Behavior:
  New admin audit logs now resolve client IP in the order `X-Forwarded-For` first IP, then `X-Real-IP`, then `request.getRemoteAddr()`.
  Local development addresses such as `127.0.0.1` and `::1` are preserved instead of being dropped, so new rows store a non-blank IP whenever the servlet request provides one.
  The same resolver is applied across admin member create/update/delete/password reset, organization delete flows, password reset request approval/rejection, mail retention policy update, and organization-member Excel import.
  Audit log query results now expose `ipAddress`; old rows that never stored an IP still fall back to `-` at response mapping time.
  Sensitive fields such as passwords, JWTs, refresh tokens, and similar secrets remain excluded from audit payload handling.
- Verification:
  Passed `.\gradlew :app-api:compileJava`
  Attempted targeted audit-related Gradle tests, but `app-api:compileTestJava` is blocked by a pre-existing unrelated constructor mismatch in `app-api/src/test/java/com/meetbowl/api/meeting/MeetingControllerTest.java`.
  Attempted targeted application tests, but `application:compileTestJava` is blocked by pre-existing unrelated compile failures in `EndMeetingUseCaseTest`, `TransferMeetingHostUseCaseTest`, `MinutesUseCaseTest`, and `GetMeetingTranscriptUseCaseTest`.

2026-06-24 PasswordResetRequestControllerTest missing bean fix

- Purpose: fix `PasswordResetRequestControllerTest > createSuccess()` failing with `NoSuchBeanDefinitionException` on the current `main` branch.
- Changed files:
  `app-api/src/test/java/com/meetbowl/api/auth/PasswordResetRequestControllerTest.java`,
  and this log.
- Behavior:
  The test now mocks `AccessTokenValidationService` in addition to `PasswordResetRequestUseCase`.
  Current `@WebMvcTest` context loads `JwtAuthenticatedUserConverter`, and that bean requires `AccessTokenValidationService` via constructor injection. The password-reset request controller itself does not use that service directly, but the MVC/security slice now does, so the test must supply it.
- Verification:
  Passed `./gradlew :app-api:test --tests 'com.meetbowl.api.auth.PasswordResetRequestControllerTest' --no-daemon`

2026-06-24 Align auth controller structure with dev

- Purpose: remove the `main`-only password reset request controller split so auth API structure matches `dev` before merging back through `dev -> main`.
- Changed files:
  `app-api/src/main/java/com/meetbowl/api/auth/PasswordResetRequestController.java`,
  `app-api/src/test/java/com/meetbowl/api/auth/PasswordResetRequestControllerTest.java`,
  and this log.
- Behavior:
  Removed the separate `PasswordResetRequestController` and its dedicated WebMvc test.
  Password reset request handling remains covered by `AuthController` and `AuthControllerTest`, which is the `dev` branch structure.
  This also removes the duplicate test slice that had drifted from the shared MVC/security test setup.
- Verification:
  Passed `./gradlew :app-api:test --tests 'com.meetbowl.api.auth.AuthControllerTest' --tests 'com.meetbowl.api.auth.AuthTokenStatePrecisionTest' --no-daemon`

2026-06-24 application test fixture alignment after status/delete rule changes

- Purpose: fix `:application:test` failures caused by stale test fixtures after effective-status, soft-delete, and organization sort-order validation rules changed.
- Changed files:
  `application/src/test/java/com/meetbowl/application/admin/AdminOrganizationMasterDataUseCaseTest.java`,
  `application/src/test/java/com/meetbowl/application/admin/AdminUserManagementUseCaseTest.java`,
  `application/src/test/java/com/meetbowl/application/user/UserDirectoryUseCaseTest.java`,
  and this log.
- Behavior:
  Updated organization master-data test inputs so auto-generated code checks are isolated from the newer sort-order uniqueness rule instead of failing early on duplicated sort orders.
  Updated admin user management fixtures so default active users remain within the fixed test clock's active window, and delete tests stub `findByIdIncludingDeleted(...)` to match the current delete path.
  Adjusted the already-inactive delete fixture to represent a true tombstone (`deletedAt != null`), which is the current semantic used by `delete(...)` for "already deleted" conflicts.
  Updated user-directory expected ordering to match the current fake repository sort by user name.
- Verification:
  Passed targeted seven previously failing tests with `./gradlew :application:test ... --no-daemon`
  Passed full `./gradlew :application:test --no-daemon`

2026-06-24 JPA user search query alignment for Hibernate 7

- Purpose: fix `JpaUserRepositoryAdapterTest` failures caused by repository JPQL drift after search-condition changes and branch merge noise.
- Changed files:
  `infrastructure/src/main/java/com/meetbowl/infrastructure/persistence/user/JpaUserRepositoryAdapter.java`,
  `infrastructure/src/main/java/com/meetbowl/infrastructure/persistence/user/SpringDataUserRepository.java`,
  and this log.
- Behavior:
  Changed the repository adapter to pass the optional status filter to JPQL as a nullable enum name string instead of a nullable `UserStatus` enum object.
  Updated the JPQL status branch conditions to compare against `'ACTIVE'`, `'INACTIVE'`, and `'LOCKED'`, which avoids the Hibernate 7 parameter conversion failure triggered by `:status is null` plus enum comparisons in the same query.
  Restored the role-display keyword literals in the admin user search JPQL to proper Korean strings (`관리자`, `일반 사용자`) so partial search by displayed role text works again.
- Verification:
  Passed `./gradlew :infrastructure:test --tests 'com.meetbowl.infrastructure.persistence.user.JpaUserRepositoryAdapterTest' --no-daemon`
  Passed full `./gradlew :infrastructure:test --no-daemon`

2026-06-24 prod-seed profile for one-time bootstrap data

- Purpose: add an explicit deployment profile for seeding initial admin/user accounts without enabling the seed path in normal `prod` boots.
- Changed files:
  `app-api/src/main/resources/application.properties`,
  `app-api/src/main/java/com/meetbowl/api/config/ProdSeedDataInitializer.java`,
  `README.md`,
  `docs/api-spec.md`,
  and this log.
- Behavior:
  Added `spring.profiles.group.prod-seed=prod` so activating `prod-seed` also loads the normal production settings.
  Added a `prod-seed` `ApplicationRunner` that reuses the existing bootstrap account use case to create the initial `admin`, `user1`, and `user2` accounts with the same idempotent behavior as local startup.
  Documented `SPRING_PROFILES_ACTIVE=prod-seed` as the one-time operational path for initial data seeding after deployment.
- Verification:
  Passed `./gradlew :app-api:compileJava`
  Attempted `./gradlew test --no-daemon`, but it is blocked by a pre-existing unrelated `app-api/src/test/java/com/meetbowl/api/meeting/MeetingControllerTest.java` constructor mismatch during `:app-api:compileTestJava`.

2026-06-24 fold bootstrap seeding into prod profile

- Purpose: make deployment use a single `SPRING_PROFILES_ACTIVE=prod` setting while still creating the initial bootstrap accounts on startup.
- Changed files:
  `app-api/src/main/resources/application.properties`,
  `app-api/src/main/java/com/meetbowl/api/config/ProdSeedDataInitializer.java`,
  `README.md`,
  `docs/api-spec.md`,
  and this log.
- Behavior:
  Removed the separate `prod-seed` profile group and switched the bootstrap initializer to `@Profile("prod")`.
  The production boot path now both applies the normal prod datasource/Flyway settings and runs the idempotent account bootstrap that creates `admin`, `user1`, and `user2` if they do not already exist.
  Documentation now tells operators to use `SPRING_PROFILES_ACTIVE=prod` only.
- Verification:
  Passed `./gradlew :app-api:compileJava --no-daemon`

2026-06-24 production CORS origin configuration

- Purpose: allow browser calls from a changing deployed frontend URL without hardcoding a single origin in code.
- Changed files:
  `app-api/src/main/java/com/meetbowl/api/config/SecurityConfig.java`,
  `app-api/src/main/resources/application-prod.properties`,
  `README.md`,
  and this log.
- Behavior:
  CORS now reads allowed origin patterns from `MEETBOWL_CORS_ALLOWED_ORIGIN_PATTERNS`, parsed as a comma-separated list.
  When the variable is unset, the server falls back to local development origins (`http://localhost:*`, `http://127.0.0.1:*`).
  Production deploys should set the environment variable to the frontend origin(s), for example `https://app.meetbowl.com,https://*.vercel.app`.
- Verification:
  Passed `./gradlew :app-api:compileJava --no-daemon`

2026-06-24 GitHub Actions SSM fetch for production CORS

- Purpose: make the deployment pipeline actually load the CORS origin patterns from AWS Systems Manager before starting the deployed app.
- Changed files:
  `.github/workflows/backend-deploy.yml`,
  `README.md`,
  and this log.
- Behavior:
  The deploy job now reads the SSM parameter name from the `MEETBOWL_CORS_ALLOWED_ORIGIN_PATTERNS_SSM_NAME` GitHub secret, fetches the decrypted value with `aws ssm get-parameter`, masks it in the job log, and passes it through to the EC2 deploy command as `MEETBOWL_CORS_ALLOWED_ORIGIN_PATTERNS`.
  This keeps the runtime CORS allowlist in SSM while ensuring the EC2 deployment path actually receives it.
- Verification:
  Ran `git diff --check` successfully to confirm the workflow and documentation edits are clean.

2026-06-24 bind CORS SSM path directly in deploy workflow

- Purpose: remove the unnecessary GitHub secret hop and read the production CORS allowlist from a fixed SSM parameter path.
- Changed files:
  `.github/workflows/backend-deploy.yml`,
  `README.md`,
  and this log.
- Behavior:
  The deploy job now reads `/meetbowl/prod/be/MEETBOWL_CORS_ALLOWED_ORIGIN_PATTERNS` directly from SSM instead of requiring `MEETBOWL_CORS_ALLOWED_ORIGIN_PATTERNS_SSM_NAME` in GitHub secrets.
  This matches the current operational setup where the parameter already exists in Parameter Store and no extra repo secret should be needed.
- Verification:
  Pending re-run of the GitHub Actions deploy workflow.

2026-06-24 Spring Boot 4 Flyway auto-configuration dependency fix

- Purpose: restore Flyway migration auto-configuration after upgrading to Spring Boot 4, where Flyway auto-configuration is no longer bundled in the general `spring-boot-autoconfigure` module.
- Changed files:
  `infrastructure/build.gradle`,
  and this log.
- Behavior:
  Replaced the direct `flyway-core` dependency with `spring-boot-starter-flyway` so the application runtime includes Spring Boot's `spring-boot-flyway` module and `FlywayAutoConfiguration`.
  Kept `flyway-mysql` explicitly because Flyway 11 requires the separate MariaDB/MySQL database plugin.
- Verification:
  Confirmed `spring-boot-flyway:4.0.6` on `:app-api:runtimeClasspath` with Gradle dependency insight.
  Confirmed `flyway-mysql:11.14.1` remains on `:app-api:runtimeClasspath`.
  Passed `./gradlew :app-api:compileJava --no-daemon`.

2026-06-24 Admin audit log schema alignment

- Purpose: fix application startup failure caused by Hibernate schema validation detecting that `admin_audit_logs.target_login_id` exists in the entity mapping but not in the Flyway-managed database schema.
- Changed files:
  `app-api/src/main/resources/db/migration/V2__add_target_login_id_to_admin_audit_logs.sql`,
  and this log.
- Behavior:
  Added a Flyway migration that introduces the missing `target_login_id` column to `admin_audit_logs`.
  The change keeps the existing baseline migration intact and lets Flyway upgrade both fresh and already-provisioned databases without breaking schema validation.
 - Verification:
  Reviewed the `AdminAuditLogEntity` mapping and confirmed `targetLoginId` is a mapped column.
  Confirmed `app-api/src/main/resources/db/migration/V1__baseline.sql` did not contain the column.

2026-06-24 Admin audit log target name schema alignment

- Purpose: fix the next Hibernate schema validation failure after `target_login_id` was added, where `admin_audit_logs.target_name` was still missing from the Flyway-managed schema.
- Changed files:
  `app-api/src/main/resources/db/migration/V3__add_target_name_to_admin_audit_logs.sql`,
  and this log.
- Behavior:
  Added a second Flyway migration that introduces the missing `target_name` column to `admin_audit_logs`.
  This keeps the schema repair incremental and safe for databases that have already applied `V2`.
 - Verification:
  Rechecked `AdminAuditLogEntity` and confirmed `targetName` is also a mapped column.
  Confirmed the baseline migration still does not contain the column, so a forward migration is required rather than a baseline rewrite.

2026-06-24 Password reset request table schema alignment

- Purpose: fix application startup failure caused by Hibernate schema validation detecting that the `password_reset_requests` table was missing entirely from the Flyway-managed schema.
- Changed files:
  `app-api/src/main/resources/db/migration/V4__create_password_reset_requests.sql`,
  and this log.
- Behavior:
  Added the missing `password_reset_requests` table with the columns and nullability required by `PasswordResetRequestEntity` and the domain model.
  Added a supporting index on `(status, requested_at)` to match the repository access pattern for status-filtered listing ordered by most recent request first.
 - Verification:
  Reviewed `PasswordResetRequestEntity` and `PasswordResetRequest` to align the table definition with the persisted fields.
  Kept the existing migrations intact so already-applied environments can upgrade forward without rebuilding the baseline.

2026-06-24 User soft-delete schema alignment

- Purpose: fix Hibernate schema validation failure caused by the `users` table missing the `deleted_at` column used by the current `UserEntity` and `User` domain model.
- Changed files:
  `app-api/src/main/resources/db/migration/V5__add_deleted_at_to_users.sql`,
  and this log.
- Behavior:
  Added the missing soft-delete timestamp column to `users` without changing the original baseline migration.
  This preserves existing upgrade history while bringing the schema in line with the entity mapping that already treats deleted users as tombstoned records.
- Verification:
  Reviewed `UserEntity` and `User` to confirm `deletedAt` is a persisted field and not an in-memory-only property.
  Confirmed the current baseline `users` table definition does not include `deleted_at`.

2026-06-24 meeting.ended RabbitMQ JSON serialization fix

- Purpose: fix meeting termination succeeding in MariaDB while the `meeting.ended` event failed before RabbitMQ transmission because the default `SimpleMessageConverter` could not convert `EventEnvelope`.
- Changed files:
  `app-api/src/main/java/com/meetbowl/api/messaging/RabbitMqMeetingEndedEventPublisher.java`,
  `app-api/src/test/java/com/meetbowl/api/messaging/RabbitMqMeetingEndedEventPublisherTest.java`,
  `infrastructure/src/main/java/com/meetbowl/infrastructure/messaging/RabbitEventPublisher.java`,
  `infrastructure/src/main/java/com/meetbowl/infrastructure/messaging/meeting/MeetingEndedMessage.java`,
  `infrastructure/src/test/java/com/meetbowl/infrastructure/messaging/RabbitEventPublisherTest.java`,
  and this log.
- Behavior:
  Replaced the direct `RabbitTemplate.convertAndSend(EventEnvelope)` call with the shared `RabbitEventPublisher`, which serializes the envelope to JSON bytes before calling `RabbitTemplate.send`.
  Added a dedicated `MeetingEndedMessage` contract DTO and reused `EventTypes.MEETING_ENDED`.
  Added shared publisher support for preserving a caller-provided correlation ID in both the event envelope and RabbitMQ message header.
  The existing meeting termination behavior remains unchanged: database termination state is retained if event publication fails.
- Excluded scope:
  Did not add an operational event replay endpoint or transactional outbox.
  Meetings that already reached `ENDED` while publication failed still require a separate replay operation because the termination use case intentionally does not republish for an already-ended meeting.
- Verification:
  Passed `./gradlew :infrastructure:test --tests com.meetbowl.infrastructure.messaging.RabbitEventPublisherTest --tests com.meetbowl.infrastructure.architecture.InfrastructureArchitectureTest :app-api:compileJava --no-daemon`.
  Confirmed JSON serialization of the `meeting.ended` payload, persistent delivery mode, message ID, and caller-provided correlation ID.
  Full `:application:test` compilation remains blocked by pre-existing test stubs that do not implement `MeetingRepositoryPort.findActiveByAttendees`.
  Full `:app-api:test` compilation remains blocked by the pre-existing `MeetingControllerTest` constructor argument mismatch.
  Full `:infrastructure:test` remains blocked by pre-existing meeting context tests missing a `MeetingAttendeeOverlapGuard` bean.

2026-06-24 meeting.ended Transactional Outbox delivery guarantee

- Purpose: remove the remaining loss window and transaction-ordering problem between the meeting `ENDED` database update and the required `meeting.ended` RabbitMQ event.
- Changed files:
  Added the meeting-ended domain event/outbox/publisher ports, the application Outbox publisher use case, JPA Outbox entity/repository/adapter, RabbitMQ adapter, scheduler, and Flyway V6 migration.
  Updated `EndMeetingUseCase`, the shared Rabbit publisher/configuration, end-meeting response naming, related tests, architecture/API/convention documents, and this log.
- Behavior:
  Meeting termination and `meeting_ended_outbox` insertion now commit in the same MariaDB transaction.
  The API returns `meetingEndedEventQueued` to distinguish durable queueing from actual broker publication.
  The scheduler locks ready Outbox rows, preserves the original `eventId`, and removes a row only after RabbitMQ publisher ACK with no returned-message routing failure.
  Broker NACK, unroutable messages, timeouts, and connection failures remain in the Outbox and retry with exponential backoff capped at 300 seconds.
  A crash after broker ACK but before the Outbox transaction commits can redeliver the same `eventId`; the AI consumer's eventId idempotency handles that at-least-once boundary.
  The RabbitMQ adapter now lives in `infrastructure`, removing the previous `app-api -> infrastructure` source dependency.
- Verification:
  Passed application source compilation and targeted `EndMeetingUseCaseTest` / `PublishMeetingEndedOutboxUseCaseTest`.
  Passed Rabbit publisher ACK/NACK/return tests and the meeting-ended adapter test.
  Passed domain, application, infrastructure, and app-api ArchUnit tests.
  Passed Spring application context startup with the new Outbox entity.
  Full `./gradlew test` still has 36 pre-existing infrastructure meeting-context failures because those test slices do not provide `MeetingAttendeeOverlapGuard`; all app-api, application, common, and domain tests completed before that infrastructure failure group.
  Spotless reports only unrelated existing violations after the changed files were aligned to its generated clean output.

2026-06-24 minutes-generation-context transcript segments expansion

- Purpose: let the AI minutes workflow consume the authoritative Final Transcript as a structured segment list instead of re-splitting only the flattened `rawTranscript` string.
- Changed files:
  `domain/src/main/java/com/meetbowl/domain/minutes/MinutesGenerationContext.java`,
  `application/src/main/java/com/meetbowl/application/minutes/GetMinutesGenerationContextUseCase.java`,
  `application/src/main/java/com/meetbowl/application/minutes/MinutesGenerationContextResult.java`,
  `application/src/test/java/com/meetbowl/application/minutes/GetMinutesGenerationContextUseCaseTest.java`,
  `infrastructure/src/main/java/com/meetbowl/infrastructure/minutes/JpaMinutesGenerationContextQueryAdapter.java`,
  `app-api/src/main/java/com/meetbowl/api/minutes/MinutesGenerationContextResponse.java`,
  `docs/api-spec.md`,
  and this log.
- Behavior:
  Expanded the internal `minutes-generation-context` contract to include transcript segment metadata (`segmentId`, `sequence`, `language`, `sourceText`, `startedAtMs`, `endedAtMs`) alongside the existing flattened `rawTranscript`.
  Reused the already loaded transcript repository rows in the JPA adapter so the API returns the same sequence-ordered Final Transcript evidence and the assembled string in one response.
  Kept `rawTranscript` in the contract for backward compatibility while enabling the AI server to switch to segment-based evidence extraction without a second BE API call.
- Verification:
  Passed `./gradlew :application:test --tests com.meetbowl.application.minutes.GetMinutesGenerationContextUseCaseTest :app-api:compileJava --no-daemon`.
  Confirmed the new context result preserves participant metadata, raw transcript text, and transcript segment payloads together.

2026-06-30 admin organization members Excel download controller test alignment

- Purpose: fix CI `./gradlew test --no-daemon` compilation failure after `AdminOrganizationMembersExcelUseCase.export` changed to require the current admin affiliate/organization id.
- Changed files:
  `app-api/src/test/java/com/meetbowl/api/admin/AdminOrganizationMembersExcelControllerTest.java`,
  and this log.
- Behavior:
  Updated the download controller test mock from the removed no-argument `export()` contract to `export(ORGANIZATION_ID)`.
  Added verification that the controller passes the authenticated admin's organization id to the UseCase, matching the existing production controller behavior.
- Excluded scope:
  Did not change production code or API behavior.
- Verification:
  Passed targeted controller/dashboard/mail/infrastructure tests.
  Passed full `./gradlew test --no-daemon`.

2026-06-30 infrastructure meeting test context external invitee dependency alignment

- Purpose: unblock full `./gradlew test --no-daemon` after meeting UseCases gained external invitee synchronization and invitation-mail dependencies that the infrastructure meeting slice tests did not register.
- Changed files:
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/meeting/MeetingLifecycleTest.java`,
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/meeting/MeetingReservationConcurrencyTest.java`,
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/meetingroom/MeetingRoomReservationsTest.java`,
  `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/meetingroom/MeetingRoomStatusTest.java`,
  and this log.
- Behavior:
  Added the real external invitee repository adapter and sync service to the meeting persistence test contexts.
  Added a mocked `SendMeetingExternalInvitationMailUseCase` because these tests verify meeting and meeting-room persistence behavior, not mail dispatch side effects.
  Added a UTC `Clock` bean because these slice tests do not load the app-api runtime bean configuration.
- Excluded scope:
  Did not change production code or external invitation mail behavior.
- Verification:
  Passed targeted infrastructure meeting tests.
  Passed full `./gradlew test --no-daemon`.

2026-06-30 domain mail test external recipient contract alignment

- Purpose: unblock full `./gradlew test --no-daemon` after current `Mail` factory methods require the external recipient list separately from internal recipient user ids.
- Changed files:
  `domain/src/test/java/com/meetbowl/domain/mail/MailTest.java`,
  and this log.
- Behavior:
  Updated domain mail tests that do not use external mail recipients to pass `List.of()` for the new `externalRecipients` argument.
  Preserved the existing test scenarios for recipient duplication, body length validation, restore state validation, attachment ownership, and delivery lifecycle.
- Excluded scope:
  Did not change domain production code or mail behavior.
- Verification:
  Passed targeted domain mail test.
  Passed full `./gradlew test --no-daemon`.

2026-06-30 admin dashboard time-slot usage test expectation alignment

- Purpose: unblock full `./gradlew test --no-daemon` after the Excel controller compile fix exposed a stale dashboard summary assertion.
- Changed files:
  `application/src/test/java/com/meetbowl/application/admin/AdminDashboardSummaryUseCaseTest.java`,
  and this log.
- Behavior:
  Aligned `timeSlotUsage` assertions with the existing `AdminDashboardSummaryUseCase` contract where this series counts meetings by reservation start time.
  Kept `timeSlotOccupancyUsage` assertions as the overlapping-slot occupancy series.
- Excluded scope:
  Did not change dashboard production logic or API response shape.
- Verification:
  Passed targeted admin dashboard summary test.
  Passed full `./gradlew test --no-daemon`.

2026-06-30 notification SSE error response content-type fix

- Purpose: diagnose and fix `No converter for [class com.meetbowl.common.response.ApiResponse] with preset Content-Type 'text/event-stream'` during notification SSE subscription failures.
- Changed files:
  `app-api/src/main/java/com/meetbowl/api/common/GlobalExceptionHandler.java`,
  `app-api/src/test/java/com/meetbowl/api/notification/NotificationControllerTest.java`,
  `app-api/src/test/java/com/meetbowl/api/config/SecurityConfigTest.java`,
  and this log.
- Behavior:
  Forced all common exception-handler responses to `application/json` so an exception raised after an SSE handler mapping is selected does not try to serialize `ApiResponse` as `text/event-stream`.
  Added a regression test proving `/api/v1/notifications/subscribe` authentication failure returns the common JSON error instead of failing message conversion.
  Added a security/CORS regression test proving the SSE endpoint accepts `?token=` access tokens and returns the expected CORS header for the default local frontend origin.
- Findings:
  The frontend already opens SSE with `new EventSource('/api/v1/notifications/subscribe?token=...')`, which matches the backend query-token resolver.
  Default backend CORS allows only `http://localhost:*` and `http://127.0.0.1:*`; deployed frontend origins still require `MEETBOWL_CORS_ALLOWED_ORIGIN_PATTERNS`.
- Excluded scope:
  Did not change the SSE event payload format or token refresh behavior for long-lived EventSource connections.
- Verification:
  Passed `./gradlew :app-api:test --tests com.meetbowl.api.notification.NotificationControllerTest --tests com.meetbowl.api.config.SecurityConfigTest --no-daemon`.
  Passed full `./gradlew test --no-daemon`.

2026-06-30 seed admin account policy update

- Purpose: restore the default seed policy to a single `admin` administrator and migrate away from the temporary `admin1`/`admin2` seed accounts on prod.
- Changed files:
  `application/src/main/java/com/meetbowl/application/auth/InitializeLocalAccountsUseCase.java`,
  `application/src/test/java/com/meetbowl/application/auth/InitializeLocalAccountsUseCaseTest.java`,
  `app-api/src/main/resources/db/migration/V10__restore_single_seed_admin.sql`,
  `README.md`,
  `docs/api-spec.md`,
  and this log.
- Behavior:
  Changed local/prod bootstrap back to `admin`, `user1`, and `user2` under one seed affiliate named `한화 시스템`.
  The seed `admin` display name is now `한화 시스템 관리자`.
  Added Flyway migration `V10` to rename the existing `admin` affiliate to `한화 시스템`, rename `admin` to `한화 시스템 관리자`, and delete existing `admin1`/`admin2` rows.
  Kept seed password `1234`, seed users `user1`/`user2`, and the seed affiliate code `LOCAL-1`.
- Excluded scope:
  Did not merge or delete orphaned affiliate rows that may have been created by the temporary two-admin seed policy.
- Verification:
  Passed `./gradlew :application:test --tests com.meetbowl.application.auth.InitializeLocalAccountsUseCaseTest --no-daemon`.
