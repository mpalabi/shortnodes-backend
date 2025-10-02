package com.app.shortnodes.api;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApiConfigRepository extends JpaRepository<ApiConfig, UUID> {
}

