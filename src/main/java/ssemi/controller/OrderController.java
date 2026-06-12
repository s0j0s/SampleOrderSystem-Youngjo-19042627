package ssemi.controller;

import lombok.RequiredArgsConstructor;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Production;
import ssemi.model.Sample;
import ssemi.repository.OrderRepository;
import ssemi.repository.ProductionRepository;
import ssemi.repository.SampleRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class OrderController {

    private static final double YIELD_SAFETY_MARGIN = 0.9;

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    private final ProductionRepository productionRepository;

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

    public Order approveOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문: " + orderId));
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태의 주문만 승인 가능: " + order.getStatus());
        }
        Sample sample = sampleRepository.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료: " + order.getSampleId()));
        return sample.getStock() >= order.getQuantity()
                ? handleSufficientStock(order, sample)
                : handleInsufficientStock(order, sample);
    }

    private Order handleSufficientStock(Order order, Sample sample) {
        sampleRepository.updateStock(sample.getSampleId(), sample.getStock() - order.getQuantity());
        orderRepository.updateStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.CONFIRMED);
        return order;
    }

    private Order handleInsufficientStock(Order order, Sample sample) {
        int shortageQty = order.getQuantity() - sample.getStock();
        int productionQty = (int) Math.ceil(shortageQty / (sample.getYield() * YIELD_SAFETY_MARGIN));
        long estimatedHours = (long) sample.getProductionTime() * productionQty;
        String productionId = String.format("PRD-%04d", productionRepository.nextSequence());
        productionRepository.save(new Production(
                productionId, order.getOrderId(), sample.getSampleId(),
                productionQty, estimatedHours, false,
                System.currentTimeMillis(), shortageQty));
        orderRepository.updateStatus(order.getOrderId(), OrderStatus.PRODUCING);
        order.setStatus(OrderStatus.PRODUCING);
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
