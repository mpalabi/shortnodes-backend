package com.app.shortnodes.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-configs")
public class ApiConfigController {

    private final ApiConfigRepository repository;
    private final ApiService apiService;

    public ApiConfigController(ApiConfigRepository repository, ApiService apiService) {
        this.repository = repository;
        this.apiService = apiService;
    }

    @GetMapping
    public List<ApiConfig> all() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<ApiConfig> one(@PathVariable UUID id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiConfig> create(@RequestBody ApiConfig cfg) {
        if (cfg.getId() == null) cfg.setId(UUID.randomUUID());
        ApiConfig saved = repository.save(cfg);
        return ResponseEntity.created(URI.create("/api/api-configs/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiConfig> update(@PathVariable UUID id, @RequestBody ApiConfig incoming) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setName(incoming.getName());
                    existing.setMethod(incoming.getMethod());
                    existing.setUrl(incoming.getUrl());
                    existing.setHeaders(incoming.getHeaders());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElseGet(() -> {
                    incoming.setId(id);
                    return ResponseEntity.ok(repository.save(incoming));
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<String> execute(@PathVariable UUID id, @RequestBody(required = false) Map<String, Object> payload) {
        return repository.findById(id)
                .map(cfg -> apiService.execute(cfg, payload == null ? null : String.valueOf(payload.get("body"))))
                .orElse(ResponseEntity.notFound().build());
    }
}
