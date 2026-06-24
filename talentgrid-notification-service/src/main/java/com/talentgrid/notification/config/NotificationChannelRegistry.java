package com.talentgrid.notification.config;

import com.talentgrid.notification.channel.NotificationChannelHandler;
import com.talentgrid.notification.model.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry that maps {@link NotificationChannelType} values to their
 * {@link NotificationChannelHandler} implementations.
 *
 * <p>All {@code NotificationChannelHandler} beans are auto-discovered via
 * Spring's constructor injection of the full list, then indexed by channel type.</p>
 *
 * <p>To add a new channel: implement {@link NotificationChannelHandler},
 * annotate with {@code @Service}, and it will be auto-registered here at startup.</p>
 *
 * <p><strong>FIX:</strong> Added {@link #registeredChannelNames()} method so the
 * orchestrator can log which handlers are actually registered when an unknown
 * channel name is encountered.</p>
 */
@Slf4j
@Component
public class NotificationChannelRegistry {

    private final Map<NotificationChannelType, NotificationChannelHandler> registry =
            new EnumMap<>(NotificationChannelType.class);

    public NotificationChannelRegistry(List<NotificationChannelHandler> handlers) {
        for (NotificationChannelHandler handler : handlers) {
            registry.put(handler.channelType(), handler);
            log.info("[CHANNEL-REGISTRY] Registered channel handler: {} -> {}",
                    handler.channelType(), handler.getClass().getSimpleName());
        }
        log.info("[CHANNEL-REGISTRY] Startup complete. Registered channels: {}",
                registry.keySet());
    }

    /**
     * Looks up the handler for the given channel type string (case-insensitive).
     *
     * @param channelName channel name from the Kafka payload (e.g. "EMAIL", "IN_APP")
     * @return the handler, or empty if the channel is not registered
     */
    public Optional<NotificationChannelHandler> findHandler(String channelName) {
        try {
            NotificationChannelType type = NotificationChannelType.valueOf(channelName.toUpperCase());
            return Optional.ofNullable(registry.get(type));
        } catch (IllegalArgumentException e) {
            log.warn("[CHANNEL-REGISTRY] Unknown channel type: '{}' — skipping", channelName);
            return Optional.empty();
        }
    }

    public int registeredChannelCount() {
        return registry.size();
    }

    /** Returns a readable list of registered channel names — used in diagnostic logs. */
    public List<String> registeredChannelNames() {
        return registry.keySet().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}