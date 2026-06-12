package ssemi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Production;
import ssemi.model.Sample;
import ssemi.repository.OrderRepository;
import ssemi.repository.ProductionRepository;
import ssemi.repository.SampleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionControllerTest {

    @Mock private ProductionRepository productionRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private SampleRepository sampleRepository;
    @InjectMocks private ProductionController productionController;

    private Production pendingProd;
    private Order producingOrder;
    private Sample sample;

    @BeforeEach
    void setUp() {
        pendingProd   = new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, false, System.currentTimeMillis(), 5);
        producingOrder = new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.PRODUCING, 0L);
        sample        = new Sample("S-001", "GaN", "4인치", 0, 0.9, 2);
    }

    @Test
    void 생산_완료_처리_성공() {
        when(productionRepository.findById("PRD-0001")).thenReturn(Optional.of(pendingProd));
        when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(producingOrder));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

        Production result = productionController.completeProduction("PRD-0001");

        assertTrue(result.isCompleted());
        verify(productionRepository).complete("PRD-0001");
        verify(orderRepository).updateStatus(eq("ORD-0001"), eq(OrderStatus.CONFIRMED));
    }

    @Test
    void 생산_완료_시_잉여재고_입고() {
        // prodQty=13, orderQty=10, stock=0 → surplus=3 → updateStock("S-001", 3)
        when(productionRepository.findById("PRD-0001")).thenReturn(Optional.of(pendingProd));
        when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(producingOrder));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

        productionController.completeProduction("PRD-0001");

        verify(sampleRepository).updateStock(eq("S-001"), eq(3));
    }

    @Test
    void 잉여없을때_updateStock_미호출() {
        // prodQty == orderQty → surplus=0 → updateStock 호출 없음
        Production noSurplusProd = new Production("PRD-0001", "ORD-0001", "S-001", 10, 20L, false, System.currentTimeMillis(), 10);
        when(productionRepository.findById("PRD-0001")).thenReturn(Optional.of(noSurplusProd));
        when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(producingOrder));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

        productionController.completeProduction("PRD-0001");

        verify(sampleRepository, never()).updateStock(any(), anyInt());
    }

    @Test
    void 이미_완료된_생산_완료_시_예외() {
        Production completed = new Production("PRD-0001", "ORD-0001", "S-001", 13, 26L, true, System.currentTimeMillis(), 5);
        when(productionRepository.findById("PRD-0001")).thenReturn(Optional.of(completed));

        assertThrows(IllegalStateException.class,
                () -> productionController.completeProduction("PRD-0001"));
    }

    @Test
    void PRODUCING_아닌_주문_완료_시_예외() {
        Order confirmedOrder = new Order("ORD-0001", "S-001", "CUST-001", 10, OrderStatus.CONFIRMED, 0L);
        when(productionRepository.findById("PRD-0001")).thenReturn(Optional.of(pendingProd));
        when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(confirmedOrder));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

        assertThrows(IllegalStateException.class,
                () -> productionController.completeProduction("PRD-0001"));
    }

    @Test
    void 존재하지_않는_생산ID_완료_시_예외() {
        when(productionRepository.findById("PRD-9999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> productionController.completeProduction("PRD-9999"));
    }

    @Test
    void 미완료_생산_큐_조회() {
        when(productionRepository.findPendingByFifo()).thenReturn(List.of(pendingProd));

        List<Production> result = productionController.getPendingQueue();

        assertEquals(1, result.size());
        verify(productionRepository).findPendingByFifo();
    }

    @Test
    void checkAndCompleteExpired_만료된_생산_자동완료() {
        // startedAt=1L, estimatedHours=1L → 1 + 3_600_000 << now → 만료 조건 충족
        Production expired = new Production("PRD-0001", "ORD-0001", "S-001",
                13, 1L, false, 1L, 5);
        when(productionRepository.findPendingByFifo()).thenReturn(List.of(expired));
        when(productionRepository.findById("PRD-0001")).thenReturn(Optional.of(expired));
        when(orderRepository.findById("ORD-0001")).thenReturn(Optional.of(producingOrder));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

        productionController.checkAndCompleteExpired();

        verify(productionRepository).complete("PRD-0001");
        verify(orderRepository).updateStatus("ORD-0001", OrderStatus.CONFIRMED);
    }

    @Test
    void checkAndCompleteExpired_미만료_생산_완료_미호출() {
        // startedAt=now, estimatedHours=100 → 만료 조건 미충족
        long now = System.currentTimeMillis();
        Production fresh = new Production("PRD-0001", "ORD-0001", "S-001",
                13, 100L, false, now, 5);
        when(productionRepository.findPendingByFifo()).thenReturn(List.of(fresh));

        productionController.checkAndCompleteExpired();

        verify(productionRepository, never()).complete(any());
    }

    @Test
    void checkAndCompleteExpired_예외발생시_다음항목_계속_처리() {
        Production prod1 = new Production("PRD-0001", "ORD-0001", "S-001", 13, 1L, false, 1L, 5);
        Production prod2 = new Production("PRD-0002", "ORD-0002", "S-001",  7, 1L, false, 1L, 3);
        when(productionRepository.findPendingByFifo()).thenReturn(List.of(prod1, prod2));
        // prod1: findById empty → IllegalArgumentException → 무시
        when(productionRepository.findById("PRD-0001")).thenReturn(Optional.empty());
        // prod2: 정상 처리
        Order producingOrder2 = new Order("ORD-0002", "S-001", "CUST-002", 5, OrderStatus.PRODUCING, 0L);
        when(productionRepository.findById("PRD-0002")).thenReturn(Optional.of(prod2));
        when(orderRepository.findById("ORD-0002")).thenReturn(Optional.of(producingOrder2));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sample));

        productionController.checkAndCompleteExpired();

        verify(productionRepository, never()).complete("PRD-0001");
        verify(productionRepository).complete("PRD-0002");
    }
}
