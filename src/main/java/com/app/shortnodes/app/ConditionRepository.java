package com.app.shortnodes.app;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConditionRepository extends JpaRepository<Condition, UUID> {
    Optional<Condition> findByOptionId(UUID optionId);
}


