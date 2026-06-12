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

@RequiredArgsConstructor
public class ProductionController {

    private final ProductionRepository productionRepository;
    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public List<Production> getPendingQueue() {
        return productionRepository.findPendingByFifo();
    }

    public Production completeProduction(String productionId) {
        Production production = productionRepository.findById(productionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 생산: " + productionId));

        if (production.isCompleted()) {
            throw new IllegalStateException("이미 완료된 생산: " + productionId);
        }

        Order order = orderRepository.findById(production.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문: " + production.getOrderId()));

        Sample sample = sampleRepository.findById(production.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료: " + production.getSampleId()));

        if (order.getStatus() != OrderStatus.PRODUCING) {
            throw new IllegalStateException("PRODUCING 상태 주문의 생산만 완료 처리 가능: " + order.getStatus());
        }

        productionRepository.complete(productionId);
        production.setCompleted(true);

        int surplus = production.getProductionQty() - order.getQuantity();
        if (surplus > 0) {
            sampleRepository.updateStock(sample.getSampleId(), sample.getStock() + surplus);
        }

        orderRepository.updateStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.CONFIRMED);

        return production;
    }
}
