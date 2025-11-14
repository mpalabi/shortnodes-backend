package com.app.shortnodes.app;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MenuOptionRepository extends JpaRepository<MenuOption, UUID> {
    List<MenuOption> findByMenuIdOrderByKeyIndexAsc(UUID menuId);
}


