# Phase 6 — 테스트 완성 설계

## 목표

개수 기준이 아닌 **의미있는 커버리지 100%** 달성.  
대상: 모든 public 메서드 호출, 모든 비즈니스 분기, 모든 예외 경로.  
제외: `SQLException` 경로 (인프라 장애 시뮬레이션 — H2 in-memory 테스트 범위 밖).

---

## 현재 테스트 현황 (35개 통과)

| 파일 | 테스트 수 |
|------|-----------|
| `SampleRepositoryTest` | 6 |
| `OrderRepositoryTest` | 4 |
| `ProductionRepositoryTest` | 6 |
| `SampleControllerTest` | 5 |
| `OrderControllerTest` | 7 |
| `ProductionControllerTest` | 7 |
| **합계** | **35** |

---

## 커버리지 갭 분석

### OrderController — 미테스트 메서드 3개

| 메서드 | 현황 |
|--------|------|
| `getAllOrders()` | 미테스트 — 단순 위임이나 호출 검증 없음 |
| `getOrderById()` | 미테스트 |
| `getOrdersByStatus()` | 미테스트 |

### OrderController — 미테스트 분기/예외

| 메서드 | 미테스트 경로 |
|--------|--------------|
| `approveOrder()` | stock == qty 경계값 → CONFIRMED (updateStock(0)) |
| `approveOrder()` | 존재하지 않는 orderId → `IllegalArgumentException` |
| `approveOrder()` | yield=1.0 생산량 공식 검증 |
| `approveOrder()` | yield=0.5 생산량 공식 검증 |
| `releaseOrder()` | status=REJECTED → `IllegalStateException` |
| `releaseOrder()` | status=PRODUCING → `IllegalStateException` |
| `rejectOrder()` | status=CONFIRMED → `IllegalStateException` |

### ProductionController — 미테스트 메서드

| 메서드 | 미테스트 경로 |
|--------|--------------|
| `checkAndCompleteExpired()` | 만료 조건 충족 → `completeProduction()` 자동 호출 |
| `checkAndCompleteExpired()` | 만료 미충족 → `complete()` 미호출 |
| `checkAndCompleteExpired()` | 예외 발생 항목 무시, 다음 항목 계속 처리 |

### SampleRepository — 미테스트 메서드

| 메서드 | 미테스트 경로 |
|--------|--------------|
| `update()` | 전체 필드 수정 |
| `searchByName()` | 결과 없음 → 빈 리스트 |

### OrderRepository — 미테스트 분기

| 메서드 | 미테스트 경로 |
|--------|--------------|
| `findByStatus()` | 결과 없음 → 빈 리스트 |
| `findByStatus()` | FIFO 정렬 (CREATED_AT ASC) 검증 |

### ProductionRepository — 미테스트 분기

| 메서드 | 미테스트 경로 |
|--------|--------------|
| `findPendingByFifo()` | 결과 없음 → 빈 리스트 |

### test-scenarios.md 오류 정정

`test-scenarios.md` §2-1에 "PRODUCING 주문 출고 → RELEASE"로 기재되어 있으나 잘못됨.  
`OrderController.releaseOrder()` 구현은 `CONFIRMED` 외 상태에서 `IllegalStateException` 발생.  
상태 기계(`CLAUDE.md`): `PRODUCING → CONFIRMED → RELEASE` (직접 출고 불가).  
→ **올바른 테스트:** PRODUCING → `IllegalStateException`

---

## 추가 테스트 설계 (총 +17개)

### `SampleRepositoryTest` +2

#### `시료_전체필드_수정`
```
given: S-001 저장 (GaN, 4인치, stock=50, yield=0.9, productionTime=2)
when:  update(S-001, name="InP", spec="3인치", stock=100, yield=0.8, productionTime=3)
then:  findById("S-001") → name="InP", spec="3인치", stock=100, yield=0.8, productionTime=3
```

#### `이름_검색_결과없음`
```
given: S-001("GaN 웨이퍼"), S-002("SiC 웨이퍼") 저장
when:  searchByName("InP")
then:  빈 List 반환
```

---

### `OrderRepositoryTest` +2

#### `상태별_조회_결과없음`
```
given: ORD-0001(RESERVED) 1개 저장
when:  findByStatus(CONFIRMED)
then:  빈 List 반환
```

#### `FIFO_정렬_확인`
```
given: ORD-0001(createdAt=2000L, RESERVED), ORD-0002(createdAt=1000L, RESERVED) 저장
when:  findByStatus(RESERVED)
then:  result[0].orderId = "ORD-0002"  ← 오래된 것이 먼저
       result[1].orderId = "ORD-0001"
```

---

### `ProductionRepositoryTest` +1

#### `미완료_목록_없으면_빈_리스트`
```
given: 저장된 Production 없음
when:  findPendingByFifo()
then:  빈 List 반환
```

---

### `OrderControllerTest` +10

#### `전체_주문_목록_반환`
```
given: orderRepository.findAll() returns [order1, order2]
when:  getAllOrders()
then:  결과 size=2, verify findAll() 호출
```

#### `ID로_주문_조회`
```
given: orderRepository.findById("ORD-0001") returns Optional.of(order)
when:  getOrderById("ORD-0001")
then:  Optional.of(order) 반환
```

#### `상태별_주문_조회`
```
given: orderRepository.findByStatus(RESERVED) returns [order]
when:  getOrdersByStatus(RESERVED)
then:  결과 size=1, verify findByStatus(RESERVED) 호출
```

#### `재고와_주문수량_정확히_같을때_CONFIRMED`
```
given: sample(stock=10), order(qty=10, RESERVED)
when:  approveOrder("ORD-0001")
then:  status = CONFIRMED
       verify updateStock("S-001", 0) 호출
       verify productionRepository.save() 미호출
```

#### `재고_부족_수율_1점0_생산량_계산`
```
given: sample(stock=0, yield=1.0, productionTime=1), order(qty=100, RESERVED)
when:  approveOrder("ORD-0001")
then:  status = PRODUCING
       verify save(production) — production.productionQty = 112
             ( ceil(100 / (1.0 × 0.9)) = ceil(111.11) = 112 )
```

#### `재고_부족_수율_0점5_생산량_계산`
```
given: sample(stock=0, yield=0.5, productionTime=1), order(qty=10, RESERVED)
when:  approveOrder("ORD-0001")
then:  status = PRODUCING
       verify save(production) — production.productionQty = 23
             ( ceil(10 / (0.5 × 0.9)) = ceil(22.22) = 23 )
```

#### `존재하지_않는_주문_승인_시_예외`
```
given: orderRepository.findById("ORD-9999") = Optional.empty()
when:  approveOrder("ORD-9999")
then:  IllegalArgumentException 발생
```

#### `REJECTED_주문_출고_시_예외`
```
given: order(status=REJECTED)
when:  releaseOrder("ORD-0001")
then:  IllegalStateException 발생
```

#### `PRODUCING_주문_출고_시_예외`
```
given: order(status=PRODUCING)
when:  releaseOrder("ORD-0001")
then:  IllegalStateException 발생
```

#### `CONFIRMED_주문_거부_시_예외`
```
given: order(status=CONFIRMED)
when:  rejectOrder("ORD-0001")
then:  IllegalStateException 발생
```

---

### `ProductionControllerTest` +3 (미테스트 메서드/분기)

#### `checkAndCompleteExpired_만료된_생산_자동완료`
```
given: production(startedAt=1L, estimatedHours=1L, completed=false)
       now >> 1 + 3_600_000 → 만료 조건 충족
       관련 Order(PRODUCING), Sample mock 세팅
when:  checkAndCompleteExpired()
then:  verify productionRepository.complete("PRD-0001") 호출
       verify orderRepository.updateStatus("ORD-0001", CONFIRMED) 호출
```

#### `checkAndCompleteExpired_미만료_생산_완료_미호출`
```
given: production(startedAt=System.currentTimeMillis(), estimatedHours=100L)
       → now < startedAt + 360_000_000ms → 만료 미충족
when:  checkAndCompleteExpired()
then:  verify productionRepository.complete() never called
```

#### `checkAndCompleteExpired_예외발생시_다음항목_계속_처리`
```
given: [prod1(만료, findById→empty→IAE 유발), prod2(만료, 정상처리)]
       findPendingByFifo → [prod1, prod2]
when:  checkAndCompleteExpired()
then:  verify complete("PRD-0002") 호출
       verify complete("PRD-0001") never called
```

---

## 변경 파일 목록

| 파일 | 추가 수 |
|------|---------|
| `SampleRepositoryTest.java` | +2 |
| `OrderRepositoryTest.java` | +2 |
| `ProductionRepositoryTest.java` | +1 |
| `OrderControllerTest.java` | +10 |
| `ProductionControllerTest.java` | +3 |
| **합계** | **+17** |

신규 파일: 없음.

---

## 수동 검증 시나리오

`./run.bat` 실행 후 아래 4개 시나리오 순서대로 수행.

### 시나리오 A — 정상 흐름 (재고 충분)

```
[1] 시료 등록: GaN 웨이퍼, 4인치, 재고=50, 수율=0.9, 단위생산시간=2
[2] 주문 접수: GaN 웨이퍼 선택, CUST-001, qty=10
[3] 주문 승인: ORD-0001 → 승인 → CONFIRMED, 재고=40 확인
[6] 출고 처리: ORD-0001 → RELEASE 확인
```

### 시나리오 B — 생산 흐름 (재고 부족)

```
[1] 시료 등록: SiC 웨이퍼, 6인치, 재고=5, 수율=0.9, 단위생산시간=2
[2] 주문 접수: SiC 웨이퍼, qty=10
[3] 주문 승인: → PRODUCING
               PRD-XXXX 생성 확인
               shortageQty=5, prodQty=ceil(5/0.81)=7, estimatedHours=14h
[5] 생산 완료: PRD-XXXX → ORD-XXXX CONFIRMED, 잉여=0 (updateStock 미호출)
[6] 출고 처리: ORD-XXXX → RELEASE
```

### 시나리오 C — 주문 거부

```
[2] 주문 접수: 기존 시료, qty=5
[3] 주문 처리: 거부 → REJECTED
[4] 모니터링: REJECTED 주문 표시 확인
```

### 시나리오 D — FIFO 생산 순서

```
[1] 시료 등록: stock=0, yield=0.9
[2] 주문 접수 2건: ORD-A(qty=5), ORD-B(qty=3)
[3] ORD-A 승인 → PRODUCING (PRD-A), ORD-B 승인 → PRODUCING (PRD-B)
[5] 생산 큐 조회: PRD-A가 PRD-B보다 먼저 표시 (FIFO 확인)
```

---

## 완료 기준

- [ ] `./gradlew test` 전체 통과 (목표: 52개 이상)
- [ ] 모든 Controller public 메서드 1회 이상 호출
- [ ] 모든 비즈니스 분기 (if/else 조건) 양쪽 모두 커버
- [ ] 모든 `IllegalArgumentException` / `IllegalStateException` 경로 커버
- [ ] `checkAndCompleteExpired()` — 만료/미만료/예외계속 3케이스 커버
- [ ] 수동 검증 시나리오 A~D 오류 없이 완료

---

## 테스트 코드 스텁

### `OrderControllerTest` 추가

```java
@Test
void 전체_주문_목록_반환() {
    Order order1 = new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.RESERVED, 0L);
    Order order2 = new Order("ORD-0002", "S-001", "CUST-002", 5,  OrderStatus.CONFIRMED, 0L);
    when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

    List<Order> result = orderController.getAllOrders();

    assertEquals(2, result.size());
    verify(orderRepository).findAll();
}

@Test
void ID로_주문_조회() {
    Order order = new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.RESERVED, 0L);
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(order));

    Optional<Order> result = orderController.getOrderById("ORD-0001");

    assertTrue(result.isPresent());
    assertEquals("ORD-0001", result.get().getOrderId());
}

@Test
void 상태별_주문_조회() {
    Order order = new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.RESERVED, 0L);
    when(orderRepository.findByStatus(OrderStatus.RESERVED)).thenReturn(List.of(order));

    List<Order> result = orderController.getOrdersByStatus(OrderStatus.RESERVED);

    assertEquals(1, result.size());
    verify(orderRepository).findByStatus(OrderStatus.RESERVED);
}

@Test
void 재고와_주문수량_정확히_같을때_CONFIRMED() {
    Sample exactSample = new Sample("S-001", "GaN 웨이퍼", "4인치", 10, 0.9, 2);
    Order reservedOrder = new Order("ORD-0001", "S-001", "CUST-001", 10,
            OrderStatus.RESERVED, System.currentTimeMillis());
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(reservedOrder));
    when(sampleRepository.findById("S-001")).thenReturn(Optional.of(exactSample));

    Order result = orderController.approveOrder("ORD-0001");

    assertEquals(OrderStatus.CONFIRMED, result.getStatus());
    verify(sampleRepository).updateStock(eq("S-001"), eq(0));
    verify(productionRepository, never()).save(any());
}

@Test
void 재고_부족_수율_1점0_생산량_계산() {
    Sample highYieldSample = new Sample("S-001", "GaN 웨이퍼", "4인치", 0, 1.0, 1);
    Order reservedOrder = new Order("ORD-0001", "S-001", "CUST-001", 100,
            OrderStatus.RESERVED, System.currentTimeMillis());
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(reservedOrder));
    when(sampleRepository.findById("S-001")).thenReturn(Optional.of(highYieldSample));
    when(productionRepository.nextSequence()).thenReturn(1);

    orderController.approveOrder("ORD-0001");

    // ceil(100 / (1.0 * 0.9)) = ceil(111.11) = 112
    verify(productionRepository).save(argThat(p -> p.getProductionQty() == 112));
}

@Test
void 재고_부족_수율_0점5_생산량_계산() {
    Sample lowYieldSample = new Sample("S-001", "GaN 웨이퍼", "4인치", 0, 0.5, 1);
    Order reservedOrder = new Order("ORD-0001", "S-001", "CUST-001", 10,
            OrderStatus.RESERVED, System.currentTimeMillis());
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(reservedOrder));
    when(sampleRepository.findById("S-001")).thenReturn(Optional.of(lowYieldSample));
    when(productionRepository.nextSequence()).thenReturn(1);

    orderController.approveOrder("ORD-0001");

    // ceil(10 / (0.5 * 0.9)) = ceil(22.22) = 23
    verify(productionRepository).save(argThat(p -> p.getProductionQty() == 23));
}

@Test
void 존재하지_않는_주문_승인_시_예외() {
    when(orderRepository.findById("ORD-9999")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
            () -> orderController.approveOrder("ORD-9999"));
}

@Test
void REJECTED_주문_출고_시_예외() {
    Order rejectedOrder = new Order("ORD-0001", "S-001", "CUST-001", 10,
            OrderStatus.REJECTED, System.currentTimeMillis());
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(rejectedOrder));

    assertThrows(IllegalStateException.class,
            () -> orderController.releaseOrder("ORD-0001"));
}

@Test
void PRODUCING_주문_출고_시_예외() {
    Order producingOrder = new Order("ORD-0001", "S-001", "CUST-001", 10,
            OrderStatus.PRODUCING, System.currentTimeMillis());
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(producingOrder));

    assertThrows(IllegalStateException.class,
            () -> orderController.releaseOrder("ORD-0001"));
}

@Test
void CONFIRMED_주문_거부_시_예외() {
    Order confirmedOrder = new Order("ORD-0001", "S-001", "CUST-001", 10,
            OrderStatus.CONFIRMED, System.currentTimeMillis());
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(confirmedOrder));

    assertThrows(IllegalStateException.class,
            () -> orderController.rejectOrder("ORD-0001"));
}
```

### `ProductionControllerTest` 추가

