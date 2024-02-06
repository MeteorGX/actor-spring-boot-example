package com.meteorcat.mix.core;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/**
 * 消息推送帧
 *
 * @param session
 * @param message
 */
public record MessageFrame(@NonNull WebSocketSession session, @NonNull Optional<TextMessage> message) {


    @Override
    public String toString() {
        return "MessageFrame{" +
                "session=" + session +
                ", message=" + message +
                '}';
    }
}
