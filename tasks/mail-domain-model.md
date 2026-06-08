# 메일 도메인 모델

## 범위

내부 메일의 본문, 발송 상태, 사용자별 메일함 상태와 첨부파일 메타데이터를 관리한다.
첨부파일 원본은 S3 호환 Object Storage에 저장한다.

## 애그리거트

`Mail`을 애그리거트 루트로 사용한다.

```text
Mail
├── MailboxEntry (발신자와 수신자의 메일함 상태)
└── MailAttachment (Object Storage 파일 메타데이터)
```

### Mail

- 다른 도메인의 Entity를 직접 참조하지 않고 조직과 발신자를 UUID로 참조한다.
- 제목, 본문, 메일 유형, 본문 유형, 관련 리소스와 발송 상태를 관리한다.
- 발신자에게 `SENT` 메일함 항목 하나를 생성하고, 각 수신자에게 `INBOX` 항목을 생성한다.
- 동일한 수신자를 중복으로 지정할 수 없다.
- 발송 상태는 `REQUESTED -> SENT` 또는 `REQUESTED -> FAILED`로만 변경할 수 있다.

### MailboxEntry

- 메일함 소유자 UUID와 메일함 유형을 저장한다.
- 사용자별 읽음/안읽음, 휴지통 이동/복구, 영구 삭제 상태를 독립적으로 관리한다.
- 휴지통에 있는 메일만 영구 삭제할 수 있다.

### MailAttachment

- object key, 파일명, MIME type, 크기와 업로더 UUID만 저장한다.
- 파일 원본은 DB에 저장하지 않는다.

## 영속성 모델

| Entity | Table | 주요 제약 조건 |
|---|---|---|
| `MailEntity` | `mail` | 조직, 발신자, 발송 상태와 요청 시각 인덱스 |
| `MailboxEntryEntity` | `mailbox_entry` | `(mail_id, owner_user_id, mailbox_type)` 유니크 제약 |
| `MailAttachmentEntity` | `mail_attachment` | object key 유니크 제약 |

영속성 Entity는 `infrastructure` 내부에서만 사용한다.
Domain 모델과 영속성 Entity 간 변환은 Persistence Adapter 경계에서 수행한다.
