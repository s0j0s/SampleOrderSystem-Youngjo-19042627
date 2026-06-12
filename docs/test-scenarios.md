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
| 시료 등록 성공 | name, spec, stock | sampleId=`S-001`, save() 호출 확인 |
| ID 포맷 검증 | sequence=5 | `S-005` 반환 |
| 전체 목록 반환 | - | findAll() 위임, 리스트 반환 |
| ID로 조회 | `S-001` | Optional.of(sample) 반환 |

### 1-2. SampleRepositoryTest

| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 저장 후 조회 | Sample(`S-001`, GaN, 50) | findById → 동일 객체 |
| 전체 목록 조회 | 2개 저장 | findAll → size=2 |
| 재고 수정 | updateStock(`S-001`, 30) | findById → stock=30 |
| 존재하지 않는 ID | `S-999` | Optional.empty() |
| 시퀀스 번호 | 저장 0개 → 1개 | nextSequence: 1 → 2 |

---

## 2. 주문 관리 (Order)

### 2-1. OrderControllerTest

| 시나리오 | 조건 | 기대 결과 |
|----------|------|-----------|
| 주문 생성 성공 | 시료 존재 | orderId=`ORD-0001`, PENDING 상태, save() 호출 |
| 존재하지 않는 시료 주문 | findById=empty | `IllegalArgumentException` |
| 승인 — 재고 충분 | stock=50, qty=10 | CONFIRMED, updateStock(S-001, 40) |
| 승인 — 재고 부족 | stock=5, qty=10 | PRODUCING, updateStock 미호출 |
| PENDING 아닌 주문 승인 | status=CONFIRMED | `IllegalStateException` |
| 주문 거부 | PENDING 주문 | REJECTED, updateStatus 호출 |
| CONFIRMED 주문 출고 | status=CONFIRMED | RELEASE |
| PRODUCING 주문 출고 | status=PRODUCING | RELEASE |

### 2-2. OrderRepositoryTest

| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 저장 후 조회 | Order(`ORD-0001`) | findById → 동일 객체 |
| 전체 목록 조회 | 2개 저장 | findAll → size=2 |
| 상태별 조회 | PENDING 1개, CONFIRMED 1개 | findByStatus(PENDING) → size=1 |
| 상태 수정 | PENDING → CONFIRMED | findById → status=CONFIRMED |

---

## 3. 생산 관리 (Production)

### 3-1. ProductionControllerTest

| 시나리오 | 조건 | 기대 결과 |
|----------|------|-----------|
| 생산 완료 처리 | 미완료 생산 존재 | order→CONFIRMED, stock += (prodQty - orderQty) |
| 생산량 계산 | qty=10, yield=0.9 | ceil(10/(0.9×0.9)) = ceil(12.35) = 13 |
| 생산량 계산 | qty=100, yield=1.0 | ceil(100/(1.0×0.9)) = ceil(111.1) = 112 |
| FIFO 순서 | 주문A(t=100), 주문B(t=50) | B가 먼저 처리 |
| 이미 완료된 생산 | completed=true | `IllegalStateException` |

### 3-2. ProductionRepositoryTest

| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 저장 후 조회 | Production(`PRD-0001`) | findById → 동일 객체 |
| 주문 ID로 조회 | orderId=`ORD-0001` | findByOrderId → production |
| FIFO 큐 조회 | 미완료 3개 (서로 다른 createdAt) | 오래된 순서 반환 |
| 완료 처리 | setCompleted=true | findById → completed=true |

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
   → Production 생성: prodQty=ceil(10/0.81)=13, estimatedHours=26h
4. 생산 완료: stock += (13-10)=3 → stock=8, ORD-0001 → CONFIRMED
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
| 수율 = 1.0 | prodQty = ceil(qty/0.9) |
| 수율 = 0.5 | prodQty = ceil(qty/0.45), 더 많이 생산 |
| 존재하지 않는 주문 승인 | `IllegalArgumentException` |
| CONFIRMED 주문을 다시 승인 | `IllegalStateException` |
| REJECTED 주문 출고 시도 | `IllegalStateException` |

---

## 6. 실행 방법

```bash
# 전체 테스트
./gradlew test

# 특정 클래스만
./gradlew test --tests "ssemi.controller.OrderControllerTest"

# 상세 출력
./gradlew test --info

# 결과 리포트
build/reports/tests/test/index.html
```
