package ssemi;

import ssemi.controller.OrderController;
import ssemi.controller.ProductionController;
import ssemi.controller.SampleController;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import ssemi.db.DatabaseManager;
import ssemi.handler.MonitorHandler;
import ssemi.handler.OrderHandler;
import ssemi.handler.ProductionHandler;
import ssemi.handler.ReleaseHandler;
import ssemi.handler.SampleHandler;
import ssemi.repository.OrderRepository;
import ssemi.repository.ProductionRepository;
import ssemi.repository.SampleRepository;
import ssemi.view.ConsoleView;
import ssemi.view.MonitorView;
import ssemi.view.OrderView;
import ssemi.view.ProductionView;
import ssemi.view.ReleaseView;
import ssemi.view.SampleView;

public class Main {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        // ── 인프라 ──────────────────────────────────────────────
        DatabaseManager      db        = new DatabaseManager();
        SampleRepository     sampleRepo = new SampleRepository(db);
        OrderRepository      orderRepo  = new OrderRepository(db);
        ProductionRepository prodRepo   = new ProductionRepository(db);

        // ── 컨트롤러 ────────────────────────────────────────────
        SampleController     sc  = new SampleController(sampleRepo);
        OrderController      oc  = new OrderController(orderRepo, sampleRepo, prodRepo);
        ProductionController pc  = new ProductionController(prodRepo, orderRepo, sampleRepo);

        // ── 뷰 ──────────────────────────────────────────────────
        ConsoleView    mainView   = new ConsoleView();
        SampleView     sampleView = new SampleView();
        OrderView      orderView  = new OrderView();
        MonitorView    monView    = new MonitorView();
        ProductionView prodView   = new ProductionView();
        ReleaseView    relView    = new ReleaseView();

        // ── 핸들러 ──────────────────────────────────────────────
        SampleHandler     sampleHandler = new SampleHandler(sc, oc, mainView, sampleView);
        OrderHandler      orderHandler  = new OrderHandler(oc, sc, mainView, orderView, sampleView);
        MonitorHandler    monHandler    = new MonitorHandler(oc, sc, mainView, monView);
        ProductionHandler prodHandler   = new ProductionHandler(pc, oc, sc, mainView, prodView);
        ReleaseHandler    relHandler    = new ReleaseHandler(oc, sc, mainView, relView);

        // ── 메인 루프 ────────────────────────────────────────────
        boolean running = true;
        while (running) {
            pc.checkAndCompleteExpired();
            mainView.showMainMenu(sc.getAllSamples(), oc.getAllOrders(), pc.getPendingQueue());
            int choice = mainView.readMenuChoice();

            switch (choice) {
                case 1 -> sampleHandler.handle();
                case 2 -> orderHandler.handleCreate();
                case 3 -> orderHandler.handleApproval();
                case 4 -> monHandler.handle();
                case 5 -> prodHandler.handle();
                case 6 -> relHandler.handle();
                case 7 -> { mainView.showInfo("시스템을 종료합니다."); running = false; }
                default -> mainView.showError("유효하지 않은 메뉴입니다. 1~7을 입력하세요.");
            }
        }
    }
}
