package ssemi.repository;

import lombok.RequiredArgsConstructor;
import ssemi.db.DatabaseManager;
import ssemi.model.Order;
import ssemi.model.OrderStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class OrderRepository {

    private final DatabaseManager dbManager;

    public void save(Order order) {
        String sql = "INSERT INTO ORDERS (ORDER_ID, SAMPLE_ID, CUSTOMER_ID, QUANTITY, STATUS, CREATED_AT) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, order.getOrderId());
            pstmt.setString(2, order.getSampleId());
            pstmt.setString(3, order.getCustomerId());
            pstmt.setInt(4, order.getQuantity());
            pstmt.setString(5, order.getStatus().name());
            pstmt.setLong(6, order.getCreatedAt());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("주문 저장 실패: " + order.getOrderId(), e);
        }
    }

    public List<Order> findAll() {
        String sql = "SELECT * FROM ORDERS ORDER BY ORDER_ID";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("주문 목록 조회 실패", e);
        }
        return orders;
    }

    public Optional<Order> findById(String orderId) {
        String sql = "SELECT * FROM ORDERS WHERE ORDER_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("주문 조회 실패: " + orderId, e);
        }
        return Optional.empty();
    }

    public List<Order> findByStatus(OrderStatus status) {
        String sql = "SELECT * FROM ORDERS WHERE STATUS = ? ORDER BY CREATED_AT ASC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("주문 상태 조회 실패: " + status, e);
        }
        return orders;
    }

    public void updateStatus(String orderId, OrderStatus status) {
        String sql = "UPDATE ORDERS SET STATUS = ? WHERE ORDER_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("주문 상태 수정 실패: " + orderId, e);
        }
    }

    public int nextSequence() {
        String sql = "SELECT COUNT(*) FROM ORDERS";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("주문 시퀀스 조회 실패", e);
        }
        return 1; // rs.next() 실패 시 (테이블 비어 있음)
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        return new Order(
                rs.getString("ORDER_ID"),
                rs.getString("SAMPLE_ID"),
                rs.getString("CUSTOMER_ID"),
                rs.getInt("QUANTITY"),
                OrderStatus.valueOf(rs.getString("STATUS")),
                rs.getLong("CREATED_AT")
        );
    }
}
