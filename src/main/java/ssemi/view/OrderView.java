package ssemi.view;

import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;

import java.util.List;
import java.util.Map;

public class OrderView extends BaseView {

    public void showOrderList(List<Order> orders, List<Sample> samples, String path) {
        printHeader(path);
        if (orders.isEmpty()) {
            showInfo("조회 가능한 주문이 없습니다.");
            return;
        }
        Map<String, Sample> sm = sampleMap(samples);
        System.out.println("  ┌──────────────┬──────────────────────────┬───────┐");
        System.out.println("  │   주문 ID    │  시료                    │  수량 │");
        System.out.println("  ├──────────────┼──────────────────────────┼───────┤");
        for (Order o : orders) {
            Sample s = sm.get(o.getSampleId());
            String sName = s != null ? s.getName() + " (" + o.getSampleId() + ")" : o.getSampleId();
            System.out.printf("  │ %s %-9s │ %-24s │  %3d  │%n",
                    statusEmoji(o.getStatus()), o.getOrderId(), sName, o.getQuantity());
        }
        System.out.println("  └──────────────┴──────────────────────────┴───────┘");
        System.out.printf("%n  총 %d건    📋 RESERVED  ✅ CONFIRMED  ⏳ PRODUCING  📦 RELEASE%n",
                orders.size());
    }

    public void showOrderListNumbered(List<Order> orders, List<Sample> samples, String path) {
        printHeader(path);
        if (orders.isEmpty()) {
            showInfo("처리 대기 중인 주문이 없습니다.");
            return;
        }
        Map<String, Sample> sm = sampleMap(samples);
        System.out.println("  ┌────┬──────────────┬──────────────────────────┬───────┐");
        System.out.println("  │  # │   주문 ID    │  시료                    │  수량 │");
        System.out.println("  ├────┼──────────────┼──────────────────────────┼───────┤");
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            Sample s = sm.get(o.getSampleId());
            String sName = s != null ? s.getName() + " (" + o.getSampleId() + ")" : o.getSampleId();
            System.out.printf("  │ %2d │ %s %-9s │ %-24s │  %3d  │%n",
                    i + 1, statusEmoji(o.getStatus()), o.getOrderId(), sName, o.getQuantity());
        }
        System.out.println("  └────┴──────────────┴──────────────────────────┴───────┘");
    }

    public void showOrderDetail(Order order, Sample sample) {
        System.out.println();
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.println("  [ 주문 상세 ]");
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf ("  주문 ID       : %s%n", order.getOrderId());
        System.out.printf ("  시료 ID       : %s  (%s)%n", sample.getSampleId(), sample.getName());
        System.out.printf ("  사양          : %s%n", sample.getSpec());
        System.out.printf ("  현재 재고     : %d개%n", sample.getStock());
        System.out.printf ("  수율          : %.2f%n", sample.getYield());
        System.out.printf ("  단위 생산시간 : %dh/개%n", sample.getProductionTime());
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf ("  주문 수량     : %d개%n", order.getQuantity());
        if (sample.getStock() >= order.getQuantity()) {
            System.out.println(GREEN + "  재고 상태     : ✅ 충분  →  승인 시 즉시 CONFIRMED" + RESET);
        } else {
            int shortage = order.getQuantity() - sample.getStock();
            System.out.println(YELLOW + "  재고 상태     : ⚠️  부족  →  승인 시 PRODUCING (생산 필요)" + RESET);
            System.out.printf (YELLOW + "  부족분        : %d개  (재고 %d개 < 주문 %d개)%n" + RESET,
                    shortage, sample.getStock(), order.getQuantity());
        }
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.println("  처리를 선택하세요.");
        System.out.print("  [1] 승인   [2] 거절   [0] 취소\n  선택> ");
    }
}
