package com.careconnect.config;

import com.careconnect.websocket.CallNotificationHandler;
import com.careconnect.websocket.CareConnectWebSocketHandler;
import com.careconnect.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private CallNotificationHandler callNotificationHandler;

    @Mock
    private CareConnectWebSocketHandler careConnectWebSocketHandler;

    @Mock
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration handlerRegistration;

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig();
        ReflectionTestUtils.setField(webSocketConfig, "callNotificationHandler", callNotificationHandler);
        ReflectionTestUtils.setField(webSocketConfig, "careConnectWebSocketHandler", careConnectWebSocketHandler);
        ReflectionTestUtils.setField(webSocketConfig, "notificationWebSocketHandler", notificationWebSocketHandler);
        ReflectionTestUtils.setField(webSocketConfig, "careConnectEndpoint", "/ws/careconnect");
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", "*");

        lenient().when(registry.addHandler(any(), anyString())).thenReturn(handlerRegistration);
        lenient().when(handlerRegistration.setAllowedOrigins(anyString())).thenReturn(handlerRegistration);
    }

    @Test
    void registersCallNotificationHandlerOnWsCalls() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(callNotificationHandler, "/ws/calls");
    }

    @Test
    void registersCareConnectHandlerOnDefaultEndpoint() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(careConnectWebSocketHandler, "/ws/careconnect");
    }

    @Test
    void registersNotificationHandlerOnWsNotifications() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(notificationWebSocketHandler, "/ws/notifications");
    }

    @Test
    void registersExactlyThreeHandlers() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry, times(3)).addHandler(any(), anyString());
    }

    @Test
    void setsAllowedOriginsOnAllHandlerRegistrations() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(handlerRegistration, times(3)).setAllowedOrigins("*");
    }

    @Test
    void callsAndCareConnectHandlersUseSockJs() {
        webSocketConfig.registerWebSocketHandlers(registry);

        // /ws/calls and /ws/careconnect both enable SockJS; /ws/notifications does not
        verify(handlerRegistration, times(2)).withSockJS();
    }

    @Test
    void notificationHandlerDoesNotUseSockJs() {
        WebSocketHandlerRegistration callsReg = mock(WebSocketHandlerRegistration.class);
        WebSocketHandlerRegistration careConnectReg = mock(WebSocketHandlerRegistration.class);
        WebSocketHandlerRegistration notificationsReg = mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(callNotificationHandler, "/ws/calls")).thenReturn(callsReg);
        when(registry.addHandler(careConnectWebSocketHandler, "/ws/careconnect")).thenReturn(careConnectReg);
        when(registry.addHandler(notificationWebSocketHandler, "/ws/notifications")).thenReturn(notificationsReg);

        when(callsReg.setAllowedOrigins(anyString())).thenReturn(callsReg);
        when(careConnectReg.setAllowedOrigins(anyString())).thenReturn(careConnectReg);
        when(notificationsReg.setAllowedOrigins(anyString())).thenReturn(notificationsReg);

        webSocketConfig.registerWebSocketHandlers(registry);

        verify(callsReg).withSockJS();
        verify(careConnectReg).withSockJS();
        verify(notificationsReg, never()).withSockJS();
    }

    @Test
    void usesCustomCareConnectEndpointWhenSet() {
        ReflectionTestUtils.setField(webSocketConfig, "careConnectEndpoint", "/ws/custom");

        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(careConnectWebSocketHandler, "/ws/custom");
    }

    @Test
    void usesCustomAllowedOriginsWhenSet() {
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", "https://app.careconnect.com");

        webSocketConfig.registerWebSocketHandlers(registry);

        verify(handlerRegistration, times(3)).setAllowedOrigins("https://app.careconnect.com");
    }

    @Test
    void defaultEndpointIsWsCareconnect() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry, never()).addHandler(careConnectWebSocketHandler, "/ws/calls");
        verify(registry, never()).addHandler(careConnectWebSocketHandler, "/ws/notifications");
        verify(registry).addHandler(careConnectWebSocketHandler, "/ws/careconnect");
    }

    @Test
    void eachHandlerRegisteredOnDistinctPath() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(callNotificationHandler, "/ws/calls");
        verify(registry).addHandler(careConnectWebSocketHandler, "/ws/careconnect");
        verify(registry).addHandler(notificationWebSocketHandler, "/ws/notifications");
        verifyNoMoreInteractions(registry);
    }
}
