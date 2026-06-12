package ssemi.view;

import ssemi.model.Order;
import ssemi.model.Sample;

import java.util.List;
import java.util.Map;

public class ReleaseView extends BaseView {

    public void showReleaseListNumbered(List<Order> orders, List<Sample> samples) {
        printHeader("메인 > 출고 관리");
        if (orders.isEmpty()) {
            showInfo("출고 대기 중인 주문이 없습니다.");
            return;
        }
        Map<String, Sample> sm = sampleMap(samples);
        System.out.println("  [ 출고 대기 주문 목록  (CONFIRMED) ]");
        System.out.printf("  %3s  %-9s  %-26s  %5s%n", "번호", "주문 ID", "시료", "수량");
        System.out.println(SEP_THIN);
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            Sample s = sm.get(o.getSampleId());
            String sName = s != null ? s.getName() + " (" + o.getSampleId() + ")" : o.getSampleId();
            System.out.printf("  [%2d]  %s %-9s  %-26s  %5d%n",
                    i + 1, statusEmoji(o.getStatus()), o.getOrderId(), sName, o.getQuantity());
        }
        System.out.println(SEP_THIN);
    }

    public void showReleaseConfirm(Order order, Sample sample) {
        System.out.println();
        System.out.println(SEP_THICK);
        System.out.println("  [ 출고 확인 ]");
        System.out.println(SEP_THIN);
        System.out.printf("  주문 ID       : %s%n", order.getOrderId());
        System.out.printf("  시료 ID       : %s  (%s)%n", sample.getSampleId(), sample.getName());
        System.out.printf("  사양          : %s%n", sample.getSpec());
        System.out.printf("  출고 수량     : %d개%n", order.getQuantity());
        System.out.printf("  현재 재고     : %d개%n", sample.getStock());
        System.out.printf("  출고 후 재고  : %d개%n", sample.getStock() - order.getQuantity());
        System.out.println(SEP_THIN);
    }
}
