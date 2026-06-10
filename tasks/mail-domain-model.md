# 메일 도메인 모델

## 범위

내부 메일의 본문, 발송 상태, 사용자별 메일함 상태와 첨부파일 메타데이터를 관리한다.
첨부파일 원본은 S3 호환 Object Storage에 저장한다.

## 애그리거트

메일 본문과 발송 상태는 `Mail`, 사용자별 메일함 상태는 `MailboxEntry`를 각각 애그리거트 루트로 사용한다.

```text
Mail
└── MailAttachment (Object Storage 파일 메타데이터)

MailboxEntry
└── mailId로 Mail 참조
```

### Mail

- 다른 도메인의 Entity를 직접 참조하지 않고 조직과 발신자를 UUID로 참조한다.
- 제목, 본문, 메일 유형, 본문 유형, 관련 리소스와 발송 상태를 관리한다.
- 수신자 UUID 목록을 발송 계약의 일부로 관리한다.
- 동일한 수신자를 중복으로 지정할 수 없다.
- 발송 상태는 `DRAFT -> REQUESTED -> SENT/FAILED`로 변경한다.
- 재시도는 `FAILED -> RETRYING -> SENT/FAILED` 전이와 재시도 횟수로 관리한다.
- 첨부파일은 `DRAFT` 상태에서만 등록하고 발송 요청 전에 확정한다.
- 본문은 최대 1,000,000자로 제한하고 DB에는 `MEDIUMTEXT`로 저장한다.

### MailboxEntry

- 메일 UUID, 메일함 소유자 UUID와 메일함 유형을 저장한다.
- 사용자별 읽음/안읽음, 휴지통 이동/복구, 영구 삭제 상태를 독립적으로 관리한다.
- 휴지통에 있는 메일만 영구 삭제할 수 있다.
- 사용자 상태 변경이 다른 수신자나 메일 본문 갱신과 충돌하지 않도록 `Mail`과 분리한다.

### MailAttachment

- object key, 파일명, MIME type, 크기와 업로더 UUID만 저장한다.
- 파일 원본은 DB에 저장하지 않는다.

## 영속성 모델

| Entity | Table | 주요 제약 조건 |
|---|---|---|
| `MailEntity` | `mail` | 조직, 발신자, 발송 상태와 요청 시각 인덱스 |
| 수신자 목록 | `mail_recipient` | `(mail_id, recipient_user_id)` 유니크 제약 |
| `MailboxEntryEntity` | `mailbox_entry` | `(mail_id, owner_user_id, mailbox_type)` 유니크 제약 |
| `MailAttachmentEntity` | `mail_attachment` | object key 유니크 제약 |

영속성 Entity는 `infrastructure` 내부에서만 사용한다.
Domain 모델과 영속성 Entity 간 변환은 Persistence Adapter 경계에서 수행한다.
가변 Entity는 `version` 컬럼을 사용해 낙관적 락을 적용한다.
운영 스키마는 Flyway migration으로 관리하고 Hibernate는 `validate`만 수행한다.
