package ssemi.view;

import ssemi.model.Order;
import ssemi.model.Production;
import ssemi.model.Sample;

import java.util.List;
import java.util.Map;

public class ProductionView extends BaseView {

    public void renderProductionStatus(List<Production> productions, List<Order> orders, List<Sample> samples) {
        printHeader("메인 > 생산라인 조회 > 생산 현황  [자동 갱신 2.5초]");
        long now = System.currentTimeMillis();
        if (productions.isEmpty()) {
            showInfo("현재 진행 중인 생산이 없습니다.");
            return;
        }
        Map<String, Order>  om = orderMap(orders);
        Map<String, Sample> sm = sampleMap(samples);
        System.out.printf("  ⏳ 생산 중 %d건%n%n", productions.size());

        for (Production p : productions) {
            Order  o = om.get(p.getOrderId());
            Sample s = sm.get(p.getSampleId());
            if (o == null || s == null) continue;

            double elapsedH = elapsedHours(p, now);
            double totalH   = p.getEstimatedHours();
            double ratio    = totalH > 0 ? Math.min(1.0, elapsedH / totalH) : 1.0;
            double remainH  = Math.max(0.0, totalH - elapsedH);
            int    curQty   = (int)(ratio * p.getProductionQty());
            long   endAt    = estimatedEndMillis(p);

            System.out.println(SEP_THIN);
            System.out.printf("  ⏳ %-8s  ->  %-8s%n", p.getProductionId(), o.getOrderId());
            System.out.printf("  시료  %s  %s  (수율 %.2f / 단위 %dh/개)%n",
                    s.getSampleId(), s.getName(), s.getYield(), s.getProductionTime());
            System.out.println(SEP_THIN);
            System.out.printf("  주문 수량: %5d개    부족분: %5d개%n",
                    o.getQuantity(), p.getShortageQty());
            System.out.printf("  실 생산량: %5d개    총 생산시간: %dh%n",
                    p.getProductionQty(), p.getEstimatedHours());
            System.out.printf("  시작:     %s%n", fmtDt(p.getStartedAt()));
            System.out.printf("  완료 예정: %s  (남은 %.1fh)%n", fmtDt(endAt), remainH);
            System.out.printf("  진행률:   [%s]  %3.0f%%%n", progressBar(ratio, 16), ratio * 100);
            System.out.printf("  현재 생산량: %d개 / %d개%n", curQty, p.getProductionQty());
            System.out.println();
        }
    }

    public void renderProductionQueue(List<Production> productions, List<Order> orders, List<Sample> samples) {
        printHeader("메인 > 생산라인 조회 > 대기 목록  [자동 갱신 2.5초]");
        System.out.println("  생산 완료는 예상 시간 경과 시 자동 처리됩니다.  스케줄링: FIFO");
        long now = System.currentTimeMillis();

        if (productions.isEmpty()) {
            System.out.println();
            showInfo("생산 대기 중인 항목이 없습니다.");
            return;
        }
        Map<String, Order>  om = orderMap(orders);
        Map<String, Sample> sm = sampleMap(samples);
        System.out.printf("%n  총 %d건 대기 중%n%n", productions.size());

        for (int i = 0; i < productions.size(); i++) {
            Production p = productions.get(i);
            Order  o     = om.get(p.getOrderId());
            Sample s     = sm.get(p.getSampleId());
            if (o == null || s == null) continue;

            double remainH = Math.max(0.0, p.getEstimatedHours() - elapsedHours(p, now));
            long   endAt   = estimatedEndMillis(p);

            System.out.printf("  [ %d순위 ]%n", i + 1);
            System.out.println(SEP_THIN);
            System.out.printf("  %-8s  ->  %-8s%n", p.getProductionId(), o.getOrderId());
            System.out.printf("  시료  %s  %s  (수율 %.2f / 단위 %dh/개)%n",
                    s.getSampleId(), s.getName(), s.getYield(), s.getProductionTime());
            System.out.printf("  주문 수량: %5d개    부족분: %5d개%n",
                    o.getQuantity(), p.getShortageQty());
            System.out.printf("  실 생산량: %5d개    총 생산시간: %dh%n",
                    p.getProductionQty(), p.getEstimatedHours());
            System.out.printf("  시작:     %s%n", fmtDt(p.getStartedAt()));
            System.out.printf("  완료 예정: %s  (남은 %.1fh)%n", fmtDt(endAt), remainH);
            System.out.println();
        }
    }

    private double elapsedHours(Production p, long now) {
        return (now - p.getStartedAt()) / 3_600_000.0;
    }

    private long estimatedEndMillis(Production p) {
        return p.getStartedAt() + p.getEstimatedHours() * 3_600_000L;
    }
}
