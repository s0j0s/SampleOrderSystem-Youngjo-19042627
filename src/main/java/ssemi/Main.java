package ssemi;

import ssemi.controller.OrderController;
import ssemi.controller.ProductionController;
import ssemi.controller.SampleController;
import ssemi.db.DatabaseManager;
import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;
import ssemi.repository.OrderRepository;
import ssemi.repository.ProductionRepository;
import ssemi.repository.SampleRepository;
import ssemi.view.ConsoleView;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        DatabaseManager       db        = new DatabaseManager();
        SampleRepository      sampleRepo = new SampleRepository(db);
        OrderRepository       orderRepo  = new OrderRepository(db);
        ProductionRepository  prodRepo   = new ProductionRepository(db);
        SampleController      sc  = new SampleController(sampleRepo);
        OrderController       oc  = new OrderController(orderRepo, sampleRepo, prodRepo);
        ProductionController  pc  = new ProductionController(prodRepo, orderRepo, sampleRepo);
        ConsoleView           view = new ConsoleView();

        boolean running = true;
        while (running) {
            pc.checkAndCompleteExpired();

            view.showMainMenu(
                    sc.getAllSamples(),
                    oc.getAllOrders(),
                    pc.getPendingQueue()
            );
            int choice = view.readMenuChoice();

            switch (choice) {
                case 1 -> handleSampleMenu(sc, oc, view);
                case 2 -> handleOrderCreate(sc, oc, view);
                case 3 -> handleOrderApproval(oc, sc, view);
                case 4 -> handleMonitor(oc, sc, view);
                case 5 -> handleProductionMenu(pc, oc, sc, view);
                case 6 -> handleRelease(oc, sc, view);
                case 7 -> { view.showInfo("시스템을 종료합니다."); running = false; }
                default -> view.showError("유효하지 않은 메뉴입니다. 1~7을 입력하세요.");
            }
        }
    }

    // ─────────────────────── [1] 시료 관리 ───────────────────────

    private static void handleSampleMenu(SampleController sc, OrderController oc, ConsoleView view) {
        while (true) {
            view.showSampleSubMenu();
            int choice = view.readMenuChoice();
            switch (choice) {
                case 1 -> {
                    view.printHeader("메인 > 시료 관리 > 시료 등록");
                    System.out.println("  새 시료 정보를 입력하세요.");
                    System.out.println("  ─────────────────────────────────────");
                    String name    = view.readLine("  시료명              : ");
                    String spec    = view.readLine("  사양                : ");
                    int    stock   = view.readIntNonNeg("  초기 재고 (0 이상)  : ");
                    double yield   = view.readDouble("  수율 (0.0 초과 1.0) : ");
                    int    prodTime = view.readInt("  단위 생산시간 (h)   : ");
                    try {
                        Sample s = sc.registerSample(name, spec, stock, yield, prodTime);
                        System.out.println("  ─────────────────────────────────────");
                        view.showSuccess("[" + s.getSampleId() + "] " + s.getName() + " 등록 완료");
                    } catch (Exception e) {
                        view.showError(e.getMessage());
                    }
                    view.readEnter();
                }
                case 2 -> {
                    List<Sample> samples = sc.getAllSamples();
                    List<Order>  orders  = oc.getAllOrders();
                    view.showSamples(samples, orders, "메인 > 시료 관리 > 시료 목록");
                    view.readEnter();
                }
                case 3 -> {
                    view.printHeader("메인 > 시료 관리 > 시료 검색");
                    String keyword = view.readLine("  검색어 (시료명)> ");
                    if (keyword.isEmpty()) { view.showError("검색어를 입력하세요."); break; }
                    List<Sample> result = sc.searchByName(keyword);
                    view.showSamples(result, oc.getAllOrders(), "메인 > 시료 관리 > 검색 결과");
                    view.readEnter();
                }
                case 0 -> { return; }
                default -> view.showError("유효하지 않은 선택입니다.");
            }
        }
    }

    // ─────────────────────── [2] 시료 주문 ───────────────────────

    private static void handleOrderCreate(SampleController sc, OrderController oc, ConsoleView view) {
        List<Sample> samples = sc.getAllSamples();
        List<Order>  orders  = oc.getAllOrders();

        view.showSamplesNumbered(samples, orders);
        if (samples.isEmpty()) { view.readEnter(); return; }

        int idx = view.readListChoice(samples.size());
        if (idx == 0) return;

        Sample selected = samples.get(idx - 1);
        System.out.println();
        System.out.printf("  선택한 시료: %s  %s  (현재 재고 %d개)%n",
                selected.getSampleId(), selected.getName(), selected.getStock());
        System.out.println("  ─────────────────────────────────────");
        int qty = view.readInt("  주문 수량> ");

        if (!view.readConfirm(String.format("[%s] %s  %d개 주문하시겠습니까?",
                selected.getSampleId(), selected.getName(), qty))) {
            view.showInfo("주문이 취소되었습니다.");
            return;
        }

        try {
            Order o = oc.createOrder(selected.getSampleId(), "", qty);
            view.showSuccess("[" + o.getOrderId() + "] 주문 접수 완료  →  상태: 📋 RESERVED");
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
        view.readEnter();
    }

    // ─────────────────────── [3] 주문 승인/거절 ───────────────────────

    private static void handleOrderApproval(OrderController oc, SampleController sc, ConsoleView view) {
        while (true) {
            view.showOrderApprovalSubMenu();
            int choice = view.readMenuChoice();
            switch (choice) {
                case 1 -> {
                    List<Order>  all     = oc.getAllOrders();
                    List<Sample> samples = sc.getAllSamples();
                    view.showOrderList(all, samples, "메인 > 주문 승인/거절 > 주문 목록");
                    view.readEnter();
                }
                case 2 -> {
                    List<Order>  reserved = oc.getOrdersByStatus(OrderStatus.RESERVED);
                    List<Sample> samples  = sc.getAllSamples();
                    view.showOrderListNumbered(reserved, samples, "메인 > 주문 승인/거절 > 주문 처리");
                    if (reserved.isEmpty()) { view.readEnter(); break; }

                    int orderIdx = view.readListChoice(reserved.size());
                    if (orderIdx == 0) break;

                    Order selectedOrder = reserved.get(orderIdx - 1);
                    Sample selectedSample = sc.getSampleById(selectedOrder.getSampleId()).orElse(null);
                    if (selectedSample == null) {
                        view.showError("시료 정보를 찾을 수 없습니다.");
                        view.readEnter();
                        break;
                    }

                    view.showOrderDetail(selectedOrder, selectedSample);
                    int action = view.readMenuChoice();
                    switch (action) {
                        case 1 -> {
                            try {
                                Order result = oc.approveOrder(selectedOrder.getOrderId());
                                if (result.getStatus() == OrderStatus.CONFIRMED) {
                                    view.showSuccess("[" + result.getOrderId() + "] 승인 완료  →  ✅ CONFIRMED");
                                } else {
                                    view.showSuccess("[" + result.getOrderId() + "] 승인 완료  →  ⏳ PRODUCING  (생산 대기 등록)");
                                }
                            } catch (Exception e) {
                                view.showError(e.getMessage());
                            }
                        }
                        case 2 -> {
                            if (view.readConfirm("[" + selectedOrder.getOrderId() + "] 정말 거절하시겠습니까?")) {
                                try {
                                    Order result = oc.rejectOrder(selectedOrder.getOrderId());
                                    view.showSuccess("[" + result.getOrderId() + "] 거절 완료  →  ❌ REJECTED");
                                } catch (Exception e) {
                                    view.showError(e.getMessage());
                                }
                            } else {
                                view.showInfo("취소되었습니다.");
                            }
                        }
                        case 0 -> view.showInfo("처리를 취소했습니다.");
                        default -> view.showError("유효하지 않은 선택입니다.");
                    }
                    view.readEnter();
                }
                case 0 -> { return; }
                default -> view.showError("유효하지 않은 선택입니다.");
            }
        }
    }

    // ─────────────────────── [4] 모니터링 ───────────────────────

    private static void handleMonitor(OrderController oc, SampleController sc, ConsoleView view) {
        List<Order>  orders  = oc.getAllOrders();
        List<Sample> samples = sc.getAllSamples();
        view.showMonitor(orders, samples);
        view.readEnter();
    }

    // ─────────────────────── [5] 생산라인 조회 ───────────────────────

    private static void handleProductionMenu(ProductionController pc, OrderController oc,
                                             SampleController sc, ConsoleView view) {
        while (true) {
            view.showProductionSubMenu();
            int choice = view.readMenuChoice();
            switch (choice) {
                case 1 -> view.liveRefreshLoop(() -> {
                    pc.checkAndCompleteExpired();
                    view.renderProductionStatus(
                            pc.getPendingQueue(),
                            oc.getAllOrders(),
                            sc.getAllSamples()
                    );
                });
                case 2 -> view.liveRefreshLoop(() -> {
                    pc.checkAndCompleteExpired();
                    view.renderProductionQueue(
                            pc.getPendingQueue(),
                            oc.getAllOrders(),
                            sc.getAllSamples()
                    );
                });
                case 0 -> { return; }
                default -> view.showError("유효하지 않은 선택입니다.");
            }
        }
    }

    // ─────────────────────── [6] 출고 관리 ───────────────────────

    private static void handleRelease(OrderController oc, SampleController sc, ConsoleView view) {
        List<Order>  confirmed = oc.getOrdersByStatus(OrderStatus.CONFIRMED);
        List<Sample> samples   = sc.getAllSamples();

        view.showReleaseListNumbered(confirmed, samples);
        if (confirmed.isEmpty()) { view.readEnter(); return; }

        int idx = view.readListChoice(confirmed.size());
        if (idx == 0) return;

        Order  selectedOrder  = confirmed.get(idx - 1);
        Sample selectedSample = sc.getSampleById(selectedOrder.getSampleId()).orElse(null);
        if (selectedSample == null) {
            view.showError("시료 정보를 찾을 수 없습니다.");
            view.readEnter();
            return;
        }

        view.showReleaseConfirm(selectedOrder, selectedSample);
        if (!view.readConfirm("[" + selectedOrder.getOrderId() + "] 정말 출고하시겠습니까?")) {
            view.showInfo("출고가 취소되었습니다.");
            view.readEnter();
            return;
        }

        try {
            Order result = oc.releaseOrder(selectedOrder.getOrderId());
            view.showSuccess("[" + result.getOrderId() + "] 출고 완료  →  📦 RELEASE");
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
        view.readEnter();
    }
}
