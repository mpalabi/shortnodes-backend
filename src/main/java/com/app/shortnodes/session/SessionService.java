package com.app.shortnodes.session;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository repository;

    public SessionService(SessionRepository repository) {
        this.repository = repository;
    }

    public Session getOrCreate(UUID id) {
        return repository.findById(id).orElseGet(() -> create(id));
    }

    private Session create(UUID id) {
        Session s = new Session();
        s.setId(id);
        s.setLastActive(Instant.now());
        s.setCurrentNodeId("root");
        return repository.save(s);
    }

    public void touch(Session session) {
        session.setLastActive(Instant.now());
        repository.save(session);
    }
}
