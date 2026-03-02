package com.careconnect.service.evv;

import com.careconnect.model.evv.EvvAuditEvent;
import com.careconnect.model.evv.EvvRecord;
import com.careconnect.repository.evv.EvvAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLoggerTest {

    @Mock EvvAuditEventRepository repo;
    @Mock EvvRecord               evvRecord;

    @InjectMocks AuditLogger auditLogger;

    @Test
    void log_savesAuditEventWithCorrectFields() {
        Map<String, Object> deviceInfo = Map.of("device", "mobile");
        Map<String, Object> details   = Map.of("key", "value");

        when(evvRecord.getDeviceInfo()).thenReturn(deviceInfo);

        auditLogger.log(evvRecord, 42L, "CREATED", details);

        ArgumentCaptor<EvvAuditEvent> captor = ArgumentCaptor.forClass(EvvAuditEvent.class);
        verify(repo).save(captor.capture());

        EvvAuditEvent saved = captor.getValue();
        assertThat(saved.getEvvRecord()).isEqualTo(evvRecord);
        assertThat(saved.getActorUserId()).isEqualTo(42L);
        assertThat(saved.getEventType()).isEqualTo("CREATED");
        assertThat(saved.getDeviceInfo()).isEqualTo(deviceInfo);
        assertThat(saved.getDetails()).isEqualTo(details);
    }

    @Test
    void log_withNullDetails_savesAuditEvent() {
        when(evvRecord.getDeviceInfo()).thenReturn(null);

        auditLogger.log(evvRecord, 1L, "APPROVED", null);

        ArgumentCaptor<EvvAuditEvent> captor = ArgumentCaptor.forClass(EvvAuditEvent.class);
        verify(repo).save(captor.capture());

        EvvAuditEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("APPROVED");
        assertThat(saved.getDetails()).isNull();
        assertThat(saved.getDeviceInfo()).isNull();
    }
}
