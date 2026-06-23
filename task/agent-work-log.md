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
- 후속 참고: 프론트엔드에서도 같은 에러 코드를 사용해 팝업 오픈 전 알림과 로비 안내 메시지를 표시하도록 함께 반영했다.
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
