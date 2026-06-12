# Architecture — 반도체 시료 생산주문관리 시스템

## 계층 구조

```
┌─────────────────────────────────────────────────┐
│  Main.java  (진입점, 메뉴 루프)                    │
└───────────────────┬─────────────────────────────┘
                    │ 사용
┌───────────────────▼─────────────────────────────┐
│  ConsoleView  (I/O 전담)                          │
│  - 메뉴 출력, 사용자 입력 수신                      │
│  - 결과 출력 (ANSI 컬러)                           │
└───────┬───────────────────┬─────────────────────┘
        │ 호출               │ 호출
┌───────▼──────┐   ┌────────▼────────┐   ┌──────────────────┐
│SampleCtrl    │   │OrderController  │   │ProductionCtrl    │
│- registerSample│  │- createOrder   │   │- completeProduction│
│- getAllSamples│   │- approveOrder  │   │- getPendingQueue  │
│- searchByName│   │- rejectOrder   │   └────────┬─────────┘
└───────┬──────┘   │- releaseOrder  │            │
        │          └────────┬───────┘            │
┌───────▼──────┐   ┌────────▼───────┐   ┌────────▼─────────┐
│SampleRepo    │   │OrderRepository │   │ProductionRepo    │
│(JDBC, H2)    │   │(JDBC, H2)      │   │(JDBC, H2)        │
└──────────────┘   └────────────────┘   └──────────────────┘
        │                   │                    │
        └───────────────────┴────────────────────┘
                            │
                ┌───────────▼──────────┐
                │  DatabaseManager     │
                │  H2 파일 DB           │
                │  ./data/ssemi        │
                └──────────────────────┘
```

---

## 패키지 구조

```
src/
├── main/java/ssemi/
│   ├── Main.java                       진입점, 메뉴 루프
│   │
│   ├── model/                          도메인 객체 (순수 데이터)
│   │   ├── Sample.java                 시료 (sampleId, name, spec, stock, yield, productionTime)
│   │   ├── Order.java                  주문 (orderId, sampleId, customerId, quantity, status, createdAt)
│   │   ├── Production.java             생산 (productionId, orderId, sampleId, qty, estimatedHours, completed)
│   │   └── OrderStatus.java            enum: RESERVED / CONFIRMED / PRODUCING / RELEASE / REJECTED
│   │
│   ├── controller/                     비즈니스 로직
│   │   ├── SampleController.java       시료 등록·조회·검색
│   │   ├── OrderController.java        주문 생성·승인·거부·출고
│   │   └── ProductionController.java   생산 큐 조회·완료 처리
│   │
│   ├── repository/                     DB 접근 (JDBC)
│   │   ├── SampleRepository.java       SAMPLE 테이블 CRUD
│   │   ├── OrderRepository.java        ORDERS 테이블 CRUD + 상태 조회
│   │   └── ProductionRepository.java   PRODUCTION 테이블 CRUD + FIFO 큐
│   │
│   ├── view/
│   │   └── ConsoleView.java            모든 콘솔 I/O, ANSI 컬러 출력
│   │
│   └── db/
│       └── DatabaseManager.java        H2 연결 관리, 스키마 초기화
│
└── test/java/ssemi/
    ├── controller/
    │   ├── SampleControllerTest.java   Mockito 기반 단위 테스트
    │   ├── OrderControllerTest.java
    │   └── ProductionControllerTest.java
    └── repository/
        ├── SampleRepositoryTest.java   H2 in-memory 통합 테스트
        ├── OrderRepositoryTest.java
        └── ProductionRepositoryTest.java
```

---

## 의존성 방향

```
Main → ConsoleView → Controller → Repository → DatabaseManager
                                ↘ Model (공유)
```

- **단방향**: 상위 레이어만 하위 레이어 참조
- **Model**: 모든 레이어가 참조 가능 (순수 데이터 객체)
- **View → Controller**: View가 Controller를 직접 호출 (MVC)

---

## DB 스키마

```
SAMPLE ──────────────────────────────────────────────
  SAMPLE_ID (PK, VARCHAR 15)
  NAME, SPEC, STOCK, YIELD, PRODUCTION_TIME

ORDERS ──────────────────────────────────────────────
  ORDER_ID (PK, VARCHAR 15)
  SAMPLE_ID (FK → SAMPLE)
  CUSTOMER_ID, QUANTITY, STATUS, CREATED_AT

PRODUCTION ──────────────────────────────────────────
  PRODUCTION_ID (PK, VARCHAR 15)
  ORDER_ID (FK → ORDERS, UNIQUE)
  SAMPLE_ID (FK → SAMPLE)
  PRODUCTION_QTY, ESTIMATED_HOURS, COMPLETED
```

관계:
- `ORDERS.SAMPLE_ID` → `SAMPLE.SAMPLE_ID` (N:1)
- `PRODUCTION.ORDER_ID` → `ORDERS.ORDER_ID` (1:1)

---

## 주요 설계 결정

### 1. 생성자 주입 (DI)

```java
public OrderController(OrderRepository orderRepository,
                       SampleRepository sampleRepository) { ... }
```

- 테스트에서 Mock 주입 용이
- 의존성 명시적, 순환 참조 방지

### 2. Repository에서 RuntimeException 래핑

```java
} catch (SQLException e) {
    throw new RuntimeException("주문 저장 실패: " + orderId, e);
}
```

- Controller/View가 checked exception을 직접 처리하지 않아도 됨
- 오류 메시지에 컨텍스트 포함

### 3. H2 in-memory (테스트) vs 파일 DB (운영)

```java
// 운영
new DatabaseManager()  // jdbc:h2:./data/ssemi

// 테스트
new DatabaseManager("jdbc:h2:mem:testdb_sample;DB_CLOSE_DELAY=-1")
```

- `DatabaseManager(String url)` 생성자로 테스트용 URL 주입
- 테스트 간 격리: `@AfterEach` DROP TABLE

### 4. FIFO 생산 큐

- `ORDERS.CREATED_AT` (epoch millis) 기준 ORDER BY
- `ProductionRepository.findPendingByFifo()` → `ORDER BY o.CREATED_AT ASC`
