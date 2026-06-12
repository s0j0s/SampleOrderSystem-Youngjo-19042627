# CLAUDE.md — 반도체 시료 생산주문관리 시스템

## 프로젝트 개요

**시스템명:** 반도체 시료 생산주문관리 시스템 (SampleOrderSystem)  
**목적:** 반도체 시료의 생산 및 주문 흐름을 관리하는 콘솔 기반 MVC 애플리케이션  
**도메인:** 시료(Sample) 재고 관리, 주문(Order) 생성·승인·생산·출고 처리

## POC 참조

| POC | 경로 | 핵심 패턴 |
|-----|------|-----------|
| ConsoleMVC | `../ConsoleMVC-Youngjo-19042627` | MVC 구조, OrderStatus 상태기계 |
| DataPersistence | `../DataPersistence-Youngjo-19042627` | Repository 패턴, JDBC PreparedStatement |
| DataMonitor | `../DataMonitor-Youngjo-19042627` | ANSI 컬러 출력 |
| DummyDataGenerator | `../DummyDataGenerator-Youngjo-19042627` | 템플릿 기반 데이터 생성 |

---

## 기술 스택

| 항목 | 버전/도구 |
|------|-----------|
| Language | Java 17 |
| Build | Gradle 8.7 (application plugin) |
| DB | H2 2.2.224 (file-based, `./data/ssemi`) |
| Test DB | H2 in-memory (`jdbc:h2:mem:testdb`) |
| Unit Test | JUnit 5.10.x |
| Mocking | Mockito 5.x |
| Lombok | 1.18.32 (boilerplate 제거) |
| UI | Console (ANSI color codes) |

### 빌드 및 실행

```bash
# Windows
gradlew.bat run
gradlew.bat test
gradlew.bat build

# macOS / Linux
./gradlew run
./gradlew test
./gradlew build
```

---

## 개발 워크플로우

각 Phase는 아래 순서로 진행하며, **Action 전에 반드시 검토**를 받는다.

```
Explore  →  Plan (design 문서 작성)  →  검토  →  Action (구현)  →  Commit
```

### 단계 설명

| 단계 | 내용 |
|------|------|
| **Explore** | 대상 파일 읽기, 현재 상태 파악, 변경 영향 범위 분석 |
| **Plan** | `docs/design/phase-N.md` 작성 — 설계 결정, 메서드 시그니처, 변경 목록 |
| **검토** | 사람이 design 문서 확인 후 다음 단계 승인 |
| **Action** | 코드 구현 (Plan 문서 기준으로만) |
| **Commit** | Conventional Commits 형식으로 커밋 |

### 설계 문서 위치

```
docs/
├── design/
│   ├── phase-1.md   도메인 모델 설계
│   ├── phase-2.md   DB 스키마 설계
│   ├── phase-3.md   Repository 설계
│   ├── phase-4.md   Controller 설계
│   ├── phase-5.md   View + Main 설계
│   └── phase-6.md   테스트 완성 설계
├── PLAN.md          전체 Phase 구현 계획
├── architecture.md  패키지 및 계층 구조
└── test-scenarios.md 테스트 시나리오
```

---

## 아키텍처

상세 → [`docs/architecture.md`](docs/architecture.md)

### 레이어 요약

```
Main (진입점, 메뉴 루프)
  └── ConsoleView (모든 I/O, 입력 검증)
        ├── SampleController      → SampleRepository
        ├── OrderController       → OrderRepository + SampleRepository
        └── ProductionController  → ProductionRepository + OrderRepository + SampleRepository
```

### 원칙
- **MVC 분리**: View는 I/O만, Controller는 비즈니스 로직만
- **Repository 패턴**: DB 구현 추상화 → 테스트에서 Mock 주입
- **생성자 주입**: 의존성 명시적 관리

---

## 도메인 모델

### OrderStatus 상태 전이

```
RESERVED → (승인, 재고 충분) → CONFIRMED → (출고) → RELEASE
         → (승인, 재고 부족) → PRODUCING → (생산완료) → CONFIRMED
         → (거부)            → REJECTED
```

| 상태 | 의미 |
|------|------|
| `RESERVED` | 주문 접수 (초기 상태) |
| `CONFIRMED` | 주문 확정 (재고 충분 승인) |
| `PRODUCING` | 생산 중 (재고 부족 승인) |
| `RELEASE` | 출고 완료 |
| `REJECTED` | 주문 거부 |

### ID 포맷

| 엔티티 | 포맷 | 예시 |
|--------|------|------|
| Sample | `S-NNN` | `S-001`, `S-012` |
| Order | `ORD-NNNN` | `ORD-0001`, `ORD-0042` |
| Production | `PRD-NNNN` | `PRD-0001` |

---

## 코딩 컨벤션

### 네이밍

| 대상 | 규칙 | 예 |
|------|------|----|
| 클래스 | PascalCase | `OrderController` |
| 메서드 | camelCase, 동사 시작 | `findById()`, `approveOrder()` |
| 변수 | camelCase | `orderId`, `sampleStock` |
| 상수 | UPPER_SNAKE_CASE | `DB_URL` |
| 패키지 | lowercase | `ssemi.repository` |

### Lombok 사용 규칙

모델, Controller, Repository에 적용. View에는 사용하지 않는다.

| 어노테이션 | 적용 대상 |
|-----------|-----------|
| `@Getter` | 모든 모델 클래스 |
| `@Setter` | 가변 필드 개별 적용 (`@Setter` on field) |
| `@AllArgsConstructor` | 모든 모델 클래스 |
| `@ToString` | 모든 모델 클래스 |
| `@RequiredArgsConstructor` | Controller, Repository (final 필드 생성자 대체) |

### SQL 규칙

- 모든 SQL: `PreparedStatement` (SQL Injection 방지)
- 리소스 정리: Try-with-resources
- 테이블명: 대문자 (`SAMPLE`, `ORDERS`, `PRODUCTION`)

### 예외 처리

- Repository: `SQLException` → `RuntimeException` 래핑
- 입력 오류: View에서 검증, 오류 메시지 후 재입력 유도

---

## 테스트 전략

상세 시나리오 → [`docs/test-scenarios.md`](docs/test-scenarios.md)

| 레이어 | 도구 | DB |
|--------|------|----|
| Repository | JUnit 5 | H2 in-memory |
| Controller | JUnit 5 + Mockito | Mock Repository |

```java
// Repository 테스트
@BeforeEach void setUp()    { dbManager = new DatabaseManager("jdbc:h2:mem:..."); }
@AfterEach  void tearDown() { stmt.execute("DROP TABLE IF EXISTS ..."); }

// Controller 테스트
@Mock SampleRepository sampleRepository;
@InjectMocks SampleController sampleController;
```

---

## 커밋 메시지 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 준수.

```
<type>(<scope>): <subject>
```

| type | 상황 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `test` | 테스트 추가/수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `docs` | 문서 수정 |
| `chore` | 빌드/설정 변경 |

---

## Git 브랜치 전략

```
main
├── feature/phase-N-<설명>   각 Phase 구현
├── fix/<버그명>
└── docs/<문서명>
```

---

## PR 체크리스트

- [ ] `./gradlew test` 전체 통과
- [ ] design 문서와 구현 일치
- [ ] PreparedStatement 사용 확인
- [ ] Try-with-resources 적용 확인
- [ ] 커밋 메시지 컨벤션 준수
