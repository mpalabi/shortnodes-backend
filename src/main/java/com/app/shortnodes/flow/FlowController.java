package com.app.shortnodes.flow;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flows")
public class FlowController {

    private final FlowRepository repository;

    public FlowController(FlowRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Flow> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Flow> one(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Flow> create(@RequestBody Flow flow) {
        if (flow.getId() == null) {
            flow.setId(UUID.randomUUID());
        }
        Flow saved = repository.save(flow);
        return ResponseEntity.created(URI.create("/api/flows/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Flow> update(@PathVariable UUID id, @RequestBody Flow incoming) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setName(incoming.getName());
                    existing.setJsonDefinition(incoming.getJsonDefinition());
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
}
