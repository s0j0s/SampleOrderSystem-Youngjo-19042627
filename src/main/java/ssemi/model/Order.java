package ssemi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Order {
    private String orderId;
    private String sampleId;
    private String customerId;
    private int quantity;
    @Setter private OrderStatus status;
    private long createdAt;
}
