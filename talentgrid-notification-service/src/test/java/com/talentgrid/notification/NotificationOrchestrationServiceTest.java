package com.talentgrid.notification;

import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.notification.channel.NotificationChannelHandler;
import com.talentgrid.notification.config.NotificationChannelRegistry;
import com.talentgrid.notification.model.NotificationChannelType;
import com.talentgrid.notification.service.NotificationOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOrchestrationService unit tests")
class NotificationOrchestrationServiceTest {

    @Mock
    private NotificationChannelRegistry channelRegistry;

    @Mock
    private NotificationChannelHandler inAppHandler;

    @Mock
    private NotificationChannelHandler emailHandler;

    private NotificationOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationOrchestrationService(channelRegistry);
    }

    @Test
    @DisplayName("should fan out to both IN_APP and EMAIL channels")
    void shouldDeliverToBothChannels() throws Exception {
        when(channelRegistry.findHandler("IN_APP")).thenReturn(Optional.of(inAppHandler));
        when(channelRegistry.findHandler("EMAIL")).thenReturn(Optional.of(emailHandler));

        NotificationPayload payload = NotificationPayload.builder()
                .recipientUserId("101")
                .recipientEmail("user@example.com")
                .notificationType("DEMAND_APPROVED")
                .title("Demand approved")
                .message("Your demand has been approved.")
                .channels(List.of("IN_APP", "EMAIL"))
                .moduleName("demand-service")
                .referenceId("demand-42")
                .referenceType("DEMAND")
                .priority("HIGH")
                .build();

        service.process(payload);

        verify(inAppHandler, times(1)).deliver(payload);
        verify(emailHandler, times(1)).deliver(payload);
    }

    @Test
    @DisplayName("should continue delivering to other channels when one fails")
    void shouldContinueOnChannelFailure() throws Exception {
        when(channelRegistry.findHandler("IN_APP")).thenReturn(Optional.of(inAppHandler));
        when(channelRegistry.findHandler("EMAIL")).thenReturn(Optional.of(emailHandler));

        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailHandler).deliver(any());

        NotificationPayload payload = NotificationPayload.builder()
                .recipientUserId("101")
                .channels(List.of("IN_APP", "EMAIL"))
                .notificationType("DEMAND_CREATED")
                .title("Test")
                .message("Test message")
                .build();

        // Should NOT throw — failure in EMAIL must not block IN_APP
        service.process(payload);

        verify(inAppHandler, times(1)).deliver(payload);
        verify(emailHandler, times(1)).deliver(payload);
    }

    @Test
    @DisplayName("should default to IN_APP when channels list is null")
    void shouldDefaultToInAppWhenChannelsNull() throws Exception {
        when(channelRegistry.findHandler("IN_APP")).thenReturn(Optional.of(inAppHandler));

        NotificationPayload payload = NotificationPayload.builder()
                .recipientUserId("101")
                .notificationType("TEST")
                .title("Test")
                .message("Test message")
                .channels(null)
                .build();

        service.process(payload);

        verify(inAppHandler, times(1)).deliver(payload);
        verify(emailHandler, never()).deliver(any());
    }

    @Test
    @DisplayName("should skip unknown channel types gracefully")
    void shouldSkipUnknownChannels() throws Exception {
        when(channelRegistry.findHandler("UNKNOWN_CHANNEL")).thenReturn(Optional.empty());

        NotificationPayload payload = NotificationPayload.builder()
                .recipientUserId("101")
                .notificationType("TEST")
                .title("Test")
                .message("Test message")
                .channels(List.of("UNKNOWN_CHANNEL"))
                .build();

        // Should not throw
        service.process(payload);

        verify(inAppHandler, never()).deliver(any());
    }

    @Test
    @DisplayName("should handle null payload without throwing")
    void shouldHandleNullPayloadGracefully() {
        service.process(null);
        verifyNoInteractions(channelRegistry);
    }
}
