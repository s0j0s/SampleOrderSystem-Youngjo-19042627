package ssemi.model;

public class Sample {
    private String sampleId;
    private String name;
    private String spec;
    private int stock;

    public Sample(String sampleId, String name, String spec, int stock) {
        this.sampleId = sampleId;
        this.name = name;
        this.spec = spec;
        this.stock = stock;
    }

    public String getSampleId() { return sampleId; }
    public String getName() { return name; }
    public String getSpec() { return spec; }
    public int getStock() { return stock; }

    public void setName(String name) { this.name = name; }
    public void setSpec(String spec) { this.spec = spec; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) 재고: %d", sampleId, name, spec, stock);
    }
}
