package com.meteorcat.mix.core;

import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Actor 状态哈希表
 */
public class ActorStateHashMap extends ConcurrentHashMap<WebSocketSession, Integer> {
}
