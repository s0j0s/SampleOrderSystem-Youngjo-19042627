# Test Scenarios — 반도체 시료 생산주문관리 시스템

## 테스트 전략 요약

| 레이어 | 도구 | DB |
|--------|------|----|
| Repository | JUnit 5 | H2 in-memory |
| Controller | JUnit 5 + Mockito | Mock Repository |

---

## 1. 시료 관리 (Sample)

### 1-1. SampleControllerTest

| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 시료 등록 성공 | name, spec, stock, yield, productionTime | sampleId=`S-001`, save() 호출 확인 |
| ID 포맷 검증 | sequence=5 | `S-005` 반환 |
| 전체 목록 반환 | - | findAll() 위임, 리스트 반환 |
| ID로 조회 | `S-001` | Optional.of(sample) 반환 |
| 키워드 검색 | keyword | searchByName() 위임, 결과 반환 |

### 1-2. SampleRepositoryTest

| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 저장 후 조회 | Sample(`S-001`, GaN, 50, yield=0.9, productionTime=2) | findById → 동일 객체 |
| 전체 목록 조회 | 2개 저장 | findAll → size=2 |
| 재고 수정 | updateStock(`S-001`, 30) | findById → stock=30 |
| 전체 필드 수정 | update(S-001, 변경된 모든 필드) | findById → 수정된 값 반환 |
| 존재하지 않는 ID | `S-999` | Optional.empty() |
| 시퀀스 번호 | 저장 0개 → 1개 | nextSequence: 1 → 2 |
| 이름 키워드 검색 | keyword="GaN", 3개 중 2개 매칭 | size=2, 모두 "GaN" 포함 |
| 이름 검색 결과없음 | keyword="InP", 매칭 없음 | 빈 리스트 |

---

## 2. 주문 관리 (Order)

### 2-1. OrderControllerTest

| 시나리오 | 조건 | 기대 결과 |
|----------|------|-----------|
| 주문 생성 성공 | 시료 존재 | orderId=`ORD-0001`, RESERVED 상태, save() 호출 |
| 존재하지 않는 시료 주문 | findById=empty | `IllegalArgumentException` |
| 전체 목록 반환 | - | findAll() 위임, 리스트 반환 |
| ID로 조회 | `ORD-0001` | Optional.of(order) 반환 |
| 상태별 조회 | status=RESERVED | findByStatus() 위임, 결과 반환 |
| 승인 — 재고 충분 | stock=50, qty=10 | CONFIRMED, updateStock(S-001, 40) |
| 승인 — 재고 = 수량 | stock=10, qty=10 | CONFIRMED, updateStock(S-001, 0) |
| 승인 — 재고 부족 | stock=5, qty=10 | PRODUCING, updateStock 미호출, Production 생성 |
| 승인 — yield=1.0 | stock=0, qty=100 | productionQty=112 (ceil(100/0.9)) |
| 승인 — yield=0.5 | stock=0, qty=10 | productionQty=23 (ceil(10/0.45)) |
| RESERVED 아닌 주문 승인 | status=CONFIRMED | `IllegalStateException` |
| 존재하지 않는 주문 승인 | findById=empty | `IllegalArgumentException` |
| 주문 거부 성공 | RESERVED 주문 | REJECTED, updateStatus 호출 |
| RESERVED 아닌 주문 거부 | status=CONFIRMED | `IllegalStateException` |
| CONFIRMED 주문 출고 | status=CONFIRMED | RELEASE |
| PRODUCING 주문 출고 시도 | status=PRODUCING | `IllegalStateException` |
| REJECTED 주문 출고 시도 | status=REJECTED | `IllegalStateException` |

### 2-2. OrderRepositoryTest

| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 저장 후 조회 | Order(`ORD-0001`, RESERVED) | findById → 동일 객체 |
| 전체 목록 조회 | 2개 저장 | findAll → size=2 |
| 상태별 조회 | RESERVED 1개, CONFIRMED 1개 | findByStatus(RESERVED) → size=1 |
| 상태별 조회 결과없음 | RESERVED만 존재 | findByStatus(CONFIRMED) → 빈 리스트 |
| FIFO 정렬 확인 | ORD-0001(t=2000), ORD-0002(t=1000) | findByStatus → ORD-0002 먼저 |
| 상태 수정 | RESERVED → CONFIRMED | findById → status=CONFIRMED |

---

## 3. 생산 관리 (Production)

### 3-1. ProductionControllerTest

