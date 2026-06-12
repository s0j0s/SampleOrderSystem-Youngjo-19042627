# Phase 3 설계 — Repository 완성

**목표:** 모든 DB 접근 로직 구현 완성 + Repository 테스트 전체 통과  
**상태:** 설계 완료 / 구현 대기

---

## Explore 결과

### SampleRepository 현재 메서드

| 메서드 | 상태 |
|--------|------|
| `save(Sample)` | ✅ 완료 (yield/productionTime 포함) |
| `findAll()` | ✅ 완료 |
| `findById(String)` | ✅ 완료 |
| `update(Sample)` | ✅ 완료 |
| `updateStock(String, int)` | ✅ 완료 |
| `nextSequence()` | ✅ 완료 |
| `searchByName(String)` | ❌ 미구현 |

### OrderRepository 현재 메서드

| 메서드 | 상태 |
|--------|------|
| `save(Order)` | ✅ 완료 (createdAt 포함) |
| `findAll()` | ✅ 완료 |
| `findById(String)` | ✅ 완료 |
| `findByStatus(OrderStatus)` | ✅ 완료 (`ORDER BY CREATED_AT ASC` 적용됨) |
| `updateStatus(String, OrderStatus)` | ✅ 완료 |
| `nextSequence()` | ✅ 완료 |

### ProductionRepository

❌ 파일 없음 — 전체 신규 구현 필요

---

## 설계 결정

### D-1. SampleRepository.searchByName — LIKE 방식
**결정:** `WHERE NAME LIKE ?`, 파라미터 = `'%' + keyword + '%'`  
**이유:** 부분 일치 검색. PreparedStatement로 SQL Injection 방지 유지.

### D-2. OrderRepository.findByStatus — 이미 FIFO 정렬 포함
**결정:** 기존 `findByStatus()`가 `ORDER BY CREATED_AT ASC` 포함 → 별도 메서드 추가 불필요  
**이유:** PLAN.md의 `findByStatusOrderByCreatedAt` 분리 계획은 불필요. 현재 구현으로 충분.

### D-3. ProductionRepository.findPendingByFifo — JOIN 방식
**결정:** PRODUCTION과 ORDERS를 JOIN해 `ORDERS.CREATED_AT` 기준 정렬  
**이유:** PRODUCTION 테이블에 createdAt 없음. 주문 접수 시간 기준 FIFO가 PRD 요구사항.

```sql
SELECT p.* FROM PRODUCTION p
JOIN ORDERS o ON p.ORDER_ID = o.ORDER_ID
WHERE p.COMPLETED = FALSE
ORDER BY o.CREATED_AT ASC
```

### D-4. ProductionRepository.complete — 단순 boolean UPDATE
**결정:** `UPDATE PRODUCTION SET COMPLETED = TRUE WHERE PRODUCTION_ID = ?`  
**이유:** 완료 처리는 단방향(false→true). 재고 증가·주문 상태 변경은 Controller 책임.

### D-5. nextSequence — COUNT(*)+1 방식 통일
**결정:** SampleRepository / OrderRepository와 동일한 패턴 유지  
**이유:** 일관성. 동시성 이슈 없는 콘솔 단일 사용자 환경.

---

## 구현 명세

### 3-1. `SampleRepository.java` — `searchByName` 추가

```java
public List<Sample> searchByName(String keyword) {
    String sql = "SELECT * FROM SAMPLE WHERE NAME LIKE ? ORDER BY SAMPLE_ID";
    List<Sample> samples = new ArrayList<>();
    try (Connection conn = dbManager.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, "%" + keyword + "%");
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                samples.add(mapRow(rs));
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("시료 검색 실패: " + keyword, e);
    }
    return samples;
}
```

### 3-2. `ProductionRepository.java` — 신규

```java
package ssemi.repository;

public class ProductionRepository {

    // 생성자
    public ProductionRepository(DatabaseManager dbManager)

    // 저장
    public void save(Production production)

    // 단건 조회
    public Optional<Production> findById(String productionId)

    // 주문 ID로 조회
    public Optional<Production> findByOrderId(String orderId)

    // 미완료 목록 — 주문 접수시간 FIFO 정렬
    public List<Production> findPendingByFifo()

    // 완료 처리
    public void complete(String productionId)

    // 시퀀스
    public int nextSequence()
}
```

