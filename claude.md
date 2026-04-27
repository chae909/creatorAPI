# Creator Settlement API — Project Guide

## Project Overview
크리에이터 정산 API. 판매/취소 내역 기반 월별 정산 계산, 운영자 집계, 정산 상태 관리.

## Tech Stack
- Java 21 (Virtual Threads, Record, Sealed Class, Text Block)
- Spring Boot 3.3.x
- Spring Data JPA + QueryDSL 5.x (복잡한 집계 쿼리)
- Flyway (스키마 버전 관리)
- H2 (개발), PostgreSQL (운영) — 동일 SQL 호환
- JUnit 5 + AssertJ + MockMvc (테스트)
- Gradle (Kotlin DSL)

## Architecture: Layered + Domain-Centric
presentation/   → Controller, GlobalExceptionHandler, ApiResponse
application/    → UseCase interface + Impl (트랜잭션 경계)
domain/         → Entity, Repository interface, DomainService, 순수 비즈니스 로직
infrastructure/ → JPA 구현체, QueryDSL, DataLoader, Config

## Key Rules

### 절대 하지 말 것
- Controller에 비즈니스 로직 금지
- Entity를 API 응답에 직접 노출 금지
- 서비스 간 직접 의존 금지 (UseCase 인터페이스 경유)
- 테스트 없이 핵심 계산 로직 구현 금지

### 코드 스타일
- DTO는 Java Record 사용
- 예외는 반드시 ErrorCode enum 경유
- 모든 금액은 Long (원 단위, 소수점 없음)
- 시간은 엔티티에 Instant, 비즈니스 로직에서 KST ZonedDateTime 변환
- 수수료율은 BigDecimal

### 타임존 원칙
- DB 저장: UTC (Instant)
- 월 경계 계산: Asia/Seoul 기준 변환 후 처리
- API 입출력: ISO 8601 (offset 포함)

### 파일 생성 원칙
- 1 클래스 = 1 파일
- 기능 추가 시 기존 파일 수정 우선, 새 파일 생성은 최소화
- DTO record는 UseCase 파일 하단에 inner record로 선언 (파일 수 최소화)

### 정산 계산 공식
- 순판매 = 총판매 - 총환불
- 수수료 = 순판매 × fee_rate (fee_policies 테이블에서 해당 월 기준 조회)
- 정산예정 = 순판매 - 수수료
- 월 경계: 해당 월 1일 00:00:00 KST ~ 말일 23:59:59 KST

### DB 스키마 변경
- 반드시 Flyway 마이그레이션 파일로만 변경
- 파일명: V{n}__{description}.sql

### 테스트 전략
- 도메인 계산 로직: 단위 테스트 (순수 Java, Spring 컨텍스트 없이)
- API 엔드포인트: @WebMvcTest + MockBean
- 시나리오 검증: @SpringBootTest + H2 (샘플 데이터 기반)

## API Endpoints
POST   /api/sales                                → 판매 등록
POST   /api/sales/{saleId}/cancel               → 취소 등록
GET    /api/sales?creatorId=&from=&to=          → 판매 내역 조회
GET    /api/settlements/monthly                 → 월별 정산 조회 (creatorId, yearMonth)
POST   /api/settlements/confirm                 → 정산 확정 (PENDING→CONFIRMED)
POST   /api/settlements/pay                     → 정산 지급 (CONFIRMED→PAID)
GET    /api/admin/settlements?from=&to=         → 운영자 집계
GET    /api/admin/settlements/export            → CSV 다운로드

## Module Order (구현 순서 — 이 순서 엄수)
1. infra/config (Flyway SQL, JPA config, DataLoader)
2. domain/creator (Entity, Repository)
3. domain/sale (Entity, Repository, 도메인 검증)
4. domain/settlement (Entity, FeePolicy, Repository)
5. application/sale (UseCase — 등록/취소)
6. application/settlement (UseCase — 계산/집계)
7. presentation (Controller, Handler)
8. test (단위 → 통합 순서)