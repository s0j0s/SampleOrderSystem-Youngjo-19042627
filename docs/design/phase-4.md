# Phase 4 설계 — Controller 완성

**목표:** 비즈니스 로직 전체 구현 완성 + Mockito 기반 Controller 테스트 전체 통과  
**상태:** 설계 완료 / 구현 대기

---

## Explore 결과

### 현재 Controller 상태

| 파일 | 구현 상태 |
|------|-----------|
| `controller/SampleController.java` | `registerSample`, `getAllSamples`, `getSampleById` ✅ / `searchByName` ❌ 없음 |
| `controller/OrderController.java` | `createOrder`, `approveOrder`, `rejectOrder`, `releaseOrder` ✅ / 재고 부족 시 Production 생성 ❌ 미구현 |
| `controller/ProductionController.java` | ❌ 파일 없음 |

### 핵심 갭: `OrderController.approveOrder()` L64-66

```java
} else {
    orderRepository.updateStatus(orderId, OrderStatus.PRODUCING);
    order.setStatus(OrderStatus.PRODUCING);
    // ← Production 레코드 생성 로직 없음
}
```

### 현재 Controller 테스트 상태

| 파일 | 테스트 수 | 누락 |
|------|-----------|------|
| `SampleControllerTest.java` | 4개 통과 | `searchByName` 테스트 없음 |
| `OrderControllerTest.java` | 6개 통과 | `재고_부족_시_승인하면_PRODUCING` — `productionRepository.save()` 미검증, `ProductionRepository` Mock 없음 |
| `ProductionControllerTest.java` | ❌ 없음 | — |

---

## 설계 결정

### D-1. 생산량 공식

```
productionQty  = (int) Math.ceil(orderQuantity / (yield * 0.9))
estimatedHours = (long) sample.getProductionTime() * productionQty
```

**출처:** PRD.md L168 — `생산량 = ceil(주문수량 / (수율 × 0.9))` 명시.

검증 예시:
- yield=0.9, qty=10 → ceil(10 / 0.81) = ceil(12.35) = **13**, estimatedHours = 2 * 13 = 26
- yield=1.0, qty=10 → ceil(10 / 0.90) = ceil(11.11) = **12**
- yield=0.5, qty=10 → ceil(10 / 0.45) = ceil(22.22) = **23**

### D-2. OrderController 생성자 — ProductionRepository 추가 주입

기존 `(OrderRepository, SampleRepository)` → `(OrderRepository, SampleRepository, ProductionRepository)`로 변경.  
`@InjectMocks`를 쓰는 기존 테스트는 `@Mock ProductionRepository productionRepository` 필드 추가만으로 호환됨.

### D-3. completeProduction 잉여 재고 처리

```
surplus  = productionQty - orderQuantity   // 항상 >= 0 (공식상 productionQty >= orderQuantity)
newStock = sample.getStock() + surplus
```

잉여분을 재고에 입고 후 주문 상태 PRODUCING → CONFIRMED.

### D-4. completeProduction 선행 조건 검증

1. `findById` 없으면 → `IllegalArgumentException`
2. `production.isCompleted() == true` → `IllegalStateException` (이미 완료)
3. 연관 Order 조회 후 `order.getStatus() != PRODUCING` → `IllegalStateException` (상태 불일치)
4. 연관 Order, Sample 조회 실패 → `IllegalArgumentException`

### D-5-수정. surplus > 0 가드

```java
if (surplus > 0) {
    sampleRepository.updateStock(sample.getSampleId(), sample.getStock() + surplus);
}
```

surplus == 0(생산량 = 주문량 정확히 일치) 시 불필요한 DB write 방지.

### D-5. 테스트 격리

`ProductionControllerTest`는 Mockito만 사용 (H2 불필요, Repository 전부 Mock).

---

## 구현 명세

### 4-1. `SampleController.java` — `searchByName` 추가

```java
public List<Sample> searchByName(String keyword) {
    return sampleRepository.searchByName(keyword);
}
```

### 4-2. `OrderController.java` — 생성자 + `approveOrder` 수정

**생성자 변경:**
```java
private final ProductionRepository productionRepository;

public OrderController(OrderRepository orderRepository,
                       SampleRepository sampleRepository,
                       ProductionRepository productionRepository) {
    this.orderRepository = orderRepository;
    this.sampleRepository = sampleRepository;
    this.productionRepository = productionRepository;
}
```

**`approveOrder()` 재고 부족 분기 교체 (기존 L64-66 대체):**
```java
} else {
    int productionQty = (int) Math.ceil(
            order.getQuantity() / (sample.getYield() * 0.9));
    long estimatedHours = (long) sample.getProductionTime() * productionQty;
    String productionId = String.format("PRD-%04d", productionRepository.nextSequence());
    productionRepository.save(new Production(
            productionId, orderId, sample.getSampleId(),
            productionQty, estimatedHours, false));
    orderRepository.updateStatus(orderId, OrderStatus.PRODUCING);
    order.setStatus(OrderStatus.PRODUCING);
}
```

추가 import: `ssemi.model.Production`, `ssemi.repository.ProductionRepository`

### 4-3. `ProductionController.java` — 신규

