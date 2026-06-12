package ssemi.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ssemi.model.Sample;
import ssemi.repository.SampleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SampleControllerTest {

    @Mock
    private SampleRepository sampleRepository;

    @InjectMocks
    private SampleController sampleController;

    @Test
    void 시료_등록_성공() {
        when(sampleRepository.nextSequence()).thenReturn(1);

        Sample sample = sampleController.registerSample("GaN 웨이퍼", "4인치 GaN", 50);

        assertEquals("S001", sample.getSampleId());
        assertEquals("GaN 웨이퍼", sample.getName());
        assertEquals(50, sample.getStock());
        verify(sampleRepository).save(any(Sample.class));
    }

    @Test
    void 시료_ID_형식_S세자리_숫자() {
        when(sampleRepository.nextSequence()).thenReturn(5);

        Sample sample = sampleController.registerSample("SiC 웨이퍼", "6인치", 30);

        assertEquals("S005", sample.getSampleId());
    }

    @Test
    void 전체_시료_목록_반환() {
        List<Sample> expected = List.of(
                new Sample("S001", "GaN", "4인치", 50),
                new Sample("S002", "SiC", "6인치", 30)
        );
        when(sampleRepository.findAll()).thenReturn(expected);

        List<Sample> result = sampleController.getAllSamples();

        assertEquals(2, result.size());
        verify(sampleRepository).findAll();
    }

    @Test
    void ID로_시료_조회() {
        Sample sample = new Sample("S001", "GaN", "4인치", 50);
        when(sampleRepository.findById("S001")).thenReturn(Optional.of(sample));

        Optional<Sample> result = sampleController.getSampleById("S001");

        assertTrue(result.isPresent());
        assertEquals("S001", result.get().getSampleId());
    }
}
