package ssemi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;
import ssemi.repository.OrderRepository;
import ssemi.repository.SampleRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SampleRepository sampleRepository;

    @InjectMocks
    private OrderController orderController;

    private Sample sampleWithStock;

    @BeforeEach
    void setUp() {
        sampleWithStock = new Sample("S-001", "GaN 웨이퍼", "4인치", 50);
    }

    @Test
    void 주문_생성_성공() {
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sampleWithStock));
        when(orderRepository.nextSequence()).thenReturn(1);

        Order order = orderController.createOrder("S-001", "CUST-001", 10);

        assertEquals("ORD-0001", order.getOrderId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void 존재하지_않는_시료로_주문_시_예외() {
        when(sampleRepository.findById("S-999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderController.createOrder("S-999", "CUST-001", 10));
    }

    @Test
    void 재고_충분_시_승인하면_CONFIRMED() {
        Order pendingOrder = new Order("O001", "S-001", "CUST-001", 10, OrderStatus.PENDING);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(pendingOrder));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(sampleWithStock));

        Order result = orderController.approveOrder("O001");

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(sampleRepository).updateStock(eq("S-001"), eq(40));
        verify(orderRepository).updateStatus(eq("O001"), eq(OrderStatus.CONFIRMED));
    }

    @Test
    void 재고_부족_시_승인하면_PRODUCING() {
        Sample lowStockSample = new Sample("S-001", "GaN 웨이퍼", "4인치", 5);
        Order pendingOrder = new Order("O001", "S-001", "CUST-001", 10, OrderStatus.PENDING);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(pendingOrder));
        when(sampleRepository.findById("S-001")).thenReturn(Optional.of(lowStockSample));

        Order result = orderController.approveOrder("O001");

        assertEquals(OrderStatus.PRODUCING, result.getStatus());
        verify(sampleRepository, never()).updateStock(any(), anyInt());
        verify(orderRepository).updateStatus(eq("O001"), eq(OrderStatus.PRODUCING));
    }

    @Test
    void PENDING_아닌_주문_승인_시_예외() {
        Order confirmedOrder = new Order("O001", "S-001", "CUST-001", 10, OrderStatus.CONFIRMED);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(confirmedOrder));

        assertThrows(IllegalStateException.class,
                () -> orderController.approveOrder("O001"));
    }

    @Test
    void 주문_거부_성공() {
        Order pendingOrder = new Order("O001", "S-001", "CUST-001", 10, OrderStatus.PENDING);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(pendingOrder));

        Order result = orderController.rejectOrder("O001");

        assertEquals(OrderStatus.REJECTED, result.getStatus());
        verify(orderRepository).updateStatus(eq("O001"), eq(OrderStatus.REJECTED));
    }

    @Test
    void CONFIRMED_주문_출고_성공() {
        Order confirmedOrder = new Order("O001", "S-001", "CUST-001", 10, OrderStatus.CONFIRMED);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(confirmedOrder));

        Order result = orderController.releaseOrder("O001");

        assertEquals(OrderStatus.RELEASED, result.getStatus());
        verify(orderRepository).updateStatus(eq("O001"), eq(OrderStatus.RELEASED));
    }

    @Test
    void PRODUCING_주문_출고_성공() {
        Order producingOrder = new Order("O001", "S-001", "CUST-001", 10, OrderStatus.PRODUCING);
        when(orderRepository.findById("O001")).thenReturn(Optional.of(producingOrder));

        Order result = orderController.releaseOrder("O001");

        assertEquals(OrderStatus.RELEASED, result.getStatus());
    }
}
