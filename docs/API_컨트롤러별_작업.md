# API 컨트롤러별 작업 (5팀)

> 기준: [프로젝트_기술서.md](프로젝트_기술서.md) 8장 API 29개, [모듈_계약서.md](모듈_계약서.md)
> 컨트롤러 하나를 작업 하나로 봅니다. 카드 하나만 읽고 바로 시작할 수 있게 정리했습니다.
> 요청·응답 필드는 **초안**입니다. 담당자가 구현하면서 다듬되, 바꾼 내용은 Swagger에 반영합니다.

---

## 1. 할당표

| # | 컨트롤러 | API | 담당 | 함께 맡는 서비스·배치 |
| --- | --- | --- | --- | --- |
| C1 | `AuthController` | 2 | 회원·공통·알림 | JWT 구현, 테스트 데이터 |
| C2 | `UserController` | 2 | 회원·공통·알림 | |
| C3 | `NotificationController` | 2 | 회원·공통·알림 | 알림 이벤트 리스너 |
| C4 | `PointController` | 2 | 포인트 | `PointService`, `FeeCalculator` |
| C5 | `CategoryController` | 1 | 포인트 | |
| C6 | `AdminCategoryController` | 3 | 포인트 | |
| C7 | `AuctionController` | 6 | 경매 | 경매 종료 스케줄러 |
| C8 | `AdminAuctionController` | 1 | 경매 | |
| C9 | `BidController` | 4 | 팀장(입찰) | `BidUnitPolicy`, Auction 입찰 메서드, 동시성 |
| C10 | `AdminBidController` | 1 | 팀장(입찰) | |
| C11 | `TradeController` | 4 | 거래 | `TradeService.createTrade`, 스케줄러 3개 |
| C12 | `AdminTradeController` | 1 | 거래 | |

| 담당 | 컨트롤러 | API 수 |
| --- | --- | --- |
| 회원·공통·알림 | C1, C2, C3 | 6 |
| 포인트 | C4, C5, C6 | 6 |
| 경매 | C7, C8 | 7 |
| 팀장(입찰) | C9, C10 | 5 |
| 거래 | C11, C12 | 5 |
| **합계** | **12개** | **29** |

> **바뀐 점**: 처음 가이드에서는 카테고리가 경매 담당이었습니다. 경매 담당 API가 11개로 몰려 있어서, 카테고리(C5, C6)를 API가 2개뿐인 포인트 담당에게 옮겼습니다.

### 모든 카드에 공통

- 위치: `{도메인}/controller/`, DTO는 `{도메인}/dto/`. 요청 DTO에는 `@Valid` 검증을 붙입니다.
- 응답: `ApiResponse.ok(data)`. 생성은 `201`, 나머지는 `200`.
- 로그인 유저: `@AuthenticationPrincipal LoginUser loginUser`. 비로그인도 되는 API에서는 `null`일 수 있습니다.
- 목록: `?page=0&size=20`을 받고 `PageResponse`로 응답합니다.
- 운영자 컨트롤러는 `/api/admin/**`로 시작하므로 `SecurityConfig`가 ADMIN만 통과시킵니다.
- 시간은 `LocalDateTime.now(clock)`.
- **완료 기준(공통)**: Swagger에서 호출 성공 · 실패 케이스가 표의 에러코드로 응답 · 서비스 테스트 작성 · `dev`로 PR

---

## 2. 회원·공통·알림

### C1. `AuthController` — `user/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/auth/signup` | 전체 | `email`, `password`, `nickname`, `phone` | 201 `{ userId }` | `EMAIL_DUPLICATED`, `NICKNAME_DUPLICATED` |
| POST | `/api/auth/login` | 전체 | `email`, `password` | `{ accessToken, tokenType: "Bearer", expiresIn }` | `LOGIN_FAILED` |

**할 일**
- JWT 라이브러리 추가(`jjwt` 추천), `JwtTokenProvider`, `JwtAuthenticationFilter` 구현. 시크릿은 환경변수.
- 비밀번호는 `PasswordEncoder`로 암호화. 가입 시 role = USER, 잔액 0, 배송지 없음.
- 로그인 실패는 이메일·비밀번호 구분 없이 같은 메시지.
- **테스트 데이터**(local 프로필): 일반 유저 3명(포인트 충전, 배송지 포함), **운영자 1명**, 카테고리 몇 개, 마감 임박 경매 1~2개(시연용).

