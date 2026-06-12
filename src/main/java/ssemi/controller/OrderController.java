package ssemi.controller;

import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;
import ssemi.repository.OrderRepository;
import ssemi.repository.SampleRepository;

import java.util.List;
import java.util.Optional;

public class OrderController {

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public OrderController(OrderRepository orderRepository, SampleRepository sampleRepository) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
    }

    public Order createOrder(String sampleId, String customerId, int quantity) {
        sampleRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료: " + sampleId));

        String orderId = String.format("ORD-%04d", orderRepository.nextSequence());
        Order order = new Order(orderId, sampleId, customerId, quantity,
                OrderStatus.RESERVED, System.currentTimeMillis());
        orderRepository.save(order);
        return order;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * 주문 승인: 재고 >= 수량 → CONFIRMED (재고 차감)
     *           재고 <  수량 → PRODUCING
     */
    public Order approveOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태의 주문만 승인 가능: " + order.getStatus());
        }

        Sample sample = sampleRepository.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료: " + order.getSampleId()));

        if (sample.getStock() >= order.getQuantity()) {
            sampleRepository.updateStock(sample.getSampleId(), sample.getStock() - order.getQuantity());
            orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED);
            order.setStatus(OrderStatus.CONFIRMED);
        } else {
            orderRepository.updateStatus(orderId, OrderStatus.PRODUCING);
            order.setStatus(OrderStatus.PRODUCING);
        }

        return order;
    }

    public Order rejectOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태의 주문만 거부 가능: " + order.getStatus());
        }

        orderRepository.updateStatus(orderId, OrderStatus.REJECTED);
        order.setStatus(OrderStatus.REJECTED);
        return order;
    }

    public Order releaseOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("CONFIRMED 상태의 주문만 출고 가능: " + order.getStatus());
        }

        orderRepository.updateStatus(orderId, OrderStatus.RELEASE);
        order.setStatus(OrderStatus.RELEASE);
        return order;
    }
}
