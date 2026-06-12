package ssemi.handler;

import lombok.RequiredArgsConstructor;
import ssemi.controller.OrderController;
import ssemi.controller.ProductionController;
import ssemi.controller.SampleController;
import ssemi.view.ConsoleView;
import ssemi.view.ProductionView;

@RequiredArgsConstructor
public class ProductionHandler {

    private final ProductionController productionController;
    private final OrderController      orderController;
    private final SampleController     sampleController;
    private final ConsoleView          mainView;
    private final ProductionView       productionView;

    public void handle() {
        while (true) {
            mainView.showProductionSubMenu();
            int choice = mainView.readMenuChoice();
            switch (choice) {
                case 1 -> mainView.liveRefreshLoop(() -> {
                    productionController.checkAndCompleteExpired();
                    productionView.renderProductionStatus(
                            productionController.getPendingQueue(),
                            orderController.getAllOrders(),
                            sampleController.getAllSamples()
                    );
                });
                case 2 -> mainView.liveRefreshLoop(() -> {
                    productionController.checkAndCompleteExpired();
                    productionView.renderProductionQueue(
                            productionController.getPendingQueue(),
                            orderController.getAllOrders(),
                            sampleController.getAllSamples()
                    );
                });
                case 0 -> { return; }
                default -> mainView.showError("유효하지 않은 선택입니다.");
            }
        }
    }
}
