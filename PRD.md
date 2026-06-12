# PRD.md — 반도체 시료 생산주문관리 시스템

**버전:** 1.0.0  
**작성일:** 2026-06-12  
**출처:** [CRA_AI] Day3 개인과제 반도체시료관리 (p.5~23)

---

## 1. 시스템 개요

### 배경

반도체 회사 "S-Semi"는 다양한 종류의 반도체 시료(Sample)를 생산하여 설계사, 팹리스(Fabless) 업체, 연구 기관에 공급한다. 시료는 주문이 들어오면 기존 재고를 이용하거나 신규 생산하여 공급되며, 관리 부재로 주문 처리 현황 파악이 어렵다는 문제가 있었다. 이를 해결하기 위해 "반도체 시료 생산주문관리 시스템"을 구축한다.

### 목표

- 시료 재고의 정확한 등록 및 관리
- 주문 생명주기(접수 → 승인 → 생산/확정 → 출고)의 일관된 상태 관리
- 재고 기반 자동 상태 분기 및 생산 큐 관리

---

## 2. 도메인 모델

### 엔티티

#### Sample (시료)

| 필드 | 타입 | 설명 |
|------|------|------|
| sampleId | String | 식별자 (S-001 형식) |
| name | String | 시료명 |
| spec | String | 사양 |
| stock | int | 현재 재고 수량 |
| yield | double | 수율 (0.0~1.0), 생산량 대비 출하 가능 비율 |
| productionTime | int | 단위당 생산 시간 (시간) |

> 수율 예시: 100개 생산 후 90개 출하 가능 = 0.9

#### Order (주문)

| 필드 | 타입 | 설명 |
|------|------|------|
| orderId | String | 식별자 (ORD-0001 형식) |
| sampleId | String | 주문 시료 ID (FK) |
| customerId | String | 고객 ID |
| quantity | int | 주문 수량 |
| status | OrderStatus | 현재 상태 |
| createdAt | long | 주문 등록 시각 (epoch millis, FIFO 정렬용) |

#### Production (생산)

| 필드 | 타입 | 설명 |
|------|------|------|
| productionId | String | 식별자 (PRD-0001 형식) |
| orderId | String | 연관 주문 ID (FK) |
| sampleId | String | 생산 시료 ID |
| productionQty | int | 생산 수량 = ceil(주문수량 / (수율 × 0.9)) |
| estimatedHours | long | 예상 생산 시간 = productionTime × productionQty |
| completed | boolean | 생산 완료 여부 |

#### OrderStatus (상태 enum)

```
RESERVED → (승인) → CONFIRMED  ← 재고 충분
         →          PRODUCING  ← 재고 부족 → (생산완료) → CONFIRMED
         → (거부) → REJECTED
CONFIRMED → (출고) → RELEASE
```

| 상태 | 의미 |
|------|------|
| RESERVED | 주문 접수 |
| REJECTED | 주문 거부 |
| PRODUCING | 생산 중 (재고 부족 승인) |
| CONFIRMED | 주문 확정 (출고 대기) |
| RELEASE | 출고 완료 |

### DB 스키마

```sql
CREATE TABLE SAMPLE (
    SAMPLE_ID       VARCHAR(15)  PRIMARY KEY,
    NAME            VARCHAR(100) NOT NULL,
    SPEC            VARCHAR(200),
    STOCK           INT          NOT NULL DEFAULT 0,
    YIELD           DOUBLE       NOT NULL DEFAULT 1.0,
    PRODUCTION_TIME INT          NOT NULL DEFAULT 1
);

CREATE TABLE ORDERS (
    ORDER_ID    VARCHAR(15)  PRIMARY KEY,
    SAMPLE_ID   VARCHAR(15)  NOT NULL,
    CUSTOMER_ID VARCHAR(50)  NOT NULL,
    QUANTITY    INT          NOT NULL,
    STATUS      VARCHAR(20)  NOT NULL DEFAULT 'RESERVED',
    CREATED_AT  BIGINT       NOT NULL,
    FOREIGN KEY (SAMPLE_ID) REFERENCES SAMPLE(SAMPLE_ID)
);

CREATE TABLE PRODUCTION (
    PRODUCTION_ID   VARCHAR(15)  PRIMARY KEY,
    ORDER_ID        VARCHAR(15)  NOT NULL UNIQUE,
    SAMPLE_ID       VARCHAR(15)  NOT NULL,
    PRODUCTION_QTY  INT          NOT NULL,
    ESTIMATED_HOURS BIGINT       NOT NULL,
    COMPLETED       BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (ORDER_ID) REFERENCES ORDERS(ORDER_ID),
    FOREIGN KEY (SAMPLE_ID) REFERENCES SAMPLE(SAMPLE_ID)
);
```

