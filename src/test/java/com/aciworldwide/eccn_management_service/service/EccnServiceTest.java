package com.aciworldwide.eccn_management_service.service;

import com.aciworldwide.eccn_management_service.exception.InvalidEccnFormatException;
import com.aciworldwide.eccn_management_service.model.Eccn;
import com.aciworldwide.eccn_management_service.repository.EccnRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EccnServiceTest {

    @Mock
    private EccnRepository eccnRepository;

    @InjectMocks
    private EccnService eccnService;

    @Test
    void createEccn_validFormat_shouldSave() {
        Eccn eccn = new Eccn();
        eccn.setCommodityCode("5D002");
        when(eccnRepository.save(any(Eccn.class))).thenReturn(eccn);

        Eccn result = eccnService.createEccn(eccn);
        
        assertNotNull(result);
        assertEquals("5D002", result.getCommodityCode());
        verify(eccnRepository).save(eccn);
    }

    @Test
    void createEccn_invalidFormat_shouldThrowException() {
        Eccn eccn = new Eccn();
        eccn.setCommodityCode("INVALID");

        assertThrows(InvalidEccnFormatException.class, () -> eccnService.createEccn(eccn));
        verify(eccnRepository, never()).save(any());
    }

    @Test
    void findByCommodityCode_validCode_shouldReturnList() {
        Eccn eccn = new Eccn();
        eccn.setCommodityCode("5D002");
        when(eccnRepository.findByCommodityCode("5D002")).thenReturn(Collections.singletonList(eccn));

        List<Eccn> result = eccnService.findByCommodityCode("5D002");
        
        assertFalse(result.isEmpty());
        assertEquals("5D002", result.get(0).getCommodityCode());
    }

    @Test
    void deprecateEccn_validInput_shouldUpdate() {
        Eccn eccn = new Eccn();
        eccn.setId("1");
        when(eccnRepository.findById("1")).thenReturn(Optional.of(eccn));
        when(eccnRepository.save(any(Eccn.class))).thenReturn(eccn);

        eccnService.deprecateEccn("1", "Obsolete", "2");
        
        assertTrue(eccn.isDeprecated());
        assertEquals("Obsolete", eccn.getDeprecationReason());
        assertEquals("2", eccn.getReplacementEccnId());
        verify(eccnRepository).save(eccn);
    }

    @Test
    void getSupersedingEccn_exists_shouldReturnEccn() {
        Eccn original = new Eccn();
        original.setReplacementEccnId("2");
        Eccn replacement = new Eccn();
        when(eccnRepository.findById("1")).thenReturn(Optional.of(original));
        when(eccnRepository.findById("2")).thenReturn(Optional.of(replacement));

        Eccn result = eccnService.getSupersedingEccn("1");
        
        assertNotNull(result);
        assertEquals(replacement, result);
    }

    @Test
    void bulkCreateEccn_validList_shouldSaveAll() {
        Eccn eccn1 = new Eccn();
        eccn1.setCommodityCode("5D002");
        Eccn eccn2 = new Eccn();
        eccn2.setCommodityCode("5A002");
        when(eccnRepository.saveAll(anyList())).thenReturn(Arrays.asList(eccn1, eccn2));

        List<Eccn> result = eccnService.bulkCreateEccn(Arrays.asList(eccn1, eccn2));
        
        assertEquals(2, result.size());
        verify(eccnRepository).saveAll(anyList());
    }

    @Test
    void validateEccnFormat_valid_shouldNotThrow() {
        assertDoesNotThrow(() -> eccnService.validateEccnFormat("5D002"));
    }

    @Test
    void validateEccnFormat_invalid_shouldThrow() {
        assertThrows(InvalidEccnFormatException.class, () -> eccnService.validateEccnFormat("INVALID"));
    }
}