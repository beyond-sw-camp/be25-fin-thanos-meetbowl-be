# 메일 API 설계 및 구현

## 범위

- 일반 내부 메일 발송
- 받은/보낸/휴지통 목록 조회
- 메일 상세 조회
- 읽음/안읽음 변경
- 휴지통 이동, 복구, 영구 삭제

검색, 백업, 첨부파일과 RabbitMQ 발송 완료 처리는 각각 검색 조건, Object Storage, 메시징 Adapter 계약이 확정된 뒤 구현한다.

## 계층 흐름

```text
MailController
  -> mail UseCase
  -> Mail / MailboxEntry
  -> MailRepositoryPort / MailboxEntryRepositoryPort
  -> JPA Adapter
```

메일 발송과 발신자·수신자 메일함 생성은 하나의 트랜잭션으로 처리한다. 사용자별 읽음 및 삭제 상태는 `MailboxEntry`만 변경하며 공용 `Mail` 본문은 변경하지 않는다.