**먼저**: **3일차까지**. 로그인이 되어야 다른 사람이 Swagger로 테스트할 수 있습니다.

### C2. `UserController` — `user/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/users/me` | USER | | `{ id, email, nickname, phone, role, availableBalance, lockedBalance, address }` | |
| PATCH | `/api/users/me` | USER | `nickname?`, `phone?`, `address?`, `clearAddress?` | 위와 같음 | `NICKNAME_DUPLICATED`, `ADDRESS_REQUIRED_WHILE_LOCKED` |

- `address`: `{ recipientName, recipientPhone, zipCode, address1, address2 }`. 없으면 `null`.
- PATCH는 보낸 필드만 바꿉니다. 배송지 삭제는 `clearAddress: true`로 합니다(PATCH의 `null`은 "안 바꿈"과 구분이 안 되기 때문).
- 묶인 포인트가 있으면 배송지를 지울 수 없습니다(R-66).

### C3. `NotificationController` — `notification/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/notifications` | USER | `page`, `size` | Page `{ id, type, message, refType, refId, read, createdAt }` | |
| PATCH | `/api/notifications/{id}/read` | USER | | 200 | `NOTIFICATION_NOT_FOUND` (남의 알림도) |

**함께 할 일**: `NotificationEventListener`에서 이벤트 7종을 받아 저장(계약서 6장 표). 문구는 여기서 만듭니다. 6~7일차.

---

## 3. 포인트

### C4. `PointController` — `point/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/users/me/charge` | USER | `amount` | `{ availableBalance, lockedBalance }` | `CHARGE_AMOUNT_INVALID` |
| GET | `/api/users/me/balance-histories` | USER | `page`, `size` | Page `{ id, type, amount, availableAfter, lockedAfter, refType, refId, createdAt }` | |

**함께 할 일 (컨트롤러보다 먼저)**
- `PointServiceImpl`: `charge`, `lockForBid` **4일차까지**(입찰이 기다림), `confirmPurchase`, `refund`, `unlockBid` **6일차까지**(거래·경매가 기다림).
- `FeeCalculatorImpl` **5일차까지**(거래가 기다림). `BigDecimal`, 원 단위 미만 버림.
- `User`의 잔액 변경 메서드.

### C5. `CategoryController` — `category/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/categories` | 전체 | | `[ { id, name } ]` (페이지 없음) | |

### C6. `AdminCategoryController` — `category/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/admin/categories` | ADMIN | `name` | 201 `{ id, name }` | `CATEGORY_NAME_DUPLICATED` |
| PATCH | `/api/admin/categories/{id}` | ADMIN | `name` | `{ id, name }` | `CATEGORY_NOT_FOUND`, `CATEGORY_NAME_DUPLICATED` |
| DELETE | `/api/admin/categories/{id}` | ADMIN | | 200 | `CATEGORY_NOT_FOUND`, `CATEGORY_IN_USE` (R-64) |

- `CategoryService`에 `getById(id)`를 만들어 두면 경매 등록에서 씁니다. **3일차까지**(경매가 기다림). 그 전에는 테스트 데이터의 카테고리로 개발할 수 있습니다.

---

## 4. 경매

### C7. `AuctionController` — `auction/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/auctions` | USER | `categoryId`, `title`, `description`, `startPrice`, `buyNowPrice?`, `durationDays`, `imageUrls[]` | 201 `{ auctionId }` | `AUCTION_PERIOD_INVALID`, `PRICE_UNIT_INVALID`, `BUY_NOW_PRICE_INVALID`, `IMAGE_COUNT_INVALID`, `CATEGORY_NOT_FOUND` |
| GET | `/api/auctions` | 전체 | `categoryId?`, `status?`(기본 ACTIVE), `sort?`, `page`, `size` | Page 목록 항목 ① | |
| GET | `/api/auctions/{id}` | 전체 | | 상세 ② | `AUCTION_NOT_FOUND` |
| PATCH | `/api/auctions/{id}` | 판매자 | 등록과 같은 필드(`durationDays` 제외), 보낸 것만 수정 | 상세 ② | `AUCTION_NOT_OWNER`, `AUCTION_NOT_EDITABLE` + 등록 검증 |
| POST | `/api/auctions/{id}/cancel` | 판매자 | | 200 | `AUCTION_NOT_OWNER`, `AUCTION_NOT_EDITABLE` |
| GET | `/api/users/me/auctions` | USER | `status?`, `page`, `size` | Page 목록 항목 ① | |

