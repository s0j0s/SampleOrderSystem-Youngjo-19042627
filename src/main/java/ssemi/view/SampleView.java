package ssemi.view;

import ssemi.model.Order;
import ssemi.model.Sample;

import java.util.List;
import java.util.Map;

public class SampleView extends BaseView {

    public void showSamples(List<Sample> samples, List<Order> orders, String path) {
        printHeader(path);
        if (samples.isEmpty()) {
            showInfo("등록된 시료가 없습니다.");
            return;
        }
        Map<String, Integer> resMap = reservedQtyMap(orders);

        System.out.printf("  %-7s  %-16s  %-10s  %4s  %5s  %4s%n",
                "ID", "이름", "사양", "재고", "수율", "시간");
        System.out.println(SEP_THIN);
        for (Sample s : samples) {
            int resQty = resMap.getOrDefault(s.getSampleId(), 0);
            String tag = s.getStock() == 0 ? "🔴" : (s.getStock() < resQty ? "🟡" : "🟢");
            System.out.printf("  %-7s  %-16s  %-10s  %4d  %.2f  %3dh  %s%n",
                    s.getSampleId(), s.getName(), s.getSpec(),
                    s.getStock(), s.getYield(), s.getProductionTime(), tag);
        }
        System.out.println(SEP_THIN);
        System.out.printf("  🟢 여유  🟡 부족  🔴 고갈    총 %d종 / 총 재고 %d개%n",
                samples.size(), samples.stream().mapToInt(Sample::getStock).sum());
    }

    public void showSamplesNumbered(List<Sample> samples, List<Order> orders) {
        printHeader("메인 > 시료 주문");
        if (samples.isEmpty()) {
            showInfo("등록된 시료가 없습니다.");
            return;
        }
        Map<String, Integer> resMap = reservedQtyMap(orders);

        System.out.println("  [ 시료 목록 - 번호를 선택하세요 ]");
        System.out.printf("  %3s  %-7s  %-16s  %-10s  %4s%n",
                "번호", "ID", "이름", "사양", "재고");
        System.out.println(SEP_THIN);
        for (int i = 0; i < samples.size(); i++) {
            Sample s = samples.get(i);
            int resQty = resMap.getOrDefault(s.getSampleId(), 0);
            String tag = s.getStock() == 0 ? "🔴" : (s.getStock() < resQty ? "🟡" : "🟢");
            System.out.printf("  [%2d]  %-7s  %-16s  %-10s  %4d  %s%n",
                    i + 1, s.getSampleId(), s.getName(), s.getSpec(), s.getStock(), tag);
        }
        System.out.println(SEP_THIN);
    }
}
