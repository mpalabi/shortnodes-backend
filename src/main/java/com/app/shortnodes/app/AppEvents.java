package com.app.shortnodes.app;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.util.*;

@Service
public class AppEvents {
    private final Map<UUID, List<SseEmitter>> emittersByApp = new HashMap<>();
    private final SimpMessagingTemplate messaging;

    public AppEvents(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public SseEmitter subscribe(UUID appId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByApp.computeIfAbsent(appId, k -> new ArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(appId, emitter));
        emitter.onTimeout(() -> remove(appId, emitter));
        return emitter;
    }

    private void remove(UUID appId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByApp.getOrDefault(appId, Collections.emptyList());
        list.remove(emitter);
    }

    public void publishChange(UUID appId, String type) {
        // SSE broadcast
        List<SseEmitter> list = new ArrayList<>(emittersByApp.getOrDefault(appId, Collections.emptyList()));
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name("change").data(type));
            } catch (IOException ignored) { remove(appId, e); }
        }
        // WebSocket broadcast
        try {
            messaging.convertAndSend("/topic/apps/" + appId + "/changes", Map.of("type", type));
        } catch (Exception ignored) {}
    }

    public void publishTree(UUID appId, Object treePayload) {
        // WebSocket: push full snapshot to avoid extra fetches
        try {
            messaging.convertAndSend("/topic/apps/" + appId + "/changes", Map.of(
                    "type", "tree",
                    "tree", treePayload
            ));
        } catch (Exception ignored) {}
        // SSE: still emit a simple change; frontend will refetch as fallback
        publishChange(appId, "tree");
    }

    public void publishSaveConfirmation(UUID appId, String message) {
        // WebSocket: send save confirmation
        try {
            messaging.convertAndSend("/topic/apps/" + appId + "/changes", Map.of(
                    "type", "save-confirmation",
                    "message", message
            ));
        } catch (Exception ignored) {}
    }
}


