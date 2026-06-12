# Phase 1 설계 — 도메인 모델 완성

**목표:** 코드의 도메인 객체를 PRD 엔티티 정의와 완전히 일치시킨다  
**상태:** 설계 완료 / 구현 대기

---

## Explore 결과

### 현재 상태 vs PRD 요구사항

#### OrderStatus.java
```
현재                  PRD 요구
------                ----------
PENDING          →   RESERVED   (이름 변경)
APPROVED         →   (삭제)     (PRD에 없는 상태)
CONFIRMED        →   CONFIRMED  (유지)
PRODUCING        →   PRODUCING  (유지)
RELEASED         →   RELEASE    (이름 변경)
REJECTED         →   REJECTED   (유지)
```

#### Sample.java
```
현재 필드            PRD 요구 필드
-----------          ---------------
sampleId             sampleId        (유지)
name                 name            (유지)
spec                 spec            (유지)
stock                stock           (유지)
(없음)               double yield    ← 추가
(없음)               int productionTime ← 추가
```

#### Order.java
```
현재 필드            PRD 요구 필드
-----------          ---------------
orderId              orderId         (유지)
sampleId             sampleId        (유지)
customerId           customerId      (유지)
quantity             quantity        (유지)
status               status          (유지)
(없음)               long createdAt  ← 추가
```

#### Production.java
```
현재: 파일 없음 → 신규 생성 필요
```

---

## 설계 결정

### D-1. Lombok 도입
**결정:** 모든 모델 클래스에 Lombok 적용  
**사용 어노테이션:**

| 어노테이션 | 용도 |
|-----------|------|
| `@Getter` | 모든 필드 getter 자동 생성 |
| `@Setter` | 가변 필드에만 개별 적용 |
| `@AllArgsConstructor` | 전체 파라미터 생성자 |
| `@ToString` | toString() 자동 생성 |
| `@RequiredArgsConstructor` | final 필드 생성자 (불필요 시 미사용) |

**이유:** 보일러플레이트(getter/setter/toString/생성자) 제거 → 필드와 비즈니스 로직에 집중.

### D-2. OrderStatus 변경 방식
**결정:** 직접 rename (PENDING→RESERVED, RELEASED→RELEASE, APPROVED 삭제)  
**이유:** 신규 프로젝트, 하위 호환 불필요.  
**영향 파일:**
- `model/OrderStatus.java`
- `controller/OrderController.java` (PENDING, RELEASED 참조)
- `test/controller/OrderControllerTest.java`
- `test/repository/OrderRepositoryTest.java`

### D-3. Sample 생성자 전략
**결정:** `@AllArgsConstructor` 하나만 사용 (6 파라미터)  
**이유:** `yield=0.0` 기본값 허용 시 생산량 계산에서 divide-by-zero 위험. 모든 필드 필수.  
**영향:** 테스트 `new Sample(...)` 호출부 전체 수정.

### D-4. Order 생성자 전략
**결정:** `@AllArgsConstructor` (6 파라미터, createdAt 포함)  
**이유:** FIFO 정렬 핵심 필드 누락 방지.

### D-5. Production 불변 설계
**결정:** 생성 후 변경 불가 필드는 `@Getter`만, `completed`만 `@Setter` 허용  
**이유:** 생산 레코드 생성 후 수량/시간은 변경되지 않음.

---

## 구현 명세

### 1-1. `OrderStatus.java`

```java
package ssemi.model;

public enum OrderStatus {
    RESERVED,
    CONFIRMED,
    PRODUCING,
    RELEASE,
    REJECTED
}
```

### 1-2. `Sample.java`

```java
package ssemi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class Sample {
    private String sampleId;
    private String name;
    private String spec;
    private int stock;
    private double yield;
    private int productionTime;
}
```

> `@ToString` 기본 포맷 사용. 출력 형식이 중요한 경우 `@ToString(exclude=...)` 또는 수동 재정의 가능.

### 1-3. `Order.java`

```java
package ssemi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Order {
    private String orderId;
    private String sampleId;
    private String customerId;
    private int quantity;
    @Setter private OrderStatus status;
    private long createdAt;
}
```

> `status`만 변경 허용. 나머지 필드 setter 없음.

### 1-4. `Production.java` (신규)

```java
package ssemi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Production {
    private String productionId;
    private String orderId;
    private String sampleId;
    private int productionQty;
    private long estimatedHours;
    @Setter private boolean completed;
}
```

---

## 영향 범위 (수정 필요 파일)

| 파일 | 변경 내용 |
|------|-----------|
| `model/OrderStatus.java` | PENDING→RESERVED, APPROVED 삭제, RELEASED→RELEASE |
| `model/Sample.java` | Lombok 적용, yield/productionTime 추가, 생성자 변경 |
| `model/Order.java` | Lombok 적용, createdAt 추가, 생성자 변경 |
| `model/Production.java` | 신규 생성 |
| `controller/OrderController.java` | PENDING→RESERVED, RELEASED→RELEASE 참조 수정 |
| `test/controller/OrderControllerTest.java` | PENDING 참조, `new Sample(...)` 생성자 수정 |
| `test/controller/SampleControllerTest.java` | `new Sample(...)` 생성자 수정 |
| `test/repository/OrderRepositoryTest.java` | PENDING 참조, `new Sample/Order(...)` 생성자 수정 |
| `test/repository/SampleRepositoryTest.java` | `new Sample(...)` 생성자 수정 |

---

## 검증 기준

- `./gradlew build` 컴파일 에러 없음
- `./gradlew test` 기존 21개 테스트 전부 통과
- `Production.java` 파일 존재, Lombok 어노테이션 적용 확인
- `OrderStatus.RESERVED`, `OrderStatus.RELEASE` 접근 가능
- `sample.getYield()`, `sample.getProductionTime()` 호출 가능
- `order.getCreatedAt()` 호출 가능
- `production.isCompleted()`, `production.setCompleted(true)` 호출 가능
