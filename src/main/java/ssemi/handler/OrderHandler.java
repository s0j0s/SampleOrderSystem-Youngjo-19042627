package ssemi.handler;

import lombok.RequiredArgsConstructor;
import ssemi.controller.OrderController;
import ssemi.controller.SampleController;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;
import ssemi.view.ConsoleView;
import ssemi.view.OrderView;
import ssemi.view.SampleView;

import java.util.List;

@RequiredArgsConstructor
public class OrderHandler {

    private final OrderController  orderController;
    private final SampleController sampleController;
    private final ConsoleView      mainView;
    private final OrderView        orderView;
    private final SampleView       sampleView;

    /** [2] 시료 주문 — 신규 주문 생성 */
    public void handleCreate() {
        List<Sample> samples = sampleController.getAllSamples();
        List<Order>  orders  = orderController.getAllOrders();

        sampleView.showSamplesNumbered(samples, orders);
        if (samples.isEmpty()) { mainView.readEnter(); return; }

        int idx = mainView.readListChoice(samples.size());
        if (idx == 0) return;

        Sample selected = samples.get(idx - 1);
        System.out.println();
        System.out.printf("  선택한 시료: %s  %s  (현재 재고 %d개)%n",
                selected.getSampleId(), selected.getName(), selected.getStock());
        System.out.println("  ─────────────────────────────────────");
        int qty = mainView.readInt("  주문 수량> ");

        if (!mainView.readConfirm(String.format("[%s] %s  %d개 주문하시겠습니까?",
                selected.getSampleId(), selected.getName(), qty))) {
            mainView.showInfo("주문이 취소되었습니다.");
            return;
        }
        try {
            Order o = orderController.createOrder(selected.getSampleId(), "", qty);
            mainView.showSuccess("[" + o.getOrderId() + "] 주문 접수 완료  →  상태: 📋 RESERVED");
        } catch (Exception e) {
            mainView.showError(e.getMessage());
        }
        mainView.readEnter();
    }

    /** [3] 주문 승인/거절 — 서브메뉴 루프 */
    public void handleApproval() {
        while (true) {
            mainView.showOrderApprovalSubMenu();
            int choice = mainView.readMenuChoice();
            switch (choice) {
                case 1 -> showAllOrders();
                case 2 -> processOrder();
                case 0 -> { return; }
                default -> mainView.showError("유효하지 않은 선택입니다.");
            }
        }
    }

    private void showAllOrders() {
        List<Order>  all     = orderController.getAllOrders();
        List<Sample> samples = sampleController.getAllSamples();
        orderView.showOrderList(all, samples, "메인 > 주문 승인/거절 > 주문 목록");
        mainView.readEnter();
    }

    private void processOrder() {
        List<Order>  reserved = orderController.getOrdersByStatus(OrderStatus.RESERVED);
        List<Sample> samples  = sampleController.getAllSamples();
        orderView.showOrderListNumbered(reserved, samples, "메인 > 주문 승인/거절 > 주문 처리");
        if (reserved.isEmpty()) { mainView.readEnter(); return; }

        int orderIdx = mainView.readListChoice(reserved.size());
        if (orderIdx == 0) return;

        Order  selectedOrder  = reserved.get(orderIdx - 1);
        Sample selectedSample = sampleController.getSampleById(selectedOrder.getSampleId()).orElse(null);
        if (selectedSample == null) {
            mainView.showError("시료 정보를 찾을 수 없습니다.");
            mainView.readEnter();
            return;
        }

        orderView.showOrderDetail(selectedOrder, selectedSample);
        int action = mainView.readMenuChoice();
        switch (action) {
            case 1 -> approveOrder(selectedOrder.getOrderId());
            case 2 -> rejectOrder(selectedOrder.getOrderId());
            case 0 -> mainView.showInfo("처리를 취소했습니다.");
            default -> mainView.showError("유효하지 않은 선택입니다.");
        }
        mainView.readEnter();
    }

    private void approveOrder(String orderId) {
        try {
            Order result = orderController.approveOrder(orderId);
            if (result.getStatus() == OrderStatus.CONFIRMED) {
                mainView.showSuccess("[" + result.getOrderId() + "] 승인 완료  →  ✅ CONFIRMED");
            } else {
                mainView.showSuccess("[" + result.getOrderId() + "] 승인 완료  →  ⏳ PRODUCING  (생산 대기 등록)");
            }
        } catch (Exception e) {
            mainView.showError(e.getMessage());
        }
    }

    private void rejectOrder(String orderId) {
        if (!mainView.readConfirm("[" + orderId + "] 정말 거절하시겠습니까?")) {
            mainView.showInfo("취소되었습니다.");
            return;
        }
        try {
            Order result = orderController.rejectOrder(orderId);
            mainView.showSuccess("[" + result.getOrderId() + "] 거절 완료  →  ❌ REJECTED");
        } catch (Exception e) {
            mainView.showError(e.getMessage());
        }
    }
}
