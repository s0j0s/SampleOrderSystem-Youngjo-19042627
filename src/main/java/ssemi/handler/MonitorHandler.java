package ssemi.handler;

import lombok.RequiredArgsConstructor;
import ssemi.controller.OrderController;
import ssemi.controller.SampleController;
import ssemi.view.ConsoleView;
import ssemi.view.MonitorView;

@RequiredArgsConstructor
public class MonitorHandler {

    private final OrderController  orderController;
    private final SampleController sampleController;
    private final ConsoleView      mainView;
    private final MonitorView      monitorView;

    public void handle() {
        monitorView.showMonitor(orderController.getAllOrders(), sampleController.getAllSamples());
        mainView.readEnter();
    }
}
