# 크리에이터 정산 API

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot) ![Build](https://img.shields.io/badge/build-passing-brightgreen) ![Test](https://img.shields.io/badge/tests-passing-brightgreen)

> 크리에이터 강의 플랫폼의 판매/취소 내역을 기반으로 월별 정산을 계산하고,
> 정산 상태를 관리하는 RESTful API 서버입니다.

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [빠른 시작](#빠른-시작)
- [API 명세](#api-명세)
- [데이터 모델](#데이터-모델)
- [핵심 설계 결정](#핵심-설계-결정)
- [요구사항 해석 및 가정](#요구사항-해석-및-가정)
- [테스트 전략](#테스트-전략)
- [미구현 및 개선 아이디어](#미구현-및-개선-아이디어)
- [AI 활용 범위](#ai-활용-범위)

---

## 프로젝트 개요

### 구현 범위

| 구분 | 기능 | 구현 여부 |
|------|------|----------|
| 필수 | 판매 내역 등록 / 취소 등록 | ✅ |
| 필수 | 판매 내역 목록 조회 (크리에이터별, 기간 필터) | ✅ |
| 필수 | 크리에이터별 월별 정산 조회 | ✅ |
| 필수 | KST 기준 월 경계 처리 | ✅ |
| 필수 | 운영자용 기간별 정산 집계 | ✅ |
| 가산점 | 정산 상태 관리 (PENDING → CONFIRMED → PAID) | ✅ |
| 가산점 | 중복 정산 방지 (DB UNIQUE constraint) | ✅ |
| 가산점 | 정산 내역 CSV 다운로드 | ✅ |
| 가산점 | 수수료율 변경 이력 관리 | ✅ |

---

## 기술 스택

| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| Java | 21 | Record로 DTO 간결화, Virtual Threads로 블로킹 I/O 효율화 |
| Spring Boot | 3.3.x | 안정적인 프로덕션 기반, 풍부한 생태계 |
| Spring Data JPA | - | 타입 안전 쿼리, 레포지토리 추상화 |
| Flyway | - | 스키마 변경 이력 관리, 환경 간 마이그레이션 일관성 보장 |
| H2 | - | 별도 DB 설치 없이 즉시 실행, PostgreSQL 호환 모드 |
| JUnit 5 + AssertJ | - | 표현력 있는 테스트 assertion |
| Spring Actuator | - | 헬스체크, 운영 모니터링 엔드포인트 |

### 패키지 구조

계층형 아키텍처로 관심사 분리:

```
src/main/java/com/example/settlement/
├── presentation/       # Controller, GlobalExceptionHandler
│   └── common/         # ApiResponse 공통 응답 래퍼
├── application/        # UseCase 인터페이스 + 구현체 (트랜잭션 경계)
│   ├── sale/
│   ├── settlement/
│   └── feepolicy/
├── domain/             # Entity, Repository 인터페이스, 순수 비즈니스 로직
│   ├── creator/
│   ├── sale/
│   ├── settlement/
│   └── common/         # TimeRangeUtils, ErrorCode, BusinessException
└── infrastructure/     # JPA 설정, DataLoader, RequestLoggingFilter
```

---

## 빠른 시작

### 요구사항

- Java 21 이상

### 실행

```bash
# 저장소 클론
git clone https://github.com/chae909/creatorAPI.git
cd creatorAPI

# 실행 (Mac/Linux)
./gradlew bootRun

# 실행 (Windows)
.\gradlew.bat bootRun
```

### 접속 정보

| 항목 | URL |
|------|-----|
| API 서버 | http://localhost:8080 |
| H2 콘솔 | http://localhost:8080/h2-console |
| 헬스체크 | http://localhost:8080/actuator/health |

H2 접속 정보:
- JDBC URL: `jdbc:h2:mem:settlementdb`
- Username: `sa` / Password: (없음)

> 샘플 데이터는 앱 시작 시 Flyway V3 마이그레이션으로 자동 삽입됩니다.
> (creators 3명, courses 4개, sale_records 7건, cancel_records 3건,
>  fee_policy 1건 — 수수료율 20%, 2020-01-01부터 적용)

---

## API 명세

### 공통 응답 형식

```json
{
  "success": true,
  "data": { },
  "errorCode": null,
  "message": null
}
```

에러 응답:

```json
{
  "success": false,
  "data": null,
  "errorCode": "SA002",
  "message": "이미 취소된 판매 내역입니다."
}
```

### 에러 코드 정의

| 코드 | HTTP | 설명 |
|------|------|------|
| CR001 | 404 | 크리에이터를 찾을 수 없습니다 |
| CO001 | 404 | 강의를 찾을 수 없습니다 |
| SA001 | 404 | 판매 내역을 찾을 수 없습니다 |
| SA002 | 409 | 이미 취소된 판매 내역입니다 |
| SA003 | 400 | 환불 금액이 원결제 금액을 초과할 수 없습니다 |
| SA004 | 400 | 미래 시점의 결제 일시는 등록할 수 없습니다 |
| SA005 | 400 | 환불 금액은 0보다 커야 합니다 |
| ST001 | 400 | 올바르지 않은 연월 형식입니다 (예: 2025-03) |
| ST002 | 404 | 정산 내역을 찾을 수 없습니다 |
| ST003 | 400 | 시작일이 종료일보다 늦을 수 없습니다 |
| ST004 | 409 | 유효하지 않은 상태 전이입니다 |
| FP001 | 500 | 해당 기간의 수수료 정책을 찾을 수 없습니다 |
| FP002 | 409 | 해당 날짜의 수수료 정책이 이미 존재합니다 |

---

### 엔드포인트 목록

#### 1. 판매 등록

판매 내역을 등록합니다. `paidAt`이 미래 시점이면 거부합니다.

```bash
curl -X POST http://localhost:8080/api/sales \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "course-1",
    "studentId": "student-99",
    "amount": 100000,
    "paidAt": "2025-01-10T10:00:00+09:00"
  }'
```

```json
{
  "success": true,
  "data": {
    "saleId": "a1b2c3d4-...",
    "courseId": "course-1",
    "studentId": "student-99",
    "amount": 100000,
    "paidAt": "2025-01-10T01:00:00Z"
  }
}
```

---

#### 2. 판매 취소

판매 내역에 취소를 등록합니다. 동일 판매에 중복 취소는 409를 반환합니다.

```bash
curl -X POST http://localhost:8080/api/sales/sale-3/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "refundAmount": 80000,
    "cancelledAt": "2025-01-26T10:00:00+09:00"
  }'
```

```json
{
  "success": true,
  "data": {
    "cancelId": "c9d8e7f6-...",
    "saleId": "sale-3",
    "refundAmount": 80000,
    "cancelledAt": "2025-01-26T01:00:00Z"
  }
}
```

---

#### 3. 판매 내역 조회

크리에이터별, 기간별 판매 목록을 페이지네이션으로 조회합니다.

```bash
curl "http://localhost:8080/api/sales?creatorId=creator-1&from=2025-01-01T00:00:00%2B09:00&to=2025-01-31T23:59:59%2B09:00&page=0&size=20"
```

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "saleId": "sale-1",
        "courseId": "course-1",
        "amount": 100000,
        "paidAt": "2025-01-10T01:00:00Z",
        "cancelled": false
      }
    ],
    "totalElements": 3,
    "page": 0,
    "size": 20
  }
}
```

---

#### 4. 월별 정산 조회

크리에이터의 특정 월 정산을 조회합니다. PENDING 상태가 없으면 자동 생성합니다.
월 경계는 KST (Asia/Seoul) 기준으로 계산합니다.

```bash
# creator-1의 2025-01 정산 (bootRun 기본 샘플 데이터)
curl "http://localhost:8080/api/settlements/monthly?creatorId=creator-1&yearMonth=2025-01"
```

```json
{
  "success": true,
  "data": {
    "creatorId": "creator-1",
    "yearMonth": "2025-01",
    "status": "PENDING",
    "totalSales": 230000,
    "totalRefunds": 80000,
    "netSales": 150000,
    "feeRate": 0.2,
    "feeAmount": 30000,
    "payoutAmount": 120000,
    "saleCount": 3,
    "cancelCount": 1
  }
}
```

> **테스트 스위트 검증 수치** — `./gradlew test` 실행 시 테스트 프로파일(V4 마이그레이션)이
> 아래 과제 명세 기댓값과 정확히 일치함을 검증합니다:
>
> ```
> creator-1 / 2025-03: totalSales=260000, totalRefunds=110000, netSales=150000
>                      feeAmount=30000, payoutAmount=120000, saleCount=4, cancelCount=2
> ```

---

#### 5. 정산 확정 (PENDING → CONFIRMED)

```bash
curl -X POST "http://localhost:8080/api/settlements/confirm?creatorId=creator-1&yearMonth=2025-01"
```

```json
{
  "success": true,
  "data": {
    "creatorId": "creator-1",
    "yearMonth": "2025-01",
    "status": "CONFIRMED",
    "payoutAmount": 120000
  }
}
```

---

#### 6. 정산 지급 처리 (CONFIRMED → PAID)

```bash
curl -X POST "http://localhost:8080/api/settlements/pay?creatorId=creator-1&yearMonth=2025-01"
```

```json
{
  "success": true,
  "data": {
    "creatorId": "creator-1",
    "yearMonth": "2025-01",
    "status": "PAID",
    "payoutAmount": 120000
  }
}
```

---

#### 7. 운영자 정산 집계

기간별 전체 크리에이터 정산 목록과 합계를 반환합니다.

```bash
curl "http://localhost:8080/api/admin/settlements?from=2025-01-01&to=2025-03-31&page=0&size=50"
```

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "creatorId": "creator-1",
        "yearMonth": "2025-01",
        "status": "PENDING",
        "netSales": 150000,
        "payoutAmount": 120000
      }
    ],
    "grandTotal": 120000,
    "page": 0,
    "size": 50,
    "totalElements": 1
  }
}
```

---

#### 8. 정산 내역 CSV 다운로드

운영자용 기간별 정산 집계를 CSV로 다운로드합니다.

```bash
curl "http://localhost:8080/api/admin/settlements/export?from=2025-01-01&to=2025-03-31" \
  -o settlements.csv
```

응답 헤더: `Content-Type: text/csv; charset=UTF-8`
CSV 컬럼: 크리에이터ID, 연, 월, 상태, 총판매, 총환불, 순판매, 수수료율, 수수료, 정산예정

---

#### 9. 수수료 정책 조회

```bash
curl http://localhost:8080/api/admin/fee-policies
```

```json
{
  "success": true,
  "data": [
    { "id": 1, "feeRate": 0.2, "feeRatePercent": "20%", "effectiveFrom": "2020-01-01" }
  ]
}
```

---

#### 10. 수수료 정책 등록

`effectiveFrom` 기준 시점부터 적용되는 새 수수료율을 등록합니다.
동일 날짜 중복 시 409를 반환합니다.

```bash
curl -X POST http://localhost:8080/api/admin/fee-policies \
  -H "Content-Type: application/json" \
  -d '{"feeRate": 0.15, "effectiveFrom": "2024-01-01"}'
```

```json
{
  "success": true,
  "data": { "id": 2, "feeRate": 0.15, "feeRatePercent": "15%", "effectiveFrom": "2024-01-01" }
}
```

---

## 데이터 모델

### ERD

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│  creators   │1   N│   courses   │1   N│   sale_records   │
│─────────────│────▶│─────────────│────▶│──────────────────│
│ id (PK)     │     │ id (PK)     │     │ id (PK)          │
│ name        │     │ creator_id  │     │ course_id (FK)   │
│ created_at  │     │ title       │     │ student_id       │
└─────────────┘     │ created_at  │     │ amount (BIGINT)  │
                    └─────────────┘     │ paid_at (UTC)    │
                                        │ created_at       │
                                        └────────┬─────────┘
                                                 │ 1
                                                 │
                                                 ▼ 0..1
                                        ┌──────────────────┐
                                        │  cancel_records  │
                                        │──────────────────│
                                        │ id (PK)          │
                                        │ sale_record_id   │
                                        │  (FK, UNIQUE)    │
                                        │ refund_amount    │
                                        │ cancelled_at(UTC)│
                                        │ created_at       │
                                        └──────────────────┘

┌─────────────────────┐     ┌──────────────────────┐
│     settlements     │     │     fee_policies     │
│─────────────────────│     │──────────────────────│
│ id (PK, BIGSERIAL)  │     │ id (PK, BIGSERIAL)   │
│ creator_id (FK)     │     │ fee_rate DECIMAL(5,4)│
│ year                │     │ effective_from (DATE)│
│ month               │     │  UNIQUE              │
│ status              │     │ created_at           │
│ total_sales         │     └──────────────────────┘
│ total_refunds       │
│ net_sales           │
│ fee_rate (snapshot) │ ← 확정 시점 수수료율 보존
│ fee_amount          │
│ payout_amount       │
│ sale_count          │
│ cancel_count        │
│ confirmed_at        │
│ paid_at             │
│ created_at          │
│ updated_at          │
│ UNIQUE(creator_id,  │
│        year, month) │ ← 중복 정산 방지
└─────────────────────┘
```

**테이블 설계 의도:**
- `creators ← courses ← sale_records`: 판매는 강의에 귀속, 강의가 크리에이터에 귀속 — 크리에이터 직접 참조 없이 도메인 경계 유지
- `cancel_records.sale_record_id UNIQUE`: 1건 판매에 1건 취소만 허용, DB 레벨에서 강제
- `settlements.fee_rate (snapshot)`: 확정 시점 수수료율 보존 — 이후 정책 변경에도 확정 수치 불변
- `settlements UNIQUE(creator_id, year, month)`: 동일 크리에이터/월 중복 정산 원자적 방지

---

## 핵심 설계 결정

### 1. 타임존 전략 — UTC 저장, KST 계산

> **결정**: 모든 timestamp를 UTC(`Instant`)로 DB 저장, 월 경계 계산 시에만 KST(`Asia/Seoul`)로 변환
>
> **이유**: DB 레벨 타임존 처리는 서버/DB 환경 설정 차이로 예기치 못한 버그를 유발할 수 있음.
> 변환 로직을 `TimeRangeUtils` 한 곳에 집중시켜 변경 영향 최소화.
>
> **대안**: DB에 KST 직접 저장 → 글로벌 확장 시 타임존 혼재 문제 발생 위험

---

### 2. 월 경계 — 배타적 상한 (Exclusive Upper Bound)

> **결정**: 월 종료를 `23:59:59.999999999`가 아닌 다음 달 1일 `00:00:00`으로 설정, 쿼리는 `< end` 사용
>
> **이유**: 나노초 방식은 DB 타임스탬프 정밀도(MySQL DATETIME(6) 등)에 따라 반올림으로
> 다음 달 데이터가 포함될 수 있음. 배타적 상한은 정밀도에 무관하게 정확한 경계 보장.
>
> **대안**: `BETWEEN` + 나노초 → DB 종류/버전에 따라 동작이 달라질 위험

---

### 3. 동시성 제어 — DB UNIQUE + 예외 처리

> **결정**: 중복 취소/정산 방지를 애플리케이션 check-then-act가 아닌
> DB UNIQUE constraint + `DataIntegrityViolationException` 처리로 구현
>
> **이유**: `isPresent() → save()` 사이에 다른 스레드가 끼어들 수 있음 (race condition).
> DB constraint는 원자적 보장. 별도 분산락 없이 단순하게 처리 가능.
>
> **대안**: `SELECT FOR UPDATE` 비관적 락 → 단일 인스턴스에서 불필요한 락 오버헤드

---

### 4. RoundingMode.DOWN (절사)

> **결정**: 수수료 계산 시 `RoundingMode.DOWN` 사용
>
> **이유**: `FLOOR`는 음수에서 더 작은 값으로 내림 (`-6000.2 → FLOOR: -6001, DOWN: -6000`).
> 환불 초과로 `netSales`가 음수인 경우 `FLOOR`는 크리에이터에게 불리한 방향으로 계산됨.
> `DOWN`은 항상 0 방향으로 절사 — 금융 처리에 적합.
>
> **대안**: `FLOOR` → 음수 netSales 케이스에서 의도치 않은 결과

---

### 5. Clock 빈 주입

> **결정**: `Instant.now()` 직접 호출 대신 `Clock`을 `@Bean`으로 주입
>
> **이유**: `Instant.now()` 직접 사용 시 시간 의존 테스트에서 실행 시점에 따라 결과가 달라짐.
> `Clock.fixed()`로 시간을 고정하면 결정론적 테스트 가능.
>
> **대안**: `Instant.now()` 직접 사용 → 미래 날짜 `paidAt` 검증 테스트가 flaky해짐

---

### 6. 수수료율 이력 테이블

> **결정**: 수수료율을 `FeePolicy` 테이블로 분리, `effective_from` 기준 최신 정책 조회
>
> **이유**: 수수료율 변경 후 과거 정산 재조회 시 당시 요율 보장.
> `settlements.fee_rate` 스냅샷도 저장하여 확정 후 정책 변경에도 확정 수치 보존.
>
> **대안**: 상수 하드코딩 → 정책 변경마다 코드 수정 + 과거 정산 수치 불일치 위험

---

## 요구사항 해석 및 가정

| 항목 | 해석 | 근거 |
|------|------|------|
| 정산 기준 | 판매는 `paidAt`, 취소는 `cancelledAt` 기준으로 각각 월 귀속 | 명세 "결제 완료 일시 기준 / 취소는 취소 일시 기준" |
| 빈 월 조회 | 판매 없는 월도 0원으로 정상 응답 | 일관성 있는 API 동작 |
| 확정 후 재계산 | CONFIRMED/PAID 상태는 수치 변경 없음 | 정산 확정의 불변성 |
| 음수 순판매 | 환불 > 판매인 월은 음수 정산 허용 | 명세에 제한 없음, 운영상 발생 가능 |
| 정산 생성 시점 | `GET /settlements/monthly` 조회 시 PENDING으로 자동 생성 | UX: 조회와 생성을 분리하면 불편 |

### 추가로 설계한 테스트 케이스

명세에 없지만 중요하다고 판단하여 직접 추가:

| 케이스 | 이유 |
|--------|------|
| 음수 netSales (`-60000 × 0.2`) | FLOOR vs DOWN 차이가 실제로 발생하는 케이스 검증 |
| 동시 정산 생성 (5 스레드) | race condition 방지 로직 실제 동작 검증 |
| 미래 `paidAt` 등록 거부 | 잘못된 데이터 유입 방지 |
| `yearMonth` 형식 오류 4종 | `2025-3` / `202503` / `2025/03` / `abcd` |
| 수수료율 이력 적용 | 15% 정책 등록 후 해당 기간 정산 수치 확인 |
| FeePolicy 날짜 중복 | 409 Conflict 응답 확인 |
| KST 월 경계 엣지 케이스 | `2025-01-31T14:30Z` = Jan KST, `02-03T01Z` = Feb KST — 월 귀속 분리 검증 |

---

## 테스트 전략

### 테스트 구성

| 테스트 | 대상 | 방식 | 특징 |
|--------|------|------|------|
| `TimeRangeUtilsTest` | 타임존 경계 계산 | 순수 단위 테스트 | Spring 컨텍스트 없음 — 빠름 |
| `SettlementCalculatorTest` | 정산 금액 계산 | 순수 단위 테스트 | DOWN vs FLOOR 차이 검증 포함 |
| `SaleApiTest` | 판매 API | `@WebMvcTest` | `Clock.fixed()`로 시간 고정 |
| `SettlementIntegrationTest` | 전체 시나리오 | `@SpringBootTest` + H2 | 명세 검증 수치 정확성 확인, 5-스레드 동시성 테스트 |

### 테스트 실행

```bash
# Mac/Linux
./gradlew test

# Windows
.\gradlew.bat test

# 결과 리포트 (Mac)
open build/reports/tests/test/index.html
```

---

## 미구현 및 개선 아이디어

### 미구현

- **인증/인가**: `creatorId`를 쿼리 파라미터로 전달 (과제 허용 방식). 실서비스에서는 JWT 토큰에서 추출
- **실제 결제 연동**: API 직접 등록 방식으로 구현

### 개선한다면

| 항목 | 방향 |
|------|------|
| 인증 | Spring Security + JWT, `creatorId`를 토큰 클레임에서 추출 |
| 대용량 CSV | 스트리밍 방식으로 메모리 부담 없이 다운로드 |
| 정산 재계산 | CONFIRMED 이후 수동 재계산 요청 기능 |
| 운영 DB | H2 → PostgreSQL 전환 (`application.yml` datasource 변경만으로 가능) |
| 이벤트 기반 | 판매 등록 시 정산 집계 캐시 무효화 이벤트 발행 |

---

## AI 활용 범위

Claude(Anthropic)를 활용하여 코드 구조 설계 및 구현 초안을 생성했습니다.

**직접 검토 및 판단한 항목:**
- 타임존 경계값 처리 방식 (배타적 상한 채택 이유)
- `RoundingMode.DOWN` vs `FLOOR` 선택 근거
- 동시성 제어 전략 (DB constraint vs 애플리케이션 락)
- PR 코드 리뷰 피드백(Gemini Code Assist) 분석 및 반영 여부 판단
- 모든 테스트 케이스의 기댓값 수동 계산 검증
- 추가 테스트 케이스 설계 이유

AI가 생성한 코드를 그대로 사용하지 않고,
각 설계 결정의 "왜"를 이해하고 검토한 후 반영했습니다.
