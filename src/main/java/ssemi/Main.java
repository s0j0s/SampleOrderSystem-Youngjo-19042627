package ssemi;

import ssemi.controller.OrderController;
import ssemi.controller.SampleController;
import ssemi.db.DatabaseManager;
import ssemi.repository.OrderRepository;
import ssemi.repository.SampleRepository;
import ssemi.view.ConsoleView;

public class Main {

    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        SampleRepository sampleRepository = new SampleRepository(dbManager);
        OrderRepository orderRepository = new OrderRepository(dbManager);
        SampleController sampleController = new SampleController(sampleRepository);
        OrderController orderController = new OrderController(orderRepository, sampleRepository);
        ConsoleView view = new ConsoleView();

        boolean running = true;
        while (running) {
            view.showMenu();
            int choice = view.readMenuChoice();

            switch (choice) {
                // TODO: 기능명세 확정 후 메뉴 케이스 추가
                case 0 -> {
                    view.showInfo("시스템을 종료합니다.");
                    running = false;
                }
                default -> view.showError("유효하지 않은 메뉴입니다.");
            }
        }
    }
}
