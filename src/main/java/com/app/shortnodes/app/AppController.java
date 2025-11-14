package com.app.shortnodes.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/apps")
public class AppController {
    private final AppRepository repo;

    public AppController(AppRepository repo) { this.repo = repo; }

    @GetMapping
    public List<App> all() { return repo.findAll(); }

    @PostMapping
    public ResponseEntity<App> create(@RequestBody App app) {
        if (app.getId() == null) app.setId(UUID.randomUUID());
        App saved = repo.save(app);
        return ResponseEntity.created(URI.create("/api/apps/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<App> get(@PathVariable UUID id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}


