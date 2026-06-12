package ssemi.handler;

import lombok.RequiredArgsConstructor;
import ssemi.controller.OrderController;
import ssemi.controller.SampleController;
import ssemi.model.Order;
import ssemi.model.Sample;
import ssemi.view.ConsoleView;
import ssemi.view.SampleView;

import java.util.List;

@RequiredArgsConstructor
public class SampleHandler {

    private final SampleController sampleController;
    private final OrderController  orderController;
    private final ConsoleView      mainView;
    private final SampleView       sampleView;

    public void handle() {
        while (true) {
            mainView.showSampleSubMenu();
            int choice = mainView.readMenuChoice();
            switch (choice) {
                case 1 -> registerSample();
                case 2 -> listSamples();
                case 3 -> searchSamples();
                case 0 -> { return; }
                default -> mainView.showError("유효하지 않은 선택입니다.");
            }
        }
    }

    private void registerSample() {
        mainView.printHeader("메인 > 시료 관리 > 시료 등록");
        System.out.println("  새 시료 정보를 입력하세요.");
        System.out.println("  ─────────────────────────────────────");
        String name     = mainView.readLine("  시료명              : ");
        String spec     = mainView.readLine("  사양                : ");
        int    stock    = mainView.readIntNonNeg("  초기 재고 (0 이상)  : ");
        double yield    = mainView.readDouble("  수율 (0.0 초과 1.0) : ");
        int    prodTime = mainView.readInt("  단위 생산시간 (h)   : ");
        try {
            Sample s = sampleController.registerSample(name, spec, stock, yield, prodTime);
            System.out.println("  ─────────────────────────────────────");
            mainView.showSuccess("[" + s.getSampleId() + "] " + s.getName() + " 등록 완료");
        } catch (Exception e) {
            mainView.showError(e.getMessage());
        }
        mainView.readEnter();
    }

    private void listSamples() {
        List<Sample> samples = sampleController.getAllSamples();
        List<Order>  orders  = orderController.getAllOrders();
        sampleView.showSamples(samples, orders, "메인 > 시료 관리 > 시료 목록");
        mainView.readEnter();
    }

    private void searchSamples() {
        mainView.printHeader("메인 > 시료 관리 > 시료 검색");
        String keyword = mainView.readLine("  검색어 (시료명)> ");
        if (keyword.isEmpty()) { mainView.showError("검색어를 입력하세요."); return; }
        List<Sample> result = sampleController.searchByName(keyword);
        sampleView.showSamples(result, orderController.getAllOrders(), "메인 > 시료 관리 > 검색 결과");
        mainView.readEnter();
    }
}