**save SQL:**
```sql
INSERT INTO PRODUCTION
  (PRODUCTION_ID, ORDER_ID, SAMPLE_ID, PRODUCTION_QTY, ESTIMATED_HOURS, COMPLETED)
VALUES (?, ?, ?, ?, ?, ?)
```

**findPendingByFifo SQL:**
```sql
SELECT p.PRODUCTION_ID, p.ORDER_ID, p.SAMPLE_ID,
       p.PRODUCTION_QTY, p.ESTIMATED_HOURS, p.COMPLETED
FROM PRODUCTION p
JOIN ORDERS o ON p.ORDER_ID = o.ORDER_ID
WHERE p.COMPLETED = FALSE
ORDER BY o.CREATED_AT ASC
```

**complete SQL:**
```sql
UPDATE PRODUCTION SET COMPLETED = TRUE WHERE PRODUCTION_ID = ?
```

### 3-3. `ProductionRepositoryTest.java` — 신규

테스트 DB URL: `jdbc:h2:mem:testdb_production;DB_CLOSE_DELAY=-1`

| 테스트 메서드 | 검증 내용 |
|------------|---------|
| `생산레코드_저장_후_조회_성공` | save → findById, 전 필드 일치 |
| `주문ID로_생산레코드_조회` | findByOrderId, Optional 존재 |
| `존재하지_않는_생산레코드_조회_시_빈_Optional` | findById 없을 때 empty |
| `미완료_목록_FIFO_순서_검증` | createdAt 빠른 순서가 먼저 반환 |
| `완료_처리_후_pending_목록_제외` | complete → findPendingByFifo에서 제외됨 |
| `시퀀스_번호는_저장된_개수_더하기_1` | nextSequence 정확성 |

tearDown 순서: `PRODUCTION → ORDERS → SAMPLE`

---

## 영향 범위

| 파일 | 변경 내용 |
|------|-----------|
| `repository/SampleRepository.java` | `searchByName` 메서드 추가 |
| `repository/ProductionRepository.java` | 신규 생성 |
| `test/repository/SampleRepositoryTest.java` | `searchByName` 테스트 1개 추가 |
| `test/repository/ProductionRepositoryTest.java` | 신규 생성 (테스트 6개) |

OrderRepository — 변경 없음 (FIFO 정렬 이미 적용됨)

---

## 검증 기준

- `./gradlew test` 기존 20개 + 신규 7개 = 27개 이상 통과
- `ProductionRepository` 파일 존재
- `searchByName("GaN")` 호출 시 이름에 "GaN" 포함된 시료만 반환
- `findPendingByFifo()` 반환 순서가 `createdAt` ASC 기준

---

## 검토 체크리스트

> Action 진행 전 아래 항목을 확인하고 승인해주세요.

### SampleRepository
- [x] `searchByName`을 `NAME LIKE '%keyword%'` 부분 일치로 구현하는 방향이 맞는가?
- [x] `findAll()`과 별도로 검색 메서드를 분리하는 것에 동의하는가?

### OrderRepository
- [x] 기존 `findByStatus()`가 `ORDER BY CREATED_AT ASC` 포함 → 별도 FIFO 메서드 추가 불필요 — 동의하는가?

### ProductionRepository
- [x] `findPendingByFifo()`가 PRODUCTION+ORDERS JOIN으로 `ORDERS.CREATED_AT` 기준 정렬하는 방식이 맞는가?
- [x] `complete()`는 DB만 업데이트, 재고·주문상태 변경은 Controller 책임 — 역할 분리에 동의하는가?
- [x] `findByOrderId()`가 필요한가? (Phase 4 Controller에서 중복 생산 방지 체크에 사용)

### 테스트
- [x] `ProductionRepositoryTest` DB URL을 `testdb_production`으로 분리 — 기존 테스트와 격리됨을 확인하는가?
- [x] FIFO 순서 검증 테스트: `createdAt`이 다른 주문 2개 저장 후 순서 확인 — 검증 방식이 충분한가?
