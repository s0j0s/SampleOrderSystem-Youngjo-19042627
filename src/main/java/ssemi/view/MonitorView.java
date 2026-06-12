package ssemi.view;

import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Sample;

import java.util.List;
import java.util.Map;

public class MonitorView extends BaseView {

    public void showMonitor(List<Order> orders, List<Sample> samples) {
        printHeader("메인 > 모니터링");

        long reserved  = orders.stream().filter(o -> o.getStatus() == OrderStatus.RESERVED).count();
        long confirmed = orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long producing = orders.stream().filter(o -> o.getStatus() == OrderStatus.PRODUCING).count();
        long release   = orders.stream().filter(o -> o.getStatus() == OrderStatus.RELEASE).count();
        long maxCount  = Math.max(1, Math.max(Math.max(reserved, confirmed), Math.max(producing, release)));

        System.out.println("  [ 주문 현황 ]  (REJECTED 제외)");
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf ("  📋 RESERVED   %s  %3d건%n", bar((int) reserved,  (int) maxCount, 16), reserved);
        System.out.printf ("  ✅ CONFIRMED  %s  %3d건%n", bar((int) confirmed, (int) maxCount, 16), confirmed);
        System.out.printf ("  ⏳ PRODUCING  %s  %3d건%n", bar((int) producing, (int) maxCount, 16), producing);
        System.out.printf ("  📦 RELEASE    %s  %3d건%n", bar((int) release,   (int) maxCount, 16), release);
        System.out.println("  ─────────────────────────────────────────────────────");

        Map<String, Integer> resMap = reservedQtyMap(orders);
        int maxStock = samples.stream().mapToInt(Sample::getStock).max().orElse(1);
        if (maxStock == 0) maxStock = 1;

        System.out.println();
        System.out.println("  [ 재고 현황 ]");
        System.out.println("  ─────────────────────────────────────────────────────");
        for (Sample s : samples) {
            int resQty = resMap.getOrDefault(s.getSampleId(), 0);
            String bChart = bar(s.getStock(), maxStock, 12);
            String statusLine;
            if (s.getStock() == 0) {
                statusLine = RED    + bChart + "  🔴 고갈" + RESET;
            } else if (s.getStock() < resQty) {
                statusLine = YELLOW + bChart + "  🟡 부족  (RESERVED 주문 " + resQty + "개)" + RESET;
            } else {
                statusLine = GREEN  + bChart + "  🟢 여유" + RESET;
            }
            System.out.printf("  %-7s  %-16s  재고 %4d개  %s%n",
                    s.getSampleId(), s.getName(), s.getStock(), statusLine);
        }
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf("  총 %d종 / 총 재고 %d개%n",
                samples.size(), samples.stream().mapToInt(Sample::getStock).sum());
    }
}