---

## 3. 기능 목록

### 3.1 메인 메뉴

기능별 대기 화면 전시. 상단에 주요 기능 목록 표시.

```
===== 반도체 시료 생산주문관리 시스템 =====
1. 시료 관리
2. 주문 접수
3. 주문 승인/거부
4. 생산 관리
5. 출고 처리
6. 모니터링
0. 종료
```

### 3.2 시료 관리

| 기능 | 설명 | 입력 |
|------|------|------|
| 시료 등록 | 새 시료를 시스템에 추가 | 이름, 사양, 재고, 수율, 단위 생산시간 |
| 시료 조회 | 등록된 전체 시료 목록과 재고 확인 | - |
| 시료 검색 | 이름 또는 속성으로 특정 시료 검색 | 검색어 |

### 3.3 주문 접수

| 기능 | 설명 | 입력 |
|------|------|------|
| 주문 접수 | 고객이 원하는 시료를 수량 지정하여 주문 | 시료 ID, 고객 ID, 주문 수량 |

- 접수 즉시 상태: **RESERVED**

### 3.4 주문 승인/거부

| 기능 | 설명 |
|------|------|
| RESERVED 주문 목록 | 대기 중인 주문 목록 표시 |
| 주문 승인 | 재고 확인 후 자동 상태 분기 처리 |
| 주문 거부 | 선택한 주문을 REJECTED 처리 |

**승인 분기 로직:**
- 재고 >= 주문수량 → **CONFIRMED** (재고 차감)
- 재고 < 주문수량 → **PRODUCING** (생산 레코드 자동 생성)

### 3.5 생산 관리

| 기능 | 설명 |
|------|------|
| 생산 현황 조회 | 시료별 진행 중인 생산 현황과 FIFO 큐 표시 |
| 생산 완료 처리 | 선택한 생산 완료 → 재고 업데이트 + 주문 CONFIRMED 처리 |

**생산 공식:**
- 생산량 = `ceil(주문수량 / (수율 × 0.9))`
- 예상 생산시간 = `단위생산시간 × 생산량` (시간)
- 큐 우선순위: FIFO (주문 접수 시각 기준)

**생산 완료 후:**
- 재고 += (생산량 - 주문수량) [잉여분 재고 편입]
- 해당 주문 상태 → **CONFIRMED**

### 3.6 출고 처리

| 기능 | 설명 |
|------|------|
| CONFIRMED 주문 목록 | 출고 가능한 주문 목록 표시 |
| 출고 처리 | 선택한 주문 → **RELEASE** 처리 |

### 3.7 모니터링

**주문 현황:**
- RESERVED / CONFIRMED / PRODUCING / RELEASE 상태별 주문 수 표시
- REJECTED는 표시하지 않음

**재고 현황:**
- 전체 시료 재고 표시
- 색상 구분: 녹색(재고 > 0), 노란색(재고 임박), 빨간색(재고 0)

---

## 4. 비기능 요구사항

| 항목 | 요구사항 |
|------|---------|
| 플랫폼 | Java 17, 콘솔 환경 |
| 영속성 | H2 파일 DB (`./data/ssemi`), 재시작 후 데이터 유지 |
| 보안 | SQL Injection 방지 (PreparedStatement 필수) |
| 테스트 | `./gradlew test` 전체 통과 |
| 코드 품질 | MVC + Repository 레이어 분리, 단일 책임 원칙 |
| UI | ANSI 컬러 콘솔 출력 |

---

## 5. 변경 이력

| 버전 | 날짜 | 내용 |
|------|------|------|
| 1.0.0-draft | 2026-06-12 | 초안 (골격) |
| 1.0.0 | 2026-06-12 | PDF 기능명세 반영 완성 |
