package com.meteorcat.mix.core;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 用于推送的网络数据帧
 * @param session 句柄
 * @param message 数据
 */
public record MessageFrame(@NonNull WebSocketSession session, TextMessage message) {
}
