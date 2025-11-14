package com.app.shortnodes.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/app-ussd")
public class AppUssdController {
    private final AppFlowEngine engine;
    private final MenuRepository menuRepo;

    public AppUssdController(AppFlowEngine engine, MenuRepository menuRepo) {
        this.engine = engine;
        this.menuRepo = menuRepo;
    }

    public static class UssdRequest {
        public UUID appId;
        public UUID menuId; // optional
        public String input; // optional
    }

    // JSON variant: accepts { appId, menuId?, input? }
    @PostMapping("/v2/{sessionId}")
    public ResponseEntity<String> handleJson(@PathVariable UUID sessionId, @RequestBody UssdRequest body) {
        UUID appId = body.appId;
        UUID menuId = body.menuId;
        String sanitized = body.input == null ? "" : body.input.trim();
        if (menuId == null) {
            return engine.entryMenu(appId)
                    .map(m -> ResponseEntity.ok()
                            .header("X-Menu-Id", m.getId().toString())
                            .body(engine.render(m)))
                    .orElse(ResponseEntity.ok("CON App has no menus"));
        }
        return menuRepo.findById(menuId)
                .map(current -> {
                    if (sanitized.isEmpty()) {
                        return ResponseEntity.ok()
                                .header("X-Menu-Id", current.getId().toString())
                                .body(engine.render(current));
                    }
                    return engine.nextFromInput(current, sanitized)
                            .map(next -> ResponseEntity.ok()
                                    .header("X-Menu-Id", next.getId().toString())
                                    .body(engine.render(next)))
                            .orElse(ResponseEntity.ok("CON Invalid input"));
                })
                .orElse(ResponseEntity.ok("CON Menu not found"));
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<String> handle(@PathVariable UUID sessionId,
                                         @RequestParam UUID appId,
                                         @RequestParam(required = false) UUID menuId,
                                         @RequestBody(required = false) String input) {
        String sanitized = input == null ? "" : input.trim();
        if (menuId == null) {
            return engine.entryMenu(appId)
                    .map(m -> ResponseEntity.ok()
                            .header("X-Menu-Id", m.getId().toString())
                            .body(engine.render(m)))
                    .orElse(ResponseEntity.ok("CON App has no menus"));
        }
        return menuRepo.findById(menuId)
                .map(current -> {
                    if (sanitized.isEmpty()) {
                        return ResponseEntity.ok()
                                .header("X-Menu-Id", current.getId().toString())
                                .body(engine.render(current));
                    }
                    return engine.nextFromInput(current, sanitized)
                            .map(next -> ResponseEntity.ok()
                                    .header("X-Menu-Id", next.getId().toString())
                                    .body(engine.render(next)))
                            .orElse(ResponseEntity.ok("CON Invalid input"));
                })
                .orElse(ResponseEntity.ok("CON Menu not found"));
    }
}