- ① 목록 항목: `{ id, title, thumbnailUrl, categoryName, currentPrice, buyNowPrice, bidCount, status, endAt }`
- ② 상세: `{ id, title, description, category{id,name}, sellerNickname, imageUrls[], startPrice, currentPrice, buyNowPrice, minimumNextBid, bidCount, status, startAt, endAt, extendCount, mine }`
  - `minimumNextBid`는 `auction.minimumBid(bidUnitPolicy.unitFor(currentPrice))`. 입찰 담당 코드를 호출합니다.
  - 최고 입찰자 정보는 넣지 않습니다(R-18).
- `sort`: `endAt,asc`(마감 임박순, 기본) · `createdAt,desc`(최신순) · `currentPrice,asc|desc`

**할 일**
- 등록 시 연장 설정 복사(R-05), `currentPrice = startPrice`, `originalEndAt = endAt = now + durationDays`.
- 기간은 1~14일(R-02)이라 API로는 짧은 경매를 못 만듭니다. **시연용 마감 임박 경매는 테스트 데이터로 넣습니다**(C1).
- 판매자 취소 시 `AuctionCanceledEvent(byAdmin = false)` 발행(알림 없음).
- 목록 N+1 주의: fetch join 또는 DTO 조회.
- `Auction`의 `close`, `cancel`, `isEditable` 구현.

**함께 할 일**: **경매 종료 스케줄러**(1분). SOLD면 `tradeService.createTrade(..., BID_WIN, currentPrice)`, FAILED면 `AuctionFailedEvent`. 한 건씩 별도 트랜잭션. 6~7일차.

**먼저**: 등록 API **4일차까지**(입찰이 기다림).

### C8. `AdminAuctionController` — `auction/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/admin/auctions/{id}/cancel` | ADMIN | | 200 | `AUCTION_NOT_FOUND`, `AUCTION_NOT_EDITABLE`(진행 중이 아님) |

- 입찰이 있어도 취소할 수 있습니다. 최고 입찰자가 있으면 `pointService.unlockBid(...)`, `AuctionCanceledEvent(byAdmin = true)`.

---

## 5. 팀장 — 입찰

### C9. `BidController` — `bid/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/auctions/{id}/bids` | USER | `amount` | 입찰 결과 ③ | `SELF_BID_NOT_ALLOWED`, `ALREADY_HIGHEST_BIDDER`, `BID_TOO_LOW`, `AUCTION_CLOSED`, `ADDRESS_REQUIRED`, `BALANCE_INSUFFICIENT`, `BID_CONFLICT` |
| POST | `/api/auctions/{id}/buy-now` | USER | | 입찰 결과 ③ | 위 + `BUY_NOW_NOT_AVAILABLE` |
| GET | `/api/auctions/{id}/bids` | 전체 | `page`, `size` | Page `{ amount, bidAt, mine }` | `AUCTION_NOT_FOUND` |
| GET | `/api/auctions/{id}/stream` | 전체 | | SSE `price` 이벤트 `{ auctionId, currentPrice, minimumNextBid, bidCount, endAt, extended, status }` (심화) | `AUCTION_NOT_FOUND` |
| GET | `/api/users/me/bids` | USER | `page`, `size` | Page `{ bidId, auctionId, auctionTitle, amount, bidAt, auctionStatus, currentPrice, highest }` | |

- ③ 입찰 결과: `{ bidId, amount, currentPrice, endAt, extended, boughtNow, tradeId? }`
- 입찰 내역은 금액과 시각만(R-18). 비로그인이면 `mine`은 모두 false.

**함께 할 일 (컨트롤러보다 먼저)**
- `BidUnitPolicyImpl`, `Auction`의 `isOpenAt`, `minimumBid`, `canBuyNow`, `applyBid`, `markBoughtNow`. **3일차까지**(경매 상세가 `minimumBid`를 씀).
- 입찰 트랜잭션 8단계(기술서 9.1), `OutbidEvent` 발행, 충돌 시 409.
- 동시 입찰 100건 테스트(8~9일차).

### C10. `AdminBidController` — `bid/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/admin/auctions/{id}/bids` | ADMIN | `page`, `size` | Page `{ bidId, amount, bidAt, bidder{ id, nickname, email } }` | `AUCTION_NOT_FOUND` |

---

## 6. 거래

