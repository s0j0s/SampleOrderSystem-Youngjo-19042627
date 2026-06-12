# PLAN.md — 구현 계획

**프로젝트:** 반도체 시료 생산주문관리 시스템  
**기준일:** 2026-06-12  
**진행 방식:** 각 Phase → Explore → Plan → Action → Commit → 검토 후 다음 Phase

---

## 현재 상태 (베이스라인)

```
✅ 완료
- Gradle 8.7 + Java 17 + H2 + JUnit5 + Mockito 설정
- 기본 패키지 구조
- Sample / Order / OrderStatus 모델 (부분)
- SampleRepository / OrderRepository (부분)
- SampleController / OrderController (부분)
- ConsoleView (골격)
- Main.java (골격)
- 테스트 21개 통과

❌ 미구현
- OrderStatus: PENDING/RELEASED → RESERVED/RELEASE 미수정
- Sample: yield, productionTime 필드 없음
- Order: createdAt 필드 없음
- Production 모델/레포/컨트롤러 없음
- DB 스키마: 신규 컬럼/테이블 미반영
- 전체 메뉴 미구현
```

---

## Phase 1 — 도메인 모델 완성

> **목표:** 코드의 도메인 객체가 PRD의 엔티티 정의와 완전히 일치

### Explore
- `model/OrderStatus.java` 현재 상태 확인
- `model/Sample.java` 필드 목록 확인
- `model/Order.java` 필드 목록 확인
- PRD.md 엔티티 정의와 비교

### Plan
- 변경할 필드 목록 정리
- 생성자 오버로딩 vs 파라미터 추가 방향 결정
- Production 클래스 필드 설계

### Action

#### 1-1. `OrderStatus.java`
```
PENDING  → RESERVED
RELEASED → RELEASE
유지: REJECTED, PRODUCING, CONFIRMED
```

#### 1-2. `Sample.java`
추가 필드:
- `double yield` (수율, 0.0~1.0)
- `int productionTime` (단위당 생산 시간, 시간)

생성자 전체 파라미터 추가. `toString()` 업데이트.

#### 1-3. `Order.java`
추가 필드:
- `long createdAt` (epoch millis, FIFO 정렬용)

생성자 전체 파라미터 추가.

#### 1-4. `Production.java` 신규
```
productionId  String   PRD-NNNN 형식
orderId       String   FK → Order
sampleId      String   FK → Sample
productionQty int      ceil(orderQty / (yield * 0.9))
estimatedHours long    productionTime * productionQty
completed     boolean  기본값 false
```

### Commit
```
feat(model): 도메인 모델 완성 - Production 추가 및 필드 보완
```

---

## Phase 2 — DB 스키마 완성

> **목표:** `DatabaseManager`의 DDL이 PRD 스키마와 100% 일치

### Explore
- `db/DatabaseManager.java` 현재 DDL 확인
- PRD.md DB 스키마 섹션과 비교
- H2 `CREATE TABLE IF NOT EXISTS` 패턴 확인

### Plan
- SAMPLE 추가 컬럼 목록 정리
- ORDERS 추가 컬럼 목록 정리
- PRODUCTION 테이블 DDL 설계
- 외래키 제약 순서 결정 (SAMPLE → ORDERS → PRODUCTION)

### Action

#### 2-1. SAMPLE 테이블
```sql
YIELD           DOUBLE NOT NULL DEFAULT 1.0
PRODUCTION_TIME INT    NOT NULL DEFAULT 1
```

#### 2-2. ORDERS 테이블
```sql
CREATED_AT BIGINT NOT NULL
```

#### 2-3. PRODUCTION 테이블 신규
```sql
CREATE TABLE IF NOT EXISTS PRODUCTION (
    PRODUCTION_ID   VARCHAR(15) PRIMARY KEY,
    ORDER_ID        VARCHAR(15) NOT NULL UNIQUE,
    SAMPLE_ID       VARCHAR(15) NOT NULL,
    PRODUCTION_QTY  INT         NOT NULL,
    ESTIMATED_HOURS BIGINT      NOT NULL,
    COMPLETED       BOOLEAN     NOT NULL DEFAULT FALSE,
    FOREIGN KEY (ORDER_ID)  REFERENCES ORDERS(ORDER_ID),
    FOREIGN KEY (SAMPLE_ID) REFERENCES SAMPLE(SAMPLE_ID)
);
```

### Commit
```
feat(db): DB 스키마 완성 - yield/productionTime/createdAt 컬럼 및 PRODUCTION 테이블 추가
```

