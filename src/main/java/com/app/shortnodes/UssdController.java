package com.app.shortnodes;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ussd")
public class UssdController {

    private final com.app.shortnodes.session.SessionService sessionService;
    private final com.app.shortnodes.flow.FlowEngine flowEngine;

    public UssdController(com.app.shortnodes.session.SessionService sessionService,
                          com.app.shortnodes.flow.FlowEngine flowEngine) {
        this.sessionService = sessionService;
        this.flowEngine = flowEngine;
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<String> handle(@PathVariable java.util.UUID sessionId,
                                         @RequestBody(required = false) String input,
                                         @RequestParam(value = "flowId", required = false) java.util.UUID flowId) {
        var session = sessionService.getOrCreate(sessionId);
        if (flowId != null && (session.getFlowId() == null || !flowId.equals(session.getFlowId()))) {
            session.setFlowId(flowId);
            sessionService.touch(session);
        }

        String sanitized = input == null ? "" : input.trim();
        String response;
        if (sanitized.isEmpty()) {
            if (session.getFlowId() != null) {
                var rendered = flowEngine.renderWelcome(session.getFlowId());
                response = rendered.orElse("CON Welcome to ShortNodes");
            } else {
                response = "CON Welcome to ShortNodes\n1. Balance\n2. Send Money";
            }
        } else if ("1".equals(sanitized)) {
            response = "END Your balance is 100.00";
        } else if ("2".equals(sanitized)) {
            response = "CON Enter recipient number:";
        } else {
            response = "CON Invalid option.\n1. Balance\n2. Send Money";
        }
        sessionService.touch(session);
        return ResponseEntity.ok(response);
    }
}
