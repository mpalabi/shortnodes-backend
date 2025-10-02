package com.app.shortnodes.flow;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FlowEngine {

    private final FlowRepository flowRepository;

    public FlowEngine(FlowRepository flowRepository) {
        this.flowRepository = flowRepository;
    }

    public Optional<String> renderWelcome(java.util.UUID flowId) {
        return flowRepository.findById(flowId).map(flow -> {
            JSONObject def = new JSONObject(flow.getJsonDefinition());
            JSONArray nodes = def.getJSONArray("nodes");
            // find first menuNode
            for (int i = 0; i < nodes.length(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                String type = node.optString("type");
                if ("subflow".equalsIgnoreCase(type) || "subflowNode".equalsIgnoreCase(type)) {
                    // segmented flows: follow into linked flow and render its welcome
                    java.util.UUID subId = null;
                    try {
                        JSONObject data = node.optJSONObject("data");
                        String idStr = data != null ? data.optString("subflowId", null) : null;
                        if (idStr != null && !idStr.isBlank()) {
                            subId = java.util.UUID.fromString(idStr);
                        }
                    } catch (Exception ignored) {}
                    if (subId != null) {
                        return renderWelcome(subId).orElse("CON Welcome");
                    }
                }
                if ("menuNode".equals(type)) {
                    JSONObject data = node.optJSONObject("data");
                    String text = data != null ? data.optString("text", "Welcome") : "Welcome";
                    JSONArray options = data != null ? data.optJSONArray("options") : null;
                    StringBuilder sb = new StringBuilder("CON ").append(text);
                    if (options != null) {
                        for (int j = 0; j < options.length(); j++) {
                            JSONObject opt = options.getJSONObject(j);
                            sb.append("\n").append(opt.optInt("key")).append(". ").append(opt.optString("label"));
                        }
                    }
                    return sb.toString();
                }
            }
            return "CON Welcome";
        });
    }
}

