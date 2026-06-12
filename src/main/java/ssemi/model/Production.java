package ssemi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Production {
    private String productionId;
    private String orderId;
    private String sampleId;
    private int    productionQty;
    private long   estimatedHours;
    @Setter private boolean completed;
    private long   startedAt;
    private int    shortageQty;
}
