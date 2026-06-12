package ssemi.handler;

import lombok.RequiredArgsConstructor;
import ssemi.controller.OrderController;
import ssemi.controller.SampleController;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;
import ssemi.view.ConsoleView;
import ssemi.view.ReleaseView;

import java.util.List;

@RequiredArgsConstructor
public class ReleaseHandler {

    private final OrderController  orderController;
    private final SampleController sampleController;
    private final ConsoleView      mainView;
    private final ReleaseView      releaseView;

    public void handle() {
        Order selected = selectConfirmedOrder();
        if (selected == null) return;
        executeRelease(selected);
        mainView.readEnter();
    }

    /** CONFIRMED 주문 목록을 표시하고 사용자가 선택한 주문을 반환. 취소/빈 목록이면 null. */
    private Order selectConfirmedOrder() {
        List<Order>  confirmed = orderController.getOrdersByStatus(OrderStatus.CONFIRMED);
        List<Sample> samples   = sampleController.getAllSamples();

        releaseView.showReleaseListNumbered(confirmed, samples);
        if (confirmed.isEmpty()) { mainView.readEnter(); return null; }

        int idx = mainView.readListChoice(confirmed.size());
        if (idx == 0) return null;
        return confirmed.get(idx - 1);
    }

    /** 선택된 주문의 출고 확인 화면을 표시하고 출고를 실행. */
    private void executeRelease(Order selectedOrder) {
        Sample selectedSample = sampleController.getSampleById(selectedOrder.getSampleId()).orElse(null);
        if (selectedSample == null) {
            mainView.showError("시료 정보를 찾을 수 없습니다.");
            mainView.readEnter();
            return;
        }

        releaseView.showReleaseConfirm(selectedOrder, selectedSample);
        if (!mainView.readConfirm("[" + selectedOrder.getOrderId() + "] 정말 출고하시겠습니까?")) {
            mainView.showInfo("출고가 취소되었습니다.");
            mainView.readEnter();
            return;
        }

        try {
            Order result = orderController.releaseOrder(selectedOrder.getOrderId());
            mainView.showSuccess("[" + result.getOrderId() + "] 출고 완료  →  📦 RELEASE");
        } catch (Exception e) {
            mainView.showError(e.getMessage());
        }
    }
}
