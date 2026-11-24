# 경매장 (프로그래머스 백엔드 데브코스 15기 2차 5팀)

가상 포인트로 물건을 사고파는 개인 간 경매 플랫폼입니다.

- 기획·도메인·규칙·API: [docs/프로젝트_기술서.md](docs/프로젝트_기술서.md)
- 모듈 간 계약 (포인트 서비스, 거래 생성, 이벤트, 에러코드): [docs/모듈_계약서.md](docs/모듈_계약서.md)
- **담당자별 할 일과 순서: [docs/담당자별_작업가이드.md](docs/담당자별_작업가이드.md)**

## 기술 스택

Java 21 · Spring Boot 4.2 · Spring Data JPA · MySQL 8.4 · Spring Security · springdoc · Testcontainers

## 실행

준비물: JDK 21, Docker Desktop (실행 중이어야 함)

```bash
./gradlew bootRun
```

- `compose.yaml`의 MySQL을 Spring Boot가 자동으로 띄우고 연결합니다. DB 툴 접속은 `localhost:13306`, 계정 `auction` / `auction`.
- Swagger UI: http://localhost:8080/swagger-ui.html

## 테스트

```bash
./gradlew test
```

Testcontainers가 MySQL 컨테이너를 띄우므로 Docker가 실행 중이어야 합니다.

## 패키지 구조

```
com.team5.auction
├── common        응답(ApiResponse, PageResponse), 예외(ErrorCode, BusinessException),
│                 보안(SecurityConfig, JWT 자리), 설정(Clock, 정책값, Swagger), BaseEntity, Address
├── user          회원·인증                         담당: 회원·공통
├── point         포인트·변동내역·수수료           담당: 포인트
├── category      카테고리                          담당: 경매
├── auction       경매, 종료 스케줄러               담당: 경매
├── bid           입찰, 즉시구매, 입찰 단위         담당: 입찰
├── trade         거래, 송장·자동확정 스케줄러      담당: 거래
└── notification  알림 (이벤트 수신)                담당: 회원·공통·알림
```

도메인마다 `controller / service / repository / domain / dto / event / exception` 패키지를 둡니다.

## 지금 상태 (뼈대)

- 엔티티 8개, Enum, Repository, 도메인별 에러코드는 만들어져 있습니다.
- `UnsupportedOperationException("TODO(담당)")`이 있는 곳은 담당자가 구현합니다.
  `grep -rn "TODO(" src/main` 으로 내 할 일을 찾을 수 있습니다.
- JWT 필터는 자리만 있습니다. 구현 전까지 로그인이 필요한 API는 401을 응답합니다.

## 규칙

- 현재 시각은 `LocalDateTime.now(clock)`으로 얻습니다. `LocalDateTime.now()`를 직접 쓰지 않습니다.
- 금액은 `long`, 비율 계산은 `BigDecimal`로만 합니다.
- 유저 잔액은 `PointService`만 바꿉니다.
- 계약서에 있는 시그니처는 팀 합의 없이 바꾸지 않습니다.