```java
@Test
void checkAndCompleteExpired_만료된_생산_자동완료() {
    Production expired = new Production("PRD-0001", "ORD-0001", "S-001",
            13, 1L, false, 1L, 5);
    when(productionRepository.findPendingByFifo()).thenReturn(List.of(expired));
    when(productionRepository.findById("PRD-0001")).thenReturn(Optional.of(expired));
    when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(producingOrder));
    when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

    productionController.checkAndCompleteExpired();

    verify(productionRepository).complete("PRD-0001");
    verify(orderRepository).updateStatus("ORD-0001", OrderStatus.CONFIRMED);
}

@Test
void checkAndCompleteExpired_미만료_생산_완료_미호출() {
    long now = System.currentTimeMillis();
    Production fresh = new Production("PRD-0001", "ORD-0001", "S-001",
            13, 100L, false, now, 5);
    when(productionRepository.findPendingByFifo()).thenReturn(List.of(fresh));

    productionController.checkAndCompleteExpired();

    verify(productionRepository, never()).complete(any());
}

@Test
void checkAndCompleteExpired_예외발생시_다음항목_계속_처리() {
    Production prod1 = new Production("PRD-0001", "ORD-0001", "S-001", 13, 1L, false, 1L, 5);
    Production prod2 = new Production("PRD-0002", "ORD-0002", "S-001",  7, 1L, false, 1L, 3);
    when(productionRepository.findPendingByFifo()).thenReturn(List.of(prod1, prod2));
    when(productionRepository.findById("PRD-0001")).thenReturn(Optional.empty());  // IAE 유발
    Order producingOrder2 = new Order("ORD-0002", "S-001", "CUST-002", 5, OrderStatus.PRODUCING, 0L);
    when(productionRepository.findById("PRD-0002")).thenReturn(Optional.of(prod2));
    when(orderRepository.findById("ORD-0002")).thenReturn(Optional.of(producingOrder2));
    when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

    productionController.checkAndCompleteExpired();

    verify(productionRepository, never()).complete("PRD-0001");
    verify(productionRepository).complete("PRD-0002");
}
```

### `SampleRepositoryTest` 추가

```java
@Test
void 시료_전체필드_수정() {
    repository.save(new Sample("S-001", "GaN 웨이퍼", "4인치", 50, 0.9, 2));
    repository.update(new Sample("S-001", "InP 웨이퍼", "3인치", 100, 0.8, 3));

    Optional<Sample> found = repository.findById("S-001");
    assertTrue(found.isPresent());
    assertEquals("InP 웨이퍼", found.get().getName());
    assertEquals("3인치",      found.get().getSpec());
    assertEquals(100,          found.get().getStock());
    assertEquals(0.8,          found.get().getYield(), 1e-9);
    assertEquals(3,            found.get().getProductionTime());
}

@Test
void 이름_검색_결과없음() {
    repository.save(new Sample("S-001", "GaN 웨이퍼", "4인치", 30, 0.9, 2));
    repository.save(new Sample("S-002", "SiC 웨이퍼", "6인치", 20, 0.8, 3));

    List<Sample> result = repository.searchByName("InP");

    assertTrue(result.isEmpty());
}
```

### `OrderRepositoryTest` 추가

```java
@Test
void 상태별_조회_결과없음() {
    repository.save(new Order("ORD-0001", "S-001", "CUST-001", 10,
            OrderStatus.RESERVED, 1000L));

    List<Order> result = repository.findByStatus(OrderStatus.CONFIRMED);

    assertTrue(result.isEmpty());
}

@Test
void FIFO_정렬_확인() {
    repository.save(new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.RESERVED, 2000L));
    repository.save(new Order("ORD-0002", "S-001", "CUST-002",  5, OrderStatus.RESERVED, 1000L));

    List<Order> result = repository.findByStatus(OrderStatus.RESERVED);

    assertEquals(2, result.size());
    assertEquals("ORD-0002", result.get(0).getOrderId());  // 오래된 것이 먼저
    assertEquals("ORD-0001", result.get(1).getOrderId());
}
```

### `ProductionRepositoryTest` 추가

```java
@Test
void 미완료_목록_없으면_빈_리스트() {
    List<Production> result = repository.findPendingByFifo();

    assertTrue(result.isEmpty());
}
```