---

## Phase 3 — Repository 완성

> **목표:** 모든 DB 접근 로직 구현 + 테스트 통과

### Explore
- `repository/SampleRepository.java` 현재 메서드 목록
- `repository/OrderRepository.java` 현재 메서드 목록
- Phase 1~2 변경 사항이 SQL에 미치는 영향 파악
- `SampleRepositoryTest`, `OrderRepositoryTest` 현재 픽스처 확인

### Plan
- SampleRepository 수정 메서드 목록
- OrderRepository 수정 메서드 목록
- ProductionRepository 메서드 설계
- 테스트 픽스처 업데이트 범위 결정

### Action

#### 3-1. `SampleRepository.java` 수정
- `save()`: INSERT에 yield, productionTime 추가
- `findAll()` / `findById()` / `update()`: mapRow() yield, productionTime 반영
- `updateStock()`: 유지
- `searchByName(String keyword)` 신규:
  ```sql
  SELECT * FROM SAMPLE WHERE NAME LIKE ? ORDER BY SAMPLE_ID
  -- ? = '%' + keyword + '%'
  ```

#### 3-2. `OrderRepository.java` 수정
- `save()`: INSERT에 createdAt 추가
- `findAll()` / `findById()`: mapRow() createdAt 반영
- `findByStatusOrderByCreatedAt(OrderStatus)` 신규:
  ```sql
  SELECT * FROM ORDERS WHERE STATUS = ? ORDER BY CREATED_AT ASC
  ```
- `findByStatus()` / `updateStatus()`: 유지

#### 3-3. `ProductionRepository.java` 신규
```java
void save(Production production)
Optional<Production> findById(String productionId)
Optional<Production> findByOrderId(String orderId)
List<Production> findPendingByFifo()
  // SELECT p.*, o.CREATED_AT FROM PRODUCTION p
  // JOIN ORDERS o ON p.ORDER_ID = o.ORDER_ID
  // WHERE p.COMPLETED = FALSE
  // ORDER BY o.CREATED_AT ASC
void complete(String productionId)
int nextSequence()
```

#### 3-4. 테스트 업데이트
- `SampleRepositoryTest`: 생성자에 yield, productionTime 추가
- `OrderRepositoryTest`: 생성자에 createdAt 추가
- `ProductionRepositoryTest` 신규:
  - save/findById/findByOrderId/findPendingByFifo(FIFO 순서)/complete

### Commit
```
feat(repository): Repository 완성 - 신규 필드 반영 및 ProductionRepository 추가
test(repository): Repository 테스트 완성
```

---

## Phase 4 — Controller 완성

> **목표:** 비즈니스 로직 전체 구현 + Mockito 테스트 통과

### Explore
- `controller/OrderController.java` approveOrder 로직 확인
- `controller/SampleController.java` 파라미터 목록 확인
- PRD 승인 분기 로직 및 생산량 공식 확인

### Plan
- OrderController 의존성 변경 확인 (ProductionRepository 주입 필요)
- 생산량 계산 공식 코드 변환: `(int) Math.ceil(qty / (yield * 0.9))`
- ProductionController 의존성 목록 (ProductionRepository + OrderRepository + SampleRepository)

### Action

#### 4-1. `SampleController.java` 수정
- `registerSample()`: yield, productionTime 파라미터 추가
- `searchByName(String keyword)` 신규

#### 4-2. `OrderController.java` 수정
- 상태 상수: `PENDING → RESERVED`, `RELEASED → RELEASE`
- 생성자: `ProductionRepository` 주입 추가
- `approveOrder()` 수정:
  ```
  재고 >= qty → CONFIRMED + updateStock(stock - qty)
  재고 < qty  → PRODUCING + Production 생성
    productionQty   = ceil(qty / (yield * 0.9))
    estimatedHours  = sample.productionTime * productionQty
    productionRepository.save(new Production(...))
  ```
- `rejectOrder()`: `PENDING → RESERVED` 조건 수정

#### 4-3. `ProductionController.java` 신규
```java
// 의존성: ProductionRepository, OrderRepository, SampleRepository

List<Production> getPendingQueue()
  → productionRepository.findPendingByFifo()

Production completeProduction(String productionId)
  → production.completed = true
  → sample.stock += (production.productionQty - order.quantity)  // 잉여분 입고
  → order.status → CONFIRMED
```

