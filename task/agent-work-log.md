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