```java
package ssemi.controller;

import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Production;
import ssemi.model.Sample;
import ssemi.repository.OrderRepository;
import ssemi.repository.ProductionRepository;
import ssemi.repository.SampleRepository;

import java.util.List;

public class ProductionController {

    private final ProductionRepository productionRepository;
    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public ProductionController(ProductionRepository productionRepository,
                                OrderRepository orderRepository,
                                SampleRepository sampleRepository) {
        this.productionRepository = productionRepository;
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
    }

    public List<Production> getPendingQueue() {
        return productionRepository.findPendingByFifo();
    }

    public Production completeProduction(String productionId) {
        Production production = productionRepository.findById(productionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 생산: " + productionId));

        if (production.isCompleted()) {
            throw new IllegalStateException("이미 완료된 생산: " + productionId);
        }

        Order order = orderRepository.findById(production.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문: " + production.getOrderId()));

        Sample sample = sampleRepository.findById(production.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료: " + production.getSampleId()));

        if (order.getStatus() != OrderStatus.PRODUCING) {
            throw new IllegalStateException("PRODUCING 상태 주문의 생산만 완료 처리 가능: " + order.getStatus());
        }

        productionRepository.complete(productionId);
        production.setCompleted(true);

        int surplus = production.getProductionQty() - order.getQuantity();
        if (surplus > 0) {
            sampleRepository.updateStock(sample.getSampleId(), sample.getStock() + surplus);
        }

        orderRepository.updateStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.CONFIRMED);

        return production;
    }
}
```

### 4-4. 테스트 수정/신규

#### `SampleControllerTest.java` — 테스트 1개 추가

```java
@Test
void 키워드로_시료_검색() {
    List<Sample> expected = List.of(new Sample("S-001", "GaN 웨이퍼", "4인치", 50, 0.9, 2));
    when(sampleRepository.searchByName("GaN")).thenReturn(expected);

    List<Sample> result = sampleController.searchByName("GaN");

    assertEquals(1, result.size());
    verify(sampleRepository).searchByName("GaN");
}
```

#### `OrderControllerTest.java` — Mock 추가 + 기존 테스트 검증 보강

필드 추가:
```java
@Mock private ProductionRepository productionRepository;
```

`재고_부족_시_승인하면_PRODUCING` 테스트에 검증 추가:
```java
verify(productionRepository).nextSequence();
verify(productionRepository).save(any(Production.class));
```

#### `ProductionControllerTest.java` — 신규 (5개 테스트)

픽스처:
```java
Production pendingProd = new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, false);
Order producingOrder   = new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.PRODUCING, 0L);
Sample sample          = new Sample("S-001", "GaN", "4인치", 0, 0.9, 2);
```

| 테스트 메서드 | 검증 내용 |
|-------------|-----------|
| `생산_완료_처리_성공` | `complete()`, `updateStock()`, `updateStatus(CONFIRMED)` 호출 검증, 반환 production.isCompleted()==true |
| `생산_완료_시_잉여재고_입고` | prodQty=13, orderQty=10, stock=0 → `updateStock("S-001", 3)` |
| `잉여없을때_updateStock_미호출` | prodQty==orderQty인 경우 → `updateStock` never 호출 |
| `이미_완료된_생산_완료_시_예외` | completed=true인 Production → `IllegalStateException` |
| `PRODUCING_아닌_주문_완료_시_예외` | order.status=CONFIRMED → `IllegalStateException` |
| `존재하지_않는_생산ID_완료_시_예외` | `findById` empty → `IllegalArgumentException` |
| `미완료_생산_큐_조회` | `getPendingQueue()` → `findPendingByFifo()` 위임 검증 |

---

## 영향 범위

| 파일 | 변경 내용 |
|------|-----------|
| `controller/SampleController.java` | `searchByName` 메서드 추가 |
| `controller/OrderController.java` | 생성자에 `ProductionRepository` 추가, `approveOrder` 재고 부족 분기 구현 |
| `controller/ProductionController.java` | **신규 생성** |
| `test/controller/SampleControllerTest.java` | `searchByName` 테스트 1개 추가 |
| `test/controller/OrderControllerTest.java` | `@Mock ProductionRepository` 추가, 기존 테스트 검증 보강 |
| `test/controller/ProductionControllerTest.java` | **신규 생성** (테스트 5개) |

`Main.java` — Phase 5 범위 (이번 Phase에서 변경 없음)

---

## 검증 기준

```bash
./gradlew test
```

- 기존 테스트 전체 유지 (깨지면 안 됨)
- `SampleControllerTest`: 5개 통과 (기존 4 + 신규 1)
- `OrderControllerTest`: 6개 통과 (검증만 보강, 구조 유지)
- `ProductionControllerTest`: 7개 통과 (신규)
- 전체: 기존 대비 **+8개** 이상

---

## 검토 체크리스트

> Action 진행 전 아래 항목을 확인하고 승인해주세요.

### SampleController
- [x] `searchByName`을 Repository에 단순 위임하는 방향이 맞는가?

### OrderController
- [x] 생산량 공식 `ceil(qty / (yield * 0.9))`에 동의하는가? — PRD.md L168 확인
- [x] `estimatedHours = productionTime * productionQty` 계산 방식이 맞는가?
- [x] `approveOrder`에서 Production 생성 후 `updateStatus` 순서가 맞는가? (save → updateStatus)

### ProductionController
- [x] ~~`completeProduction`에서 PRODUCING 상태 검증 없이 `isCompleted` 여부만 체크하는 방향이 맞는가?~~ → **PRODUCING 상태 검증 추가로 결정**
- [x] 잉여 재고 공식 `surplus = productionQty - orderQuantity` — PRD L173 확인
- [x] ~~surplus가 0인 경우도 `updateStock` 호출~~ → **surplus > 0 가드 추가로 결정**

### 테스트
- [x] `OrderControllerTest.재고_부족_시_승인하면_PRODUCING`에 `productionRepository.save()` 검증 추가
- [x] `ProductionControllerTest` 픽스처: `sample.stock=0`으로 설정해 surplus만큼만 증가하는 방식 확정
