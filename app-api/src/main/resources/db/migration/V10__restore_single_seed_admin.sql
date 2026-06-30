-- 이전 prod seed 정책에서 생성된 admin1/admin2는 기본 관리자 정책에서 제외되므로 삭제한다.
-- admin 계정의 소속 계열사 row만 이름을 바꾸면 해당 조직에 속한 기존 사용자들의 조직 표시명도 함께 바뀐다.
UPDATE affiliates a
JOIN users u ON u.affiliate_id = a.id
SET a.name = '한화 시스템',
    a.status = 'ACTIVE',
    a.updated_at = CURRENT_TIMESTAMP(6)
WHERE u.login_id = 'admin'
  AND u.deleted_at IS NULL;

UPDATE users
SET name = '한화 시스템 관리자',
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE login_id = 'admin'
  AND deleted_at IS NULL;

DELETE FROM users
WHERE login_id IN ('admin1', 'admin2');
