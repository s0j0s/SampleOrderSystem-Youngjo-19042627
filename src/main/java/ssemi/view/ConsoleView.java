package ssemi.view;

import ssemi.model.Order;
import ssemi.model.OrderStatus;
import ssemi.model.Production;
import ssemi.model.Sample;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ConsoleView {

    private static final String RESET  = "\033[0m";
    private static final String BOLD   = "\033[1m";
    private static final String CYAN   = "\033[36m";
    private static final String GREEN  = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String RED    = "\033[31m";
    private static final String CLEAR  = "\033[2J\033[H";

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String LOGO =
            CYAN +
            "   ███████╗      ███████╗███████╗███╗   ███╗██╗\n" +
            "   ██╔════╝      ██╔════╝██╔════╝████╗ ████║██║\n" +
            "   ███████╗█████╗███████╗█████╗  ██╔████╔██║██║\n" +
            "   ╚════██║╚════╝╚════██║██╔══╝  ██║╚██╔╝██║██║\n" +
            "   ███████║      ███████║███████╗██║ ╚═╝ ██║██║\n" +
            "   ╚══════╝      ╚══════╝╚══════╝╚═╝     ╚═╝╚═╝\n" +
            RESET;

    private final Scanner scanner = new Scanner(System.in);

    // ─────────────────────── 공통 헬퍼 ───────────────────────

    public void printHeader(String path) {
        System.out.println();
        System.out.println(CYAN + "  ╔══════════════════════════════════════════════════════╗");
        System.out.println(       "  ║  🔬 S-SEMI  │  " + path);
        System.out.println(       "  ╚══════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private String statusEmoji(OrderStatus status) {
        return switch (status) {
            case RESERVED  -> "📋";
            case CONFIRMED -> "✅";
            case PRODUCING -> "⏳";
            case RELEASE   -> "📦";
            case REJECTED  -> "❌";
        };
    }

    private String bar(int value, int max, int width) {
        if (max == 0) return "░".repeat(width);
        int filled = (int) Math.round((double) value / max * width);
        filled = Math.min(filled, width);
        return "█".repeat(filled) + "░".repeat(width - filled);
    }

    private String progressBar(double ratio, int width) {
        int filled = (int) Math.min(Math.round(ratio * width), width);
        return "█".repeat(filled) + "░".repeat(width - filled);
    }

    private String fmtDt(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis),
                ZoneId.systemDefault()).format(DT_FMT);
    }

    private Map<String, Sample> sampleMap(List<Sample> samples) {
        return samples.stream().collect(Collectors.toMap(Sample::getSampleId, s -> s));
    }

    private Map<String, Order> orderMap(List<Order> orders) {
        return orders.stream().collect(Collectors.toMap(Order::getOrderId, o -> o));
    }

    private Map<String, Integer> reservedQtyMap(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.RESERVED)
                .collect(Collectors.groupingBy(Order::getSampleId,
                        Collectors.summingInt(Order::getQuantity)));
    }

    // ─────────────────────── 입력 메서드 ───────────────────────

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
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v > 0) return v;
                showError("양수를 입력하세요.");
            } catch (NumberFormatException e) {
                showError("숫자를 입력하세요.");
            }
        }
    }

    public int readIntNonNeg(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= 0) return v;
                showError("0 이상의 값을 입력하세요.");
            } catch (NumberFormatException e) {
                showError("숫자를 입력하세요.");
            }
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(scanner.nextLine().trim());
                if (v > 0.0 && v <= 1.0) return v;
                showError("0.0 초과 1.0 이하의 값을 입력하세요.");
            } catch (NumberFormatException e) {
                showError("숫자를 입력하세요.");
            }
        }
    }

    public int readListChoice(int max) {
        while (true) {
            System.out.print("  번호 선택 (0: 돌아가기)> ");
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= 0 && v <= max) return v;
                showError("0 ~ " + max + " 사이의 번호를 입력하세요.");
            } catch (NumberFormatException e) {
                showError("숫자를 입력하세요.");
            }
        }
    }

    public boolean readConfirm(String prompt) {
        while (true) {
            System.out.print("  " + prompt + " (y/n)> ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y")) return true;
            if (input.equals("n")) return false;
            showError("y 또는 n을 입력하세요.");
        }
    }

    public void readEnter() {
        System.out.print("\n  [Enter] 돌아가기...");
        scanner.nextLine();
    }

    // ─────────────────────── 출력 메서드 ───────────────────────

    public void showSuccess(String message) {
        System.out.println(GREEN + "  ✔ " + message + RESET);
    }

    public void showError(String message) {
        System.out.println(RED + "  ✘ " + message + RESET);
    }

    public void showInfo(String message) {
        System.out.println(CYAN + "  ℹ " + message + RESET);
    }

    // ─────────────────────── 메인 메뉴 ───────────────────────

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
        System.out.println("  │ 등록 시료  │ 유효 주문  │  총 재고   │ 생산 진행  │");
        System.out.printf ("  │   %3d 종  │   %3d 건  │  %4d 개  │   %2d 건   │%n",
                samples.size(), validOrders, totalStock, pendingProds.size());
        System.out.println("  └───────────┴───────────┴───────────┴───────────┘");
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────────┐");
        System.out.println("  │  [1]  시료 관리      → 시료 등록 / 조회 / 검색   │");
        System.out.println("  │  [2]  시료 주문      → 신규 주문 생성            │");
        System.out.println("  │  [3]  주문 승인/거절  → 주문 목록 / 승인 · 거절   │");
        System.out.println("  │  [4]  모니터링       → 전체 현황 조회            │");
        System.out.println("  │  [5]  생산라인 조회   → 생산 현황 / 대기 목록     │");
        System.out.println("  │  [6]  출고 관리      → CONFIRMED 주문 출고       │");
        System.out.println("  │  [7]  종료                                      │");
        System.out.println("  └─────────────────────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }

    // ─────────────────────── 서브메뉴 ───────────────────────

    public void showSampleSubMenu() {
        printHeader("메인 > 시료 관리");
        System.out.println("  ┌──────────────────────────────────┐");
        System.out.println("  │  [1]  시료 등록                   │");
        System.out.println("  │  [2]  시료 목록 조회              │");
        System.out.println("  │  [3]  시료 검색                   │");
        System.out.println("  │  [0]  ← 돌아가기                  │");
        System.out.println("  └──────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }

    public void showOrderApprovalSubMenu() {
        printHeader("메인 > 주문 승인/거절");
        System.out.println("  ┌──────────────────────────────────┐");
        System.out.println("  │  [1]  주문 목록 조회              │");
        System.out.println("  │  [2]  주문 처리  (승인 / 거절)    │");
        System.out.println("  │  [0]  ← 돌아가기                  │");
        System.out.println("  └──────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }

    public void showProductionSubMenu() {
        printHeader("메인 > 생산라인 조회");
        System.out.println("  ┌──────────────────────────────────┐");
        System.out.println("  │  [1]  생산 현황  (실시간 갱신)    │");
        System.out.println("  │  [2]  대기 목록  (FIFO 큐)        │");
        System.out.println("  │  [0]  ← 돌아가기                  │");
        System.out.println("  └──────────────────────────────────┘");
        System.out.print("\n  선택> ");
    }

    // ─────────────────────── 시료 화면 ───────────────────────

    public void showSamples(List<Sample> samples, List<Order> orders, String path) {
        printHeader(path);
        if (samples.isEmpty()) {
            showInfo("등록된 시료가 없습니다.");
            return;
        }
        Map<String, Integer> resMap = reservedQtyMap(orders);

        System.out.println("  ┌─────────┬──────────────────┬──────────┬──────┬──────┬──────┐");
        System.out.println("  │  ID     │  이름             │  사양    │ 재고  │ 수율  │ 시간  │");
        System.out.println("  ├─────────┼──────────────────┼──────────┼──────┼──────┼──────┤");
        for (Sample s : samples) {
            int resQty = resMap.getOrDefault(s.getSampleId(), 0);
            String tag = s.getStock() == 0 ? "🔴" : (s.getStock() < resQty ? "🟡" : "🟢");
            System.out.printf("  │ %-7s │ %-16s │ %-8s │%4d  │ %.2f │ %2dh  │  %s%n",
                    s.getSampleId(), s.getName(), s.getSpec(),
                    s.getStock(), s.getYield(), s.getProductionTime(), tag);
        }
        System.out.println("  └─────────┴──────────────────┴──────────┴──────┴──────┴──────┘");
        System.out.printf("%n  🟢 여유  🟡 부족  🔴 고갈    총 %d종 / 총 재고 %d개%n",
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
        System.out.println("  ┌────┬─────────┬──────────────────┬──────────┬──────┬──────┐");
        System.out.println("  │  # │  ID     │  이름             │  사양    │ 재고  │      │");
        System.out.println("  ├────┼─────────┼──────────────────┼──────────┼──────┼──────┤");
        for (int i = 0; i < samples.size(); i++) {
            Sample s = samples.get(i);
            int resQty = resMap.getOrDefault(s.getSampleId(), 0);
            String tag = s.getStock() == 0 ? "🔴" : (s.getStock() < resQty ? "🟡" : "🟢");
            System.out.printf("  │ %2d │ %-7s │ %-16s │ %-8s │%4d  │  %s  │%n",
                    i + 1, s.getSampleId(), s.getName(), s.getSpec(), s.getStock(), tag);
        }
        System.out.println("  └────┴─────────┴──────────────────┴──────────┴──────┴──────┘");
    }

    // ─────────────────────── 주문 화면 ───────────────────────

    public void showOrderList(List<Order> orders, List<Sample> samples, String path) {
        printHeader(path);
        if (orders.isEmpty()) {
            showInfo("조회 가능한 주문이 없습니다.");
            return;
        }
        Map<String, Sample> sm = sampleMap(samples);
        System.out.println("  ┌──────────────┬──────────────────────────┬───────┐");
        System.out.println("  │    주문 ID    │  시료                     │  수량  │");
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
        System.out.println("  │  # │    주문 ID    │  시료                     │  수량  │");
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
        System.out.printf ("  현재 재고      : %d개%n", sample.getStock());
        System.out.printf ("  수율           : %.2f%n", sample.getYield());
        System.out.printf ("  단위 생산시간  : %dh/개%n", sample.getProductionTime());
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf ("  주문 수량      : %d개%n", order.getQuantity());
        if (sample.getStock() >= order.getQuantity()) {
            System.out.println(GREEN + "  재고 상태      : ✅ 충분  →  승인 시 즉시 CONFIRMED" + RESET);
        } else {
            int shortage = order.getQuantity() - sample.getStock();
            System.out.println(YELLOW + "  재고 상태      : ⚠️  부족  →  승인 시 PRODUCING (생산 필요)" + RESET);
            System.out.printf (YELLOW + "  부족분         : %d개  (재고 %d개 < 주문 %d개)%n" + RESET,
                    shortage, sample.getStock(), order.getQuantity());
        }
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.println("  처리를 선택하세요.");
        System.out.print("  [1] 승인   [2] 거절   [0] 취소\n  선택> ");
    }

    // ─────────────────────── 모니터링 화면 ───────────────────────

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

    // ─────────────────────── 생산라인 화면 ───────────────────────

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

            double elapsedH = (now - p.getStartedAt()) / 3_600_000.0;
            double totalH   = p.getEstimatedHours();
            double ratio    = totalH > 0 ? Math.min(1.0, elapsedH / totalH) : 1.0;
            double remainH  = Math.max(0.0, totalH - elapsedH);
            int    curQty   = (int)(ratio * p.getProductionQty());
            long   endAt    = p.getStartedAt() + p.getEstimatedHours() * 3_600_000L;

            System.out.println("  ┌─────────────────────────────────────────────────────┐");
            System.out.printf ("  │  ⏳ %s  →  %s%n", p.getProductionId(), o.getOrderId());
            System.out.printf ("  │  시료  %s  %s  (수율 %.2f / 단위 %dh/개)%n",
                    s.getSampleId(), s.getName(), s.getYield(), s.getProductionTime());
            System.out.println("  ├──────────────────────┬──────────────────────────────┤");
            System.out.printf ("  │  주문 수량  %5d개   │  부족분       %5d개         │%n",
                    o.getQuantity(), p.getShortageQty());
            System.out.printf ("  │  실 생산량  %5d개   │  총 생산시간  %5dh          │%n",
                    p.getProductionQty(), p.getEstimatedHours());
            System.out.println("  ├──────────────────────┴──────────────────────────────┤");
            System.out.printf ("  │  시작      %s%n", fmtDt(p.getStartedAt()));
            System.out.printf ("  │  완료 예정  %s  (남은 %.1fh)%n", fmtDt(endAt), remainH);
            String pBar = progressBar(ratio, 12);
            System.out.printf ("  │  진행률    %s  %3.0f%%%n", pBar, ratio * 100);
            System.out.printf ("  │  현재 생산량  ≈ %3d개 / %3d개%n", curQty, p.getProductionQty());
            System.out.println("  └─────────────────────────────────────────────────────┘");
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

            double remainH = Math.max(0.0, p.getEstimatedHours() - (now - p.getStartedAt()) / 3_600_000.0);
            long   endAt   = p.getStartedAt() + p.getEstimatedHours() * 3_600_000L;

            System.out.printf("  [ %d순위 ]%n", i + 1);
            System.out.println("  ┌─────────────────────────────────────────────────────┐");
            System.out.printf ("  │  %s  →  %s%n", p.getProductionId(), o.getOrderId());
            System.out.printf ("  │  시료  %s  %s  (수율 %.2f / 단위 %dh/개)%n",
                    s.getSampleId(), s.getName(), s.getYield(), s.getProductionTime());
            System.out.println("  ├──────────────────────┬──────────────────────────────┤");
            System.out.printf ("  │  주문 수량  %5d개   │  부족분       %5d개         │%n",
                    o.getQuantity(), p.getShortageQty());
            System.out.printf ("  │  실 생산량  %5d개   │  총 생산시간   %5dh         │%n",
                    p.getProductionQty(), p.getEstimatedHours());
            System.out.println("  ├──────────────────────┴──────────────────────────────┤");
            System.out.printf ("  │  시작      %s%n", fmtDt(p.getStartedAt()));
            System.out.printf ("  │  완료 예정  %s  (남은 %.1fh)%n", fmtDt(endAt), remainH);
            System.out.println("  └─────────────────────────────────────────────────────┘");
            System.out.println();
        }
    }

    public void liveRefreshLoop(Runnable renderer) {
        while (true) {
            System.out.print(CLEAR);
            renderer.run();
            System.out.println("\n  [Q + Enter] 돌아가기");
            long deadline = System.currentTimeMillis() + 2_500;
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (System.in.available() > 0) {
                        int ch = System.in.read();
                        if (ch == 'q' || ch == 'Q' || ch == '\n' || ch == '\r') return;
                    }
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    return;
                }
            }
        }
    }

    // ─────────────────────── 출고 화면 ───────────────────────

    public void showReleaseListNumbered(List<Order> orders, List<Sample> samples) {
        printHeader("메인 > 출고 관리");
        if (orders.isEmpty()) {
            showInfo("출고 대기 중인 주문이 없습니다.");
            return;
        }
        Map<String, Sample> sm = sampleMap(samples);
        System.out.println("  [ 출고 대기 주문 목록  (CONFIRMED) ]");
        System.out.println("  ┌────┬──────────────┬──────────────────────────┬───────┐");
        System.out.println("  │  # │    주문 ID    │  시료                     │  수량  │");
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

    public void showReleaseConfirm(Order order, Sample sample) {
        System.out.println();
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.println("  [ 출고 확인 ]");
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf ("  주문 ID       : %s%n", order.getOrderId());
        System.out.printf ("  시료 ID       : %s  (%s)%n", sample.getSampleId(), sample.getName());
        System.out.printf ("  사양          : %s%n", sample.getSpec());
        System.out.printf ("  출고 수량      : %d개%n", order.getQuantity());
        System.out.printf ("  현재 재고      : %d개%n", sample.getStock());
        System.out.printf ("  출고 후 재고   : %d개%n", sample.getStock() - order.getQuantity());
        System.out.println("  ─────────────────────────────────────────────────────");
    }
}