### C11. `TradeController` — `trade/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/trades` | USER | `role`(buyer\|seller), `status?`, `page`, `size` | Page `{ tradeId, auctionId, auctionTitle, type, price, status, tradedAt, shipDeadlineAt, autoConfirmAt }` | |
| GET | `/api/trades/{id}` | 당사자 | | 상세 ④ | `TRADE_NOT_FOUND`, `TRADE_NOT_PARTICIPANT` |
| PATCH | `/api/trades/{id}/tracking` | 판매자 | `trackingNumber` | 상세 ④ | `TRADE_NOT_PARTICIPANT`, `TRADE_INVALID_STATUS`(PAID가 아님) |
| POST | `/api/trades/{id}/confirm` | 구매자 | | 상세 ④ | `TRADE_NOT_PARTICIPANT`, `TRADE_INVALID_STATUS`(SHIPPED가 아님) |

- ④ 상세: 목록 필드 + `{ counterpart{ nickname, phone }, shippingAddress, trackingNumber, feeAmount, settleAmount, shippedAt, confirmedAt, canceledAt, cancelReason }`
  - `counterpart`는 내가 구매자면 판매자, 판매자면 구매자(R-50).
  - `feeAmount`, `settleAmount`는 판매자에게만 보여줘도 됩니다.
- 송장 입력: `shippedAt = now`, `autoConfirmAt = now + 7일`, `TradeShippedEvent`.
- 구매확정: `pointService.confirmPurchase(...)`.

**함께 할 일**
- `TradeServiceImpl.createTrade` **6일차까지**(입찰의 즉시구매, 경매 종료 스케줄러가 기다림).
- 스케줄러 3개(송장 기한 초과, 자동 구매확정, 확정 임박 알림). 7~8일차. 조건은 기술서 9.3.

### C12. `AdminTradeController` — `trade/controller`

| Method | URI | 권한 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/admin/trades/{id}/cancel` | ADMIN | | 200 | `TRADE_NOT_FOUND`, `TRADE_INVALID_STATUS`(CONFIRMED·CANCELED) |

- PAID, SHIPPED만 취소 가능(R-55). `pointService.refund(...)`, `TradeCanceledEvent(reason = ADMIN)`.

---

## 7. 브랜치·PR 규칙

GitHub 이슈는 쓰지 않습니다. 작업 관리는 노션 WBS에서 하고, 카드 하나를 API 단위의 PR 여러 개로 나눠 올립니다.

### 브랜치

- `dev`에서 `feature/{도메인}-{작업}`으로 만듭니다. 예: `feature/bid-controller`, `feature/auth-login`, `feature/point-service`
- 도메인: `user`, `auth`, `point`, `category`, `auction`, `bid`, `trade`, `notification`, `common`
- 한글과 공백은 쓰지 않습니다.

### PR 규칙

| 항목 | 규칙 |
| --- | --- |
| 대상 브랜치 | 항상 `dev`. `main`은 팀장만 병합할 수 있고, 중간 점검·발표 전에 `dev` → `main` PR로 합칩니다. |
| 단위 | API 1개(또는 함께 묶이는 1~2개)나 서비스 하나. 변경은 150줄 이내 권장, 300줄을 넘으면 나눕니다(테스트 코드 제외). |
| 제목 | `[도메인] 작업 내용`. 예: `[bid] 입찰 API 구현`, `[point] lockForBid 구현` |
| 본문 | PR 템플릿(`.github/pull_request_template.md`)을 채웁니다. 관련 API는 꼭 적고, 규칙 번호(R-xx)는 알면 적습니다(선택). |
| 머지 조건 | CI 통과 + 리뷰어 1명 이상 승인(가능하면 팀장 포함). GitHub 브랜치 보호로 강제되어 있어서 조건이 안 맞으면 머지 버튼이 막힙니다. |
| 머지 방법 | 승인 후 작성자가 **Squash and merge**로 합칩니다. 머지한 브랜치는 지우지 않습니다. |
| 리뷰 기한 | 리뷰 요청을 받으면 다음 날 09시 회의 전까지 리뷰합니다. 급하면 회의에서 말합니다. |
| 리뷰 코멘트 | 앞에 `[필수]`(고쳐야 머지), `[제안]`(선택), `[질문]`을 붙입니다. |
| 충돌 | 작성자가 최신 `dev`를 받아(`git pull origin dev`) 해결합니다. |
| 계약 파일 | `PointService`, `TradeService`, `FeeCalculator`, `BidUnitPolicy`, 이벤트 record의 시그니처를 바꾸면 영향받는 담당자를 리뷰어로 지정합니다. |
