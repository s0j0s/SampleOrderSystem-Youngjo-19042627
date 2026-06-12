package ssemi.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ssemi.db.DatabaseManager;
import ssemi.model.Sample;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SampleRepositoryTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb_sample;DB_CLOSE_DELAY=-1";

    private DatabaseManager dbManager;
    private SampleRepository repository;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager(TEST_DB_URL);
        repository = new SampleRepository(dbManager);
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
    void 시료_저장_후_조회_성공() {
        Sample sample = new Sample("S-001", "GaN 웨이퍼", "4인치 GaN", 50);
        repository.save(sample);

        Optional<Sample> found = repository.findById("S-001");
        assertTrue(found.isPresent());
        assertEquals("GaN 웨이퍼", found.get().getName());
        assertEquals(50, found.get().getStock());
    }

    @Test
    void 전체_시료_목록_조회() {
        repository.save(new Sample("S-001", "GaN 웨이퍼", "4인치", 30));
        repository.save(new Sample("S-002", "SiC 웨이퍼", "6인치", 20));

        List<Sample> samples = repository.findAll();
        assertEquals(2, samples.size());
    }

    @Test
    void 재고_수정() {
        repository.save(new Sample("S-001", "GaN 웨이퍼", "4인치", 50));
        repository.updateStock("S-001", 30);

        Optional<Sample> found = repository.findById("S-001");
        assertTrue(found.isPresent());
        assertEquals(30, found.get().getStock());
    }

    @Test
    void 존재하지_않는_시료_조회_시_빈_Optional() {
        Optional<Sample> found = repository.findById("S-999");
        assertFalse(found.isPresent());
    }

    @Test
    void 시퀀스_번호는_저장된_개수_더하기_1() {
        assertEquals(1, repository.nextSequence());
        repository.save(new Sample("S-001", "GaN 웨이퍼", "4인치", 50));
        assertEquals(2, repository.nextSequence());
    }
}
