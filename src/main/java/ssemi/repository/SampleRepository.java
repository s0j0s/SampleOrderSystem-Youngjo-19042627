package ssemi.repository;

import lombok.RequiredArgsConstructor;
import ssemi.db.DatabaseManager;
import ssemi.model.Sample;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class SampleRepository {

    private final DatabaseManager dbManager;

    public void save(Sample sample) {
        String sql = "INSERT INTO SAMPLE (SAMPLE_ID, NAME, SPEC, STOCK, YIELD, PRODUCTION_TIME) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sample.getSampleId());
            pstmt.setString(2, sample.getName());
            pstmt.setString(3, sample.getSpec());
            pstmt.setInt(4, sample.getStock());
            pstmt.setDouble(5, sample.getYield());
            pstmt.setInt(6, sample.getProductionTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("시료 저장 실패: " + sample.getSampleId(), e);
        }
    }

    public List<Sample> findAll() {
        String sql = "SELECT * FROM SAMPLE ORDER BY SAMPLE_ID";
        List<Sample> samples = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                samples.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("시료 목록 조회 실패", e);
        }
        return samples;
    }

    public Optional<Sample> findById(String sampleId) {
        String sql = "SELECT * FROM SAMPLE WHERE SAMPLE_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sampleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("시료 조회 실패: " + sampleId, e);
        }
        return Optional.empty();
    }

    public void update(Sample sample) {
        String sql = "UPDATE SAMPLE SET NAME = ?, SPEC = ?, STOCK = ?, YIELD = ?, PRODUCTION_TIME = ? WHERE SAMPLE_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sample.getName());
            pstmt.setString(2, sample.getSpec());
            pstmt.setInt(3, sample.getStock());
            pstmt.setDouble(4, sample.getYield());
            pstmt.setInt(5, sample.getProductionTime());
            pstmt.setString(6, sample.getSampleId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("시료 수정 실패: " + sample.getSampleId(), e);
        }
    }

    public void updateStock(String sampleId, int newStock) {
        String sql = "UPDATE SAMPLE SET STOCK = ? WHERE SAMPLE_ID = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newStock);
            pstmt.setString(2, sampleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("재고 수정 실패: " + sampleId, e);
        }
    }

    public int nextSequence() {
        String sql = "SELECT COUNT(*) FROM SAMPLE";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("시료 시퀀스 조회 실패", e);
        }
        return 1; // rs.next() 실패 시 (테이블 비어 있음)
    }

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

    private Sample mapRow(ResultSet rs) throws SQLException {
        return new Sample(
                rs.getString("SAMPLE_ID"),
                rs.getString("NAME"),
                rs.getString("SPEC"),
                rs.getInt("STOCK"),
                rs.getDouble("YIELD"),
                rs.getInt("PRODUCTION_TIME")
        );
    }
}
