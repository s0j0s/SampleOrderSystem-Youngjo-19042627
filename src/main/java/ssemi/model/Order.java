package ssemi.model;

public class Order {
    private String orderId;
    private String sampleId;
    private String customerId;
    private int quantity;
    private OrderStatus status;

    public Order(String orderId, String sampleId, String customerId, int quantity, OrderStatus status) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public String getSampleId() { return sampleId; }
    public String getCustomerId() { return customerId; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }

    public void setStatus(OrderStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("[%s] 시료:%s 고객:%s 수량:%d 상태:%s",
                orderId, sampleId, customerId, quantity, status);
    }
}
