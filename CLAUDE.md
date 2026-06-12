# CLAUDE.md — 반도체 시료 생산주문관리 시스템

## 프로젝트 개요

**시스템명:** 반도체 시료 생산주문관리 시스템 (SampleOrderSystem)  
**목적:** 반도체 시료의 생산 및 주문 흐름을 관리하는 콘솔 기반 MVC 애플리케이션  
**도메인:** 시료(Sample) 재고 관리, 주문(Order) 생성 및 상태 처리

## POC 참조

| POC | 경로 | 핵심 패턴 |
|-----|------|-----------|
| ConsoleMVC | `../ConsoleMVC-Youngjo-19042627` | MVC 구조, OrderStatus 상태기계 |
| DataPersistence | `../DataPersistence-Youngjo-19042627` | Repository 패턴, JDBC PreparedStatement |
| DataMonitor | `../DataMonitor-Youngjo-19042627` | ANSI 컬러 출력, 스케줄링 |
| DummyDataGenerator | `../DummyDataGenerator-Youngjo-19042627` | 템플릿 기반 데이터 생성 |

---

## 기술 스택

| 항목 | 버전/도구 |
|------|-----------|
| Language | Java 17 |
| Build | Gradle 8.x (application plugin) |
| DB | H2 2.2.224 (file-based, `./data/ssemi`) |
| Test DB | H2 in-memory (`jdbc:h2:mem:testdb`) |
| Unit Test | JUnit 5.10.x |
| Mocking | Mockito 5.x |
| UI | Console (ANSI color codes) |

### 빌드 및 실행

```bash
./gradlew run          # 애플리케이션 실행
./gradlew test         # 전체 테스트 실행
./gradlew test --info  # 상세 테스트 출력
./gradlew build        # 빌드 + 테스트
```

---

## 아키텍처

### 레이어 구조

```
Main (진입점, 메뉴 루프)
  └── ConsoleView (모든 I/O, 입력 검증)
        ├── OrderController (주문 비즈니스 로직)
        │     └── OrderRepository (JDBC, H2)
        └── SampleController (시료 비즈니스 로직)
              └── SampleRepository (JDBC, H2)
```

### 결정 이유
- **MVC 분리**: View는 I/O만, Controller는 비즈니스 로직만 담당 → 테스트 용이
- **Repository 패턴**: DB 구현을 교체 가능하도록 추상화 → 테스트 시 Mock 사용
- **생성자 주입**: 의존성을 명시적으로 관리, 테스트에서 Mock 주입 용이

### 패키지 구조

```
src/main/java/ssemi/
├── Main.java
├── model/
│   ├── Order.java
│   ├── Sample.java
│   └── OrderStatus.java          (enum)
├── controller/
│   ├── OrderController.java
│   └── SampleController.java
├── repository/
│   ├── OrderRepository.java
│   └── SampleRepository.java
├── view/
│   └── ConsoleView.java
└── db/
    └── DatabaseManager.java

src/test/java/ssemi/
├── controller/
│   ├── OrderControllerTest.java
│   └── SampleControllerTest.java
└── repository/
    ├── OrderRepositoryTest.java
    └── SampleRepositoryTest.java
```

---

## 도메인 모델

### OrderStatus 상태 전이

```
PENDING → APPROVED → CONFIRMED → RELEASED
                  ↘ PRODUCING → RELEASED
        ↘ REJECTED
```

- `PENDING`: 주문 접수
- `APPROVED`: 승인 요청됨
- `CONFIRMED`: 재고 충분 → 즉시 확정
- `PRODUCING`: 재고 부족 → 생산 중
- `RELEASED`: 출고 완료
- `REJECTED`: 승인 거부

---

## 코딩 컨벤션

### 네이밍

| 대상 | 규칙 | 예 |
|------|------|----|
| 클래스 | PascalCase | `OrderController` |
| 메서드 | camelCase, 동사 시작 | `findById()`, `saveOrder()` |
| 변수 | camelCase | `orderId`, `sampleStock` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY`, `DB_URL` |
| 패키지 | lowercase | `ssemi.repository` |

### ID 포맷

- 시료: `S001`, `S002`, ...
- 주문: `O001`, `O002`, ...

### SQL

- 모든 SQL은 `PreparedStatement` 사용 (SQL Injection 방지)
- Try-with-resources로 Connection/Statement/ResultSet 반드시 닫기
- 테이블명 대문자: `SAMPLE`, `ORDERS`

### 예외 처리

- Repository 메서드: `SQLException`을 `RuntimeException`으로 래핑하여 상위 전파
- 사용자 입력 오류: View에서 검증, 오류 메시지 출력 후 재입력 유도

---

## 테스트 전략

### 원칙

1. **Repository 테스트**: H2 in-memory DB 사용, 실제 SQL 검증
2. **Controller 테스트**: Repository를 Mockito Mock으로 교체, 비즈니스 로직만 검증
3. **테스트 격리**: 각 테스트 메서드는 독립적 (BeforeEach에서 DB 초기화)

### Repository 테스트 패턴

```java
@BeforeEach
void setUp() {
    // H2 in-memory 연결
    // 스키마 초기화
    // 테스트 픽스처 삽입
}

@AfterEach
void tearDown() {
    // 테이블 DROP 또는 DELETE ALL
}
```

### Controller 테스트 패턴

```java
@Mock
OrderRepository orderRepository;

@InjectMocks
OrderController orderController;

@Test
void 재고_충분_시_CONFIRMED_상태로_변경() { ... }
```

### 커버리지 목표

- Controller: 주요 비즈니스 로직 분기 100%
- Repository: CRUD 메서드 전체

---

## 커밋 메시지 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 준수.

```
<type>(<scope>): <subject>

<body>  (선택)
```

| type | 사용 상황 |
|------|-----------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `test` | 테스트 추가/수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `docs` | 문서 수정 |
| `chore` | 빌드/설정 변경 |

### 예시

```
feat(order): 주문 승인 시 재고 확인 후 상태 분기 처리

재고 >= 주문수량 → CONFIRMED
재고 < 주문수량  → PRODUCING
```

---

## Git 브랜치 전략

```
main
└── feature/<기능명>    (기능 개발)
└── fix/<버그명>        (버그 수정)
└── test/<테스트명>     (테스트 추가)
```

---

## 체크리스트 (PR 전)

- [ ] `./gradlew test` 전체 통과
- [ ] 신규 기능에 대응하는 테스트 작성
- [ ] PreparedStatement 사용 (raw SQL concat 없음)
- [ ] Try-with-resources 적용
- [ ] 커밋 메시지 컨벤션 준수
