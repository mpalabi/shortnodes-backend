package com.app.shortnodes.flow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlowRepository extends JpaRepository<Flow, UUID> {
}

