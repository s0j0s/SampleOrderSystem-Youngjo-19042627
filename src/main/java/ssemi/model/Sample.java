package ssemi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Sample {
    private String sampleId;
    private String name;
    private String spec;
    @Setter private int stock;
    private double yield;
    private int productionTime;
}
