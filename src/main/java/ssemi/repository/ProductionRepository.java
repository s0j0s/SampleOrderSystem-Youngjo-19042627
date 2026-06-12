package ssemi.repository;

import lombok.RequiredArgsConstructor;
import ssemi.db.DatabaseManager;
import ssemi.model.Production;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProductionRepository {

    private final DatabaseManager dbManager;

    public void save(Production production) {
        String sql = "INSERT INTO PRODUCTION (PRODUCTION_ID, ORDER_ID, SAMPLE_ID, PRODUCTION_QTY, ESTIMATED_HOURS, COMPLETED, STARTED_AT, SHORTAGE_QTY) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, production.getProductionId());
            pstmt.setString(2, production.getOrderId());
            pstmt.setString(3, production.getSampleId());
            pstmt.setInt(4, production.getProductionQty());
            pstmt.setLong(5, production.getEstimatedHours());
            pstmt.setBoolean(6, production.isCompleted());
            pstmt.setLong(7, production.getStartedAt());
            pstmt.setInt(8, production.getShortageQty());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("생산 저장 실패: " + production.getProductionId(), e);
        }
    }

    public Optional<Production> findById(String productionId) {
        String sql = "SELECT * FROM PRODUCTION WHERE PRODUCTION_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("생산 조회 실패: " + productionId, e);
        }
        return Optional.empty();
    }

    public Optional<Production> findByOrderId(String orderId) {
        String sql = "SELECT * FROM PRODUCTION WHERE ORDER_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("주문별 생산 조회 실패: " + orderId, e);
        }
        return Optional.empty();
    }

    public List<Production> findPendingByFifo() {
        String sql = """
                SELECT p.PRODUCTION_ID, p.ORDER_ID, p.SAMPLE_ID,
                       p.PRODUCTION_QTY, p.ESTIMATED_HOURS, p.COMPLETED,
                       p.STARTED_AT, p.SHORTAGE_QTY
                FROM PRODUCTION p
                JOIN ORDERS o ON p.ORDER_ID = o.ORDER_ID
                WHERE p.COMPLETED = FALSE
                ORDER BY o.CREATED_AT ASC
                """;
        List<Production> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("생산 대기 목록 조회 실패", e);
        }
        return list;
    }

    public void complete(String productionId) {
        String sql = "UPDATE PRODUCTION SET COMPLETED = TRUE WHERE PRODUCTION_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("생산 완료 처리 실패: " + productionId, e);
        }
    }

    public int nextSequence() {
        String sql = "SELECT COUNT(*) FROM PRODUCTION";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("생산 시퀀스 조회 실패", e);
        }
        return 1; // rs.next() 실패 시 (테이블 비어 있음)
    }

    private Production mapRow(ResultSet rs) throws SQLException {
        return new Production(
                rs.getString("PRODUCTION_ID"),
                rs.getString("ORDER_ID"),
                rs.getString("SAMPLE_ID"),
                rs.getInt("PRODUCTION_QTY"),
                rs.getLong("ESTIMATED_HOURS"),
                rs.getBoolean("COMPLETED"),
                rs.getLong("STARTED_AT"),
                rs.getInt("SHORTAGE_QTY")
        );
    }
}
