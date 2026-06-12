package ssemi.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:h2:./data/ssemi;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private final String url;

    public DatabaseManager() {
        this(DB_URL);
    }

    public DatabaseManager(String url) {
        this.url = url;
        initSchema();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, USER, PASSWORD);
    }

    private void initSchema() {
        String createSample = """
                CREATE TABLE IF NOT EXISTS SAMPLE (
                    SAMPLE_ID   VARCHAR(10)  PRIMARY KEY,
                    NAME        VARCHAR(100) NOT NULL,
                    SPEC        VARCHAR(200),
                    STOCK       INT          NOT NULL DEFAULT 0
                )
                """;

        String createOrders = """
                CREATE TABLE IF NOT EXISTS ORDERS (
                    ORDER_ID    VARCHAR(10)  PRIMARY KEY,
                    SAMPLE_ID   VARCHAR(10)  NOT NULL,
                    CUSTOMER_ID VARCHAR(50)  NOT NULL,
                    QUANTITY    INT          NOT NULL,
                    STATUS      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                    FOREIGN KEY (SAMPLE_ID) REFERENCES SAMPLE(SAMPLE_ID)
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSample);
            stmt.execute(createOrders);
        } catch (SQLException e) {
            throw new RuntimeException("DB 스키마 초기화 실패", e);
        }
    }
}
