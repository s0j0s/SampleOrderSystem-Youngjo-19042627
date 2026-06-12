# 반도체 시료 생산주문관리 시스템

반도체 회사 S-Semi의 시료 재고 및 생산 주문을 관리하는 콘솔 기반 애플리케이션.

## 요구 환경

| 항목 | 버전 |
|------|------|
| Java | 17 이상 |
| Gradle | 8.7 (wrapper 포함) |
| OS | Windows / macOS / Linux |

## 실행 방법

### 터미널 (Windows)

```bat
run.bat
```

> `gradlew.bat run`은 한글·특수문자가 깨질 수 있음.  
> `run.bat`은 `chcp 65001` + `GRADLE_OPTS` 인코딩 설정을 자동으로 처리함.

### 터미널 (macOS / Linux)

```bash
./gradlew run
```

### IntelliJ IDEA

1. 프로젝트 열기: `File → Open` → 프로젝트 루트 선택
2. Gradle 연동 확인: `Build, Execution, Deployment → Gradle → Build and run using: Gradle`
3. `Main.java` 우클릭 → `Run 'Main.main()'`

> IntelliJ에서 한글 깨짐 발생 시: `File → Settings → Editor → File Encodings` 모두 `UTF-8` 설정

## 테스트 실행

```bat
test.bat                  # 전체 테스트 (한글 테스트명 정상 출력)
test.bat --info           # 상세 출력
```

## 빌드

```bat
gradlew.bat build         # 컴파일 + 테스트 + JAR 생성
```

## 프로젝트 구조

```
SampleOrderSystem-Youngjo-19042627/
├── CLAUDE.md               개발 지침 및 컨벤션
├── PRD.md                  기능 요구사항 명세
├── README.md               실행 방법 (이 파일)
├── docs/
│   ├── architecture.md     패키지 및 계층 구조
│   └── test-scenarios.md   테스트 시나리오
├── build.gradle
├── gradle.properties       UTF-8 인코딩 설정
├── src/
│   ├── main/java/ssemi/
│   │   ├── Main.java
│   │   ├── model/          Order, Sample, Production, OrderStatus
│   │   ├── controller/     OrderController, SampleController, ProductionController
│   │   ├── repository/     OrderRepository, SampleRepository, ProductionRepository
│   │   ├── view/           ConsoleView
│   │   └── db/             DatabaseManager
│   └── test/java/ssemi/
│       ├── controller/
│       └── repository/
└── data/                   H2 DB 파일 (런타임 생성, git 제외)
```

## 주요 기능

| 메뉴 | 기능 |
|------|------|
| 1. 시료 관리 | 등록 / 조회 / 검색 |
| 2. 주문 접수 | 시료 주문 접수 (RESERVED) |
| 3. 주문 승인/거부 | 재고 확인 → CONFIRMED 또는 PRODUCING / REJECTED |
| 4. 생산 관리 | FIFO 생산 큐 조회 및 생산 완료 처리 |
| 5. 출고 처리 | CONFIRMED 주문 → RELEASE |
| 6. 모니터링 | 주문 현황 + 시료 재고 컬러 표시 |

## 상태 흐름

```
RESERVED → (승인, 재고 충분) → CONFIRMED → RELEASE
         → (승인, 재고 부족) → PRODUCING → CONFIRMED → RELEASE
         → (거부)            → REJECTED
```

## 참고 문서

- [기능 요구사항](PRD.md)
- [개발 지침](CLAUDE.md)
- [아키텍처](docs/architecture.md)
- [테스트 시나리오](docs/test-scenarios.md)
