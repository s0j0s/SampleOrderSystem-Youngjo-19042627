package ssemi.controller;

import ssemi.model.Sample;
import ssemi.repository.SampleRepository;

import java.util.List;
import java.util.Optional;

public class SampleController {

    private final SampleRepository sampleRepository;

    public SampleController(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    public Sample registerSample(String name, String spec, int stock) {
        String sampleId = String.format("S-%03d", sampleRepository.nextSequence());
        Sample sample = new Sample(sampleId, name, spec, stock);
        sampleRepository.save(sample);
        return sample;
    }

    public List<Sample> getAllSamples() {
        return sampleRepository.findAll();
    }

    public Optional<Sample> getSampleById(String sampleId) {
        return sampleRepository.findById(sampleId);
    }
}
