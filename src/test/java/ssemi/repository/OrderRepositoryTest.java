package ssemi.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssemi.db.DatabaseManager;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OrderRepositoryTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb_order;DB_CLOSE_DELAY=-1";

    private DatabaseManager dbManager;
    private SampleRepository sampleRepository;
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager(TEST_DB_URL);
        sampleRepository = new SampleRepository(dbManager);
        orderRepository = new OrderRepository(dbManager);
        sampleRepository.save(new Sample("S001", "GaN 웨이퍼", "4인치", 50));
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS ORDERS");
            stmt.execute("DROP TABLE IF EXISTS SAMPLE");
        }
    }

    @Test
    void 주문_저장_후_조회_성공() {
        Order order = new Order("O001", "S001", "CUST-001", 10, OrderStatus.PENDING);
        orderRepository.save(order);

        Optional<Order> found = orderRepository.findById("O001");
        assertTrue(found.isPresent());
        assertEquals("S001", found.get().getSampleId());
        assertEquals(OrderStatus.PENDING, found.get().getStatus());
    }

    @Test
    void 전체_주문_목록_조회() {
        orderRepository.save(new Order("O001", "S001", "CUST-001", 10, OrderStatus.PENDING));
        orderRepository.save(new Order("O002", "S001", "CUST-002", 5, OrderStatus.PENDING));

        List<Order> orders = orderRepository.findAll();
        assertEquals(2, orders.size());
    }

    @Test
    void 상태별_주문_조회() {
        orderRepository.save(new Order("O001", "S001", "CUST-001", 10, OrderStatus.PENDING));
        orderRepository.save(new Order("O002", "S001", "CUST-002", 5, OrderStatus.CONFIRMED));

        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
        assertEquals(1, pending.size());
        assertEquals("O001", pending.get(0).getOrderId());
    }

    @Test
    void 주문_상태_수정() {
        orderRepository.save(new Order("O001", "S001", "CUST-001", 10, OrderStatus.PENDING));
        orderRepository.updateStatus("O001", OrderStatus.CONFIRMED);

        Optional<Order> found = orderRepository.findById("O001");
        assertTrue(found.isPresent());
        assertEquals(OrderStatus.CONFIRMED, found.get().getStatus());
    }
}
