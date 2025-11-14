package com.app.shortnodes.app;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppRepository extends JpaRepository<App, UUID> {
}


