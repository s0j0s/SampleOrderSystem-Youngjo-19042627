package ssemi.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssemi.db.DatabaseManager;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Production;
import ssemi.model.Sample;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductionRepositoryTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb_production;DB_CLOSE_DELAY=-1";

    private DatabaseManager dbManager;
    private SampleRepository sampleRepository;
    private OrderRepository orderRepository;
    private ProductionRepository productionRepository;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager(TEST_DB_URL);
        sampleRepository = new SampleRepository(dbManager);
        orderRepository = new OrderRepository(dbManager);
        productionRepository = new ProductionRepository(dbManager);

        sampleRepository.save(new Sample("S-001", "GaN 웨이퍼", "4인치", 5, 0.9, 2));
        orderRepository.save(new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.PRODUCING, 1000L));
        orderRepository.save(new Order("ORD-0002", "S-001", "CUST-002", 20, OrderStatus.PRODUCING, 2000L));
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS PRODUCTION");
            stmt.execute("DROP TABLE IF EXISTS ORDERS");
            stmt.execute("DROP TABLE IF EXISTS SAMPLE");
        }
    }

    @Test
    void 생산레코드_저장_후_조회_성공() {
        Production production = new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, false);
        productionRepository.save(production);

        Optional<Production> found = productionRepository.findById("PRD-0001");
        assertTrue(found.isPresent());
        assertEquals("ORD-0001", found.get().getOrderId());
        assertEquals(13, found.get().getProductionQty());
        assertEquals(26L, found.get().getEstimatedHours());
        assertFalse(found.get().isCompleted());
    }

    @Test
    void 주문ID로_생산레코드_조회() {
        productionRepository.save(new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, false));

        Optional<Production> found = productionRepository.findByOrderId("ORD-0001");
        assertTrue(found.isPresent());
        assertEquals("PRD-0001", found.get().getProductionId());
    }

    @Test
    void 존재하지_않는_생산레코드_조회_시_빈_Optional() {
        Optional<Production> found = productionRepository.findById("PRD-9999");
        assertFalse(found.isPresent());
    }

    @Test
    void 미완료_목록_FIFO_순서_검증() {
        productionRepository.save(new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, false));
        productionRepository.save(new Production("PRD-0002", "ORD-0002", "S-001", 25, 50L, false));

        List<Production> pending = productionRepository.findPendingByFifo();

        assertEquals(2, pending.size());
        assertEquals("PRD-0001", pending.get(0).getProductionId()); // createdAt=1000L 먼저
        assertEquals("PRD-0002", pending.get(1).getProductionId()); // createdAt=2000L 나중
    }

    @Test
    void 완료_처리_후_pending_목록_제외() {
        productionRepository.save(new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, false));
        productionRepository.save(new Production("PRD-0002", "ORD-0002", "S-001", 25, 50L, false));

        productionRepository.complete("PRD-0001");

        List<Production> pending = productionRepository.findPendingByFifo();
        assertEquals(1, pending.size());
        assertEquals("PRD-0002", pending.get(0).getProductionId());
    }

    @Test
    void 시퀀스_번호는_저장된_개수_더하기_1() {
        assertEquals(1, productionRepository.nextSequence());
        productionRepository.save(new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, false));
        assertEquals(2, productionRepository.nextSequence());
    }
}
