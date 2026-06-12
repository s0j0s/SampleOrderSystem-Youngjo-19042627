package ssemi.view;

import ssemi.model.Order;
import ssemi.model.Sample;

import java.util.List;
import java.util.Scanner;

public class ConsoleView {

    private static final String RESET  = "[0m";
    private static final String BOLD   = "[1m";
    private static final String CYAN   = "[36m";
    private static final String GREEN  = "[32m";
    private static final String YELLOW = "[33m";
    private static final String RED    = "[31m";

    private final Scanner scanner = new Scanner(System.in);

    public void showMenu() {
        System.out.println();
        System.out.println(BOLD + CYAN + "===== 반도체 시료 생산주문관리 시스템 =====" + RESET);
        // TODO: 기능명세 확정 후 메뉴 항목 추가
        System.out.println("0. 종료");
        System.out.print("선택> ");
    }

    public int readMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value > 0) return value;
                showError("양수를 입력하세요.");
            } catch (NumberFormatException e) {
                showError("숫자를 입력하세요.");
            }
        }
    }

    public void showSamples(List<Sample> samples) {
        System.out.println();
        System.out.println(BOLD + "--- 시료 목록 ---" + RESET);
        if (samples.isEmpty()) {
            System.out.println("  등록된 시료가 없습니다.");
            return;
        }
        samples.forEach(s -> {
            String stockColor = s.getStock() >= 15 ? GREEN : YELLOW;
            System.out.printf("  [%s] %s (%s) 재고: %s%d%s%n",
                    s.getSampleId(), s.getName(), s.getSpec(),
                    stockColor, s.getStock(), RESET);
        });
    }

    public void showOrders(List<Order> orders) {
        System.out.println();
        System.out.println(BOLD + "--- 주문 목록 ---" + RESET);
        if (orders.isEmpty()) {
            System.out.println("  등록된 주문이 없습니다.");
            return;
        }
        orders.forEach(o -> System.out.printf("  %s%n", o));
    }

    public void showSuccess(String message) {
        System.out.println(GREEN + "[완료] " + message + RESET);
    }

    public void showError(String message) {
        System.out.println(RED + "[오류] " + message + RESET);
    }

    public void showInfo(String message) {
        System.out.println(CYAN + "[정보] " + message + RESET);
    }
}