| 시나리오 | 조건 | 기대 결과 |
|----------|------|-----------|
| 생산 완료 처리 | 미완료 생산 존재 | order→CONFIRMED, completed=true |
| 잉여 재고 입고 | prodQty=13, orderQty=10, stock=0 | updateStock("S-001", 3) |
| 잉여 없음 | prodQty=orderQty | updateStock 미호출 |
| 이미 완료된 생산 | completed=true | `IllegalStateException` |
| PRODUCING 아닌 주문의 생산 완료 | order.status=CONFIRMED | `IllegalStateException` |
| 존재하지 않는 생산ID | findById=empty | `IllegalArgumentException` |
| 미완료 큐 조회 | - | findPendingByFifo() 위임 |
| 만료 자동완료 | startedAt=1L, estimatedHours=1L | complete() 호출, order→CONFIRMED |
| 미만료 자동완료 안함 | startedAt=now, estimatedHours=100L | complete() 미호출 |
| 자동완료 예외 무시 | prod1 예외 발생, prod2 정상 | prod1 건너뛰고 prod2 완료 처리 |

### 3-2. ProductionRepositoryTest

| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 저장 후 조회 | Production(`PRD-0001`) | findById → 동일 객체 |
| 주문 ID로 조회 | orderId=`ORD-0001` | findByOrderId → production |
| 존재하지 않는 ID | `PRD-9999` | Optional.empty() |
| FIFO 큐 조회 | 미완료 2개 (createdAt 다름) | 오래된 순서 반환 |
| FIFO 큐 결과없음 | 저장 없음 | 빈 리스트 |
| 완료 처리 후 pending 제외 | complete("PRD-0001") | findPendingByFifo → PRD-0001 제외 |
| 시퀀스 번호 | 저장 0개 → 1개 | nextSequence: 1 → 2 |

---

## 4. 통합 시나리오

### 4-1. 정상 흐름 (재고 충분)

```
1. 시료 등록: S-001, GaN, stock=50
2. 주문 접수: ORD-0001, S-001, qty=10 → RESERVED
3. 주문 승인: stock=50 >= qty=10 → CONFIRMED, stock=40
4. 출고 처리: ORD-0001 → RELEASE
```

### 4-2. 생산 흐름 (재고 부족)

```
1. 시료 등록: S-001, GaN, stock=5, yield=0.9, productionTime=2h
2. 주문 접수: ORD-0001, S-001, qty=10 → RESERVED
3. 주문 승인: stock=5 < qty=10
   → PRODUCING
   → shortageQty=5, prodQty=ceil(5/0.81)=7, estimatedHours=14h
4. 생산 완료: surplus=7-10=-3 → 잉여 없음, updateStock 미호출
             ORD-0001 → CONFIRMED
5. 출고 처리: ORD-0001 → RELEASE
```

### 4-3. 주문 거부 흐름

```
1. 주문 접수: ORD-0001 → RESERVED
2. 주문 거부: ORD-0001 → REJECTED
3. 모니터링: REJECTED 주문 표시 안 됨 확인
```

### 4-4. FIFO 생산 순서

```
1. ORD-0001 접수 (t=1000ms) → PRODUCING
2. ORD-0002 접수 (t=2000ms) → PRODUCING
3. 생산 큐 조회: ORD-0001이 먼저 (FIFO)
4. ORD-0001 생산 완료 → CONFIRMED
5. 생산 큐 조회: ORD-0002만 남음
```

---

## 5. 경계값 및 예외 시나리오

| 시나리오 | 기대 동작 |
|----------|-----------|
| 재고 = 주문수량 정확히 일치 | CONFIRMED (재고 0으로 차감) |
| 주문수량 = 1 | 정상 처리 |
| 수율 = 1.0 | prodQty = ceil(shortageQty / 0.9) |
| 수율 = 0.5 | prodQty = ceil(shortageQty / 0.45), 더 많이 생산 |
| 존재하지 않는 주문 승인 | `IllegalArgumentException` |
| CONFIRMED 주문을 다시 승인 | `IllegalStateException` |
| REJECTED 주문 출고 시도 | `IllegalStateException` |
| PRODUCING 주문 출고 시도 | `IllegalStateException` |

---

## 6. 실행 방법

```bash
# 전체 테스트 (Windows)
test.bat

# 전체 테스트 (macOS / Linux)
./gradlew test

# 특정 클래스만
./gradlew test --tests "ssemi.controller.OrderControllerTest"

# 상세 출력
./gradlew test --info

# 결과 리포트
build/reports/tests/test/index.html
```