#### 4-4. 테스트 업데이트
- `OrderControllerTest`: RESERVED/RELEASE, 승인 시 Production save() 검증
- `SampleControllerTest`: registerSample 파라미터 추가, searchByName 테스트
- `ProductionControllerTest` 신규:
  - completeProduction 정상 처리
  - 생산량 공식 검증 (yield=0.9, qty=10 → prodQty=13)
  - 이미 완료된 생산 → `IllegalStateException`
  - FIFO 순서 검증

### Commit
```
feat(controller): Controller 완성 - 생산 로직 및 ProductionController 추가
test(controller): Controller 테스트 완성
```

---

## Phase 5 — View + Main 완성

> **목표:** 전체 메뉴 동작, `./gradlew run`으로 수동 검증 가능

### Explore
- `view/ConsoleView.java` 현재 메서드 목록
- `Main.java` 현재 메뉴 switch 구조
- POC3(DataMonitor) ANSI 컬러 패턴 참조

### Plan
- 메뉴 번호 → 기능 매핑 최종 확정
- 서브메뉴 구조 결정 (단일 레벨 vs 2단계)
- ANSI 컬러 기준: 재고 > 15 녹색 / 1~15 노란색 / 0 빨간색

### Action

#### 5-1. `ConsoleView.java` 수정
추가 메서드:
```
showMainMenu()
showSampleSubMenu()
showOrderSubMenu()
showProductionQueue(List<Production>, Map<String, Sample>)
showMonitor(Map<OrderStatus, Long> orderCounts, List<Sample>)
```
재고 컬러 기준 적용.

#### 5-2. `Main.java` 수정
전체 메뉴 케이스 구현:
```
1. 시료 관리
   1. 시료 등록   ← 이름/사양/재고/수율(0.0~1.0)/단위생산시간(h) 입력
   2. 시료 조회
   3. 시료 검색   ← 키워드 입력
2. 주문 접수      ← 시료ID/고객ID/수량 입력
3. 주문 승인/거부
   1. RESERVED 주문 목록
   2. 주문 승인   ← 주문ID 입력
   3. 주문 거부   ← 주문ID 입력
4. 생산 관리
   1. 생산 큐 조회 (FIFO)
   2. 생산 완료   ← 생산ID 입력
5. 출고 처리      ← CONFIRMED 목록 → 주문ID 입력
6. 모니터링
0. 종료
```

### Commit
```
feat(view): 전체 메뉴 UI 구현
feat(main): Main 메뉴 루프 완성
```

---

## Phase 6 — 테스트 완성 및 최종 검증

> **목표:** 전체 테스트 30개 이상 통과, 시나리오 기반 수동 검증

### Explore
- 현재 테스트 커버리지 현황
- `docs/test-scenarios.md` 시나리오 목록 대비 누락 테스트 파악

### Plan
- 추가할 테스트 케이스 목록 정리
- 경계값 시나리오 우선순위 결정

### Action

#### 6-1. 경계값 테스트 추가
- 재고 = 주문수량 정확히 일치 → CONFIRMED
- 수율 1.0 / 0.5 생산량 계산 검증
- REJECTED 주문 출고 시도 → `IllegalStateException`

#### 6-2. 수동 검증 시나리오
`docs/test-scenarios.md` 4절 통합 시나리오 순서대로 실행:
1. 정상 흐름 (재고 충분)
2. 생산 흐름 (재고 부족)
3. 주문 거부 흐름
4. FIFO 생산 순서

### Commit
```
test: 경계값 및 예외 시나리오 테스트 추가
docs: PLAN.md 완료 체크리스트 업데이트
```

---

## 완료 체크리스트

- [x] Phase 1: 모델 컴파일 통과, Production 클래스 생성
- [x] Phase 2: `./gradlew run` DB 초기화 정상
- [x] Phase 3: `./gradlew test` Repository 테스트 전체 통과
- [x] Phase 4: `./gradlew test` Controller 테스트 전체 통과
- [x] Phase 5: `./gradlew run` 전체 메뉴 수동 동작 확인
- [x] Phase 6: `./gradlew test` 52개 통과 (커버리지 100%), 수동 시나리오 A~D 완료

---

## 의존 관계

```
Phase 1 (모델)
    ↓
Phase 2 (DB 스키마)
    ↓
Phase 3 (Repository)  ─── Phase 3 테스트
    ↓
Phase 4 (Controller)  ─── Phase 4 테스트
    ↓
Phase 5 (View + Main)
    ↓
Phase 6 (최종 테스트 + 수동 검증)
```
