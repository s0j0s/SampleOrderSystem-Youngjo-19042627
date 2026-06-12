# Phase 2 설계 — DB 스키마 완성

**목표:** `DatabaseManager` DDL이 PRD 스키마와 100% 일치  
**상태:** Phase 1 커밋에서 동시 구현 완료 → 설계 문서 소급 작성

---

## Explore 결과

### Phase 1 이전 스키마 vs PRD 요구

| 컬럼/테이블 | Phase 1 이전 | PRD 요구 | 결과 |
|------------|------------|---------|------|
| SAMPLE.YIELD | 없음 | DOUBLE NOT NULL | ❌ 누락 |
| SAMPLE.PRODUCTION_TIME | 없음 | INT NOT NULL | ❌ 누락 |
| ORDERS.CREATED_AT | 없음 | BIGINT NOT NULL | ❌ 누락 |
| ORDERS.STATUS 기본값 | `'PENDING'` | `'RESERVED'` | ❌ 불일치 |
| PRODUCTION 테이블 | 없음 | 신규 테이블 | ❌ 누락 |
| ID 컬럼 VARCHAR 길이 | VARCHAR(10) | VARCHAR(15) | ❌ 불일치 |

### Phase 1 커밋 이후 현재 스키마

```sql
-- SAMPLE
SAMPLE_ID       VARCHAR(15)  PRIMARY KEY
NAME            VARCHAR(100) NOT NULL
SPEC            VARCHAR(200)
STOCK           INT          NOT NULL DEFAULT 0
YIELD           DOUBLE       NOT NULL DEFAULT 1.0
PRODUCTION_TIME INT          NOT NULL DEFAULT 1

-- ORDERS
ORDER_ID    VARCHAR(15)  PRIMARY KEY
SAMPLE_ID   VARCHAR(15)  NOT NULL
CUSTOMER_ID VARCHAR(50)  NOT NULL
QUANTITY    INT          NOT NULL
STATUS      VARCHAR(20)  NOT NULL DEFAULT 'RESERVED'
CREATED_AT  BIGINT       NOT NULL DEFAULT 0
FOREIGN KEY (SAMPLE_ID) REFERENCES SAMPLE(SAMPLE_ID)

-- PRODUCTION (신규)
PRODUCTION_ID   VARCHAR(15)  PRIMARY KEY
ORDER_ID        VARCHAR(15)  NOT NULL
SAMPLE_ID       VARCHAR(15)  NOT NULL
PRODUCTION_QTY  INT          NOT NULL
ESTIMATED_HOURS BIGINT       NOT NULL
COMPLETED       BOOLEAN      NOT NULL DEFAULT FALSE
FOREIGN KEY (ORDER_ID)  REFERENCES ORDERS(ORDER_ID)
FOREIGN KEY (SAMPLE_ID) REFERENCES SAMPLE(SAMPLE_ID)
```

---

## 설계 결정

### D-1. VARCHAR(15) 통일
**결정:** 모든 ID 컬럼 VARCHAR(15)  
**이유:** `PRD-NNNN` 포맷 최대 8자, 여유분 포함. 기존 VARCHAR(10)은 부족.

### D-2. PRODUCTION.ORDER_ID — UNIQUE 제약 제거
**결정:** UNIQUE 제약 없음  
**이유:** 주문 재생산 시나리오(예: 불량 재처리)에서 동일 orderId로 복수 생산 레코드가 생길 수 있음. Phase 4 Controller에서 비즈니스 레벨로 제어.

### D-3. CREATE TABLE IF NOT EXISTS 패턴 유지
**결정:** 애플리케이션 기동 시 자동 스키마 초기화  
**이유:** 별도 마이그레이션 도구 없는 콘솔 앱 특성. 파일 기반 H2 재기동 시에도 안전.

### D-4. FK 생성 순서
**결정:** SAMPLE → ORDERS → PRODUCTION  
**이유:** FK 참조 대상 테이블이 먼저 존재해야 함. tearDown에서는 역순(PRODUCTION → ORDERS → SAMPLE)으로 DROP.

### D-5. YIELD DEFAULT 1.0, PRODUCTION_TIME DEFAULT 1
**결정:** 스키마 레벨 기본값 설정  
**이유:** NOT NULL 컬럼이나, INSERT 시 누락 방지용 안전망. 실제 등록은 Controller에서 필수 파라미터로 강제.

---

## 테스트 검증

Phase 1 커밋 시 Repository 테스트에서 간접 검증 완료.

| 검증 항목 | 방법 | 결과 |
|----------|------|------|
| SAMPLE yield/productionTime 저장·조회 | SampleRepositoryTest | ✅ PASSED |
| ORDERS created_at 저장·조회 | OrderRepositoryTest | ✅ PASSED |
| PRODUCTION 테이블 FK tearDown 순서 | OrderRepositoryTest.tearDown | ✅ 수정 완료 |
| 전체 빌드 | `./gradlew test` 20개 통과 | ✅ BUILD SUCCESSFUL |

---

## 검토 체크리스트

> Action 진행 전 아래 항목을 확인하고 승인해주세요.

### 스키마 정합성
- [x] 모든 ID 컬럼 VARCHAR(15) — `S-NNN`, `ORD-NNNN`, `PRD-NNNN` 포맷에 충분한가?
- [x] `ORDERS.STATUS` 기본값 `'RESERVED'` — PRD 초기 상태와 일치하는가?
- [x] `ORDERS.CREATED_AT BIGINT` (epoch millis) — FIFO 정렬 목적으로 충분한가?

### PRODUCTION 테이블 설계
- [x] `ORDER_ID`에 UNIQUE 제약 없음 — 재생산 시나리오 허용, 비즈니스 레벨로 제어하는 방식에 동의하는가?
- [x] `PRODUCTION_ID` PRIMARY KEY만 있으면 충분한가, 아니면 추가 인덱스가 필요한가?
- [x] FK 순서 SAMPLE → ORDERS → PRODUCTION (tearDown 역순) — 이해하고 동의하는가?

### 스키마 초기화 방식
- [x] `CREATE TABLE IF NOT EXISTS` 패턴 — 별도 마이그레이션 없이 앱 기동 시 자동 초기화하는 방식이 맞는가?
- [x] 파일 기반 H2(`./data/ssemi`)와 인메모리 H2(`mem:testdb`) 두 환경 모두 같은 DDL 사용 — 문제 없는가?

---

## Phase 3 연결 포인트

Phase 2 스키마 변경으로 인해 Phase 3에서 추가 구현이 필요한 항목:

| 항목 | 위치 | 내용 |
|------|------|------|
| `searchByName` | SampleRepository | `NAME LIKE ?` 검색 |
| FIFO 조회 | OrderRepository | `findByStatus` → `ORDER BY CREATED_AT ASC` (이미 적용됨) |
| ProductionRepository | 신규 | save / findByOrderId / findPendingByFifo / complete |
| ProductionRepositoryTest | 신규 | 위 메서드 전체 테스트 |
