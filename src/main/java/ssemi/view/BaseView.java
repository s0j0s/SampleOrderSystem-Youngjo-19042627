package ssemi.view;

import ssemi.model.Order;
import ssemi.model.OrderStatus;
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

public abstract class BaseView {

    // ─── ANSI ────────────────────────────────────────────────
    protected static final String RESET  = "\033[0m";
    protected static final String BOLD   = "\033[1m";
    protected static final String CYAN   = "\033[36m";
    protected static final String GREEN  = "\033[32m";
    protected static final String YELLOW = "\033[33m";
    protected static final String RED    = "\033[31m";
    protected static final String CLEAR  = "\033[2J\033[H";

    protected static final String SEP_THICK = "================================================================================";
    protected static final String SEP_THIN  = "--------------------------------------------------------------------------------";

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // stdin은 하나이므로 static 공유 스캐너 사용
    protected static final Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);

    // ─── 레이아웃 헬퍼 ───────────────────────────────────────

    public void printHeader(String path) {
        System.out.println();
        System.out.println(SEP_THICK);
        System.out.println(path);
        System.out.println(SEP_THICK);
    }

    protected String statusEmoji(OrderStatus status) {
        return switch (status) {
            case RESERVED  -> "📋";
            case CONFIRMED -> "✅";
            case PRODUCING -> "⏳";
            case RELEASE   -> "📦";
            case REJECTED  -> "❌";
        };
    }

    protected String bar(int value, int max, int width) {
        if (max == 0) return "░".repeat(width);
        int filled = (int) Math.round((double) value / max * width);
        return "▓".repeat(Math.min(filled, width)) + "░".repeat(width - Math.min(filled, width));
    }

    protected String progressBar(double ratio, int width) {
        int filled = (int) Math.min(Math.round(ratio * width), width);
        return "▓".repeat(filled) + "░".repeat(width - filled);
    }

    protected int visualWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            if ((cp >= 0xAC00 && cp <= 0xD7AF) ||
                (cp >= 0x1100 && cp <= 0x11FF) ||
                (cp >= 0x3130 && cp <= 0x318F) ||
                (cp >= 0x2300 && cp <= 0x27BF) ||
                cp > 0xFFFF) {
                w += 2;
            } else {
                w += 1;
            }
            i += Character.charCount(cp);
        }
        return w;
    }

    protected String padVisual(String s, int width) {
        int pad = Math.max(0, width - visualWidth(s));
        return s + " ".repeat(pad);
    }

    protected String fmtDt(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis),
                ZoneId.systemDefault()).format(DT_FMT);
    }

    protected Map<String, Sample> sampleMap(List<Sample> samples) {
        return samples.stream().collect(Collectors.toMap(Sample::getSampleId, s -> s));
    }

    protected Map<String, Order> orderMap(List<Order> orders) {
        return orders.stream().collect(Collectors.toMap(Order::getOrderId, o -> o));
    }

    protected Map<String, Integer> reservedQtyMap(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.RESERVED)
                .collect(Collectors.groupingBy(Order::getSampleId,
                        Collectors.summingInt(Order::getQuantity)));
    }

    // ─── 입력 ────────────────────────────────────────────────

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

    // ─── 출력 ────────────────────────────────────────────────

    public void showSuccess(String message) {
        System.out.println(GREEN + "  ✔ " + message + RESET);
    }

    public void showError(String message) {
        System.out.println(RED + "  ✘ " + message + RESET);
    }

    public void showInfo(String message) {
        System.out.println(CYAN + "  ℹ " + message + RESET);
    }

    // ─── 실시간 갱신 루프 ────────────────────────────────────

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
}
