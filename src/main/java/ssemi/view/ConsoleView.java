package ssemi.view;

import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Production;
import ssemi.model.Sample;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConsoleView extends BaseView {

    private static final String LOGO =
            CYAN +
            "   ▓▓▓▓▓▓▓╗      ▓▓▓▓▓▓▓╗▓▓▓▓▓▓▓╗▓▓▓╗   ▓▓▓╗▓▓╗\n" +
            "   ▓▓╔════╝      ▓▓╔════╝▓▓╔════╝▓▓▓▓╗ ▓▓▓▓║▓▓║\n" +
            "   ▓▓▓▓▓▓▓╗▓▓▓▓▓╗▓▓▓▓▓▓▓╗▓▓▓▓▓╗  ▓▓╔▓▓▓▓╔▓▓║▓▓║\n" +
            "   ╚════▓▓║╚════╝╚════▓▓║▓▓╔══╝  ▓▓║╚▓▓╔╝▓▓║▓▓║\n" +
            "   ▓▓▓▓▓▓▓║      ▓▓▓▓▓▓▓║▓▓▓▓▓▓▓╗▓▓║ ╚═╝ ▓▓║▓▓║\n" +
            "   ╚══════╝      ╚══════╝╚══════╝╚═╝     ╚═╝╚═╝\n" +
            RESET;

    // ─── 메인 메뉴 ────────────────────────────────────────────

    public void showMainMenu(List<Sample> samples, List<Order> orders, List<Production> pendingProds) {
        long validOrders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.REJECTED).count();
        int totalStock = samples.stream().mapToInt(Sample::getStock).sum();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.print(CLEAR);
        System.out.println(LOGO);
        System.out.println(BOLD + "          반도체 시료 생산주문관리 시스템" + RESET);
        System.out.println("  ════════════════════════════════════════════════════");
        System.out.printf("%n  🕐 %s%n%n", now);
        System.out.println("  ┌───────────┬───────────┬───────────┬───────────┐");
        System.out.println("  │ 등록 시료 │ 유효 주문 │  총 재고  │ 생산 진행 │");
        System.out.printf ("  │   %3d 종  │   %3d 건  │  %4d 개  │   %2d 건   │%n",
                samples.size(), validOrders, totalStock, pendingProds.size());
        System.out.println("  └───────────┴───────────┴───────────┴───────────┘");
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────┐");
        System.out.println("  │  [1]  시료 관리      → 시료 등록 / 조회 / 검색  │");
        System.out.println("  │  [2]  시료 주문      → 신규 주문 생성           │");
        System.out.println("  │  [3]  주문 승인/거절  → 주문 목록 / 승인 · 거절 │");
        System.out.println("  │  [4]  모니터링       → 전체 현황 조회            │");
        System.out.println("  │  [5]  생산라인 조회   → 생산 현황 / 대기 목록   │");
        System.out.println("  │  [6]  출고 관리      → CONFIRMED 주문 출고      │");
        System.out.println("  │  [7]  종료                                      │");
        System.out.println("  └─────────────────────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }

    // ─── 서브메뉴 ─────────────────────────────────────────────

    public void showSampleSubMenu() {
        printHeader("메인 > 시료 관리");
        System.out.println("  ┌──────────────────────────────────┐");
        System.out.println("  │  [1]  시료 등록                  │");
        System.out.println("  │  [2]  시료 목록 조회              │");
        System.out.println("  │  [3]  시료 검색                  │");
        System.out.println("  │  [0]  ← 돌아가기                 │");
        System.out.println("  └──────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }

    public void showOrderApprovalSubMenu() {
        printHeader("메인 > 주문 승인/거절");
        System.out.println("  ┌──────────────────────────────────┐");
        System.out.println("  │  [1]  주문 목록 조회              │");
        System.out.println("  │  [2]  주문 처리  (승인 / 거절)   │");
        System.out.println("  │  [0]  ← 돌아가기                 │");
        System.out.println("  └──────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }

    public void showProductionSubMenu() {
        printHeader("메인 > 생산라인 조회");
        System.out.println("  ┌──────────────────────────────────┐");
        System.out.println("  │  [1]  생산 현황  (실시간 갱신)   │");
        System.out.println("  │  [2]  대기 목록  (FIFO 큐)       │");
        System.out.println("  │  [0]  ← 돌아가기                 │");
        System.out.println("  └──────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }
}
