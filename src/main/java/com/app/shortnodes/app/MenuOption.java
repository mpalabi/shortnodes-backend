package com.app.shortnodes.app;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "menu_options")
public class MenuOption {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID menuId;

    @Column(nullable = false)
    private int keyIndex;

    @Column(nullable = false)
    private String label;

    private UUID targetMenuId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMenuId() { return menuId; }
    public void setMenuId(UUID menuId) { this.menuId = menuId; }
    public int getKeyIndex() { return keyIndex; }
    public void setKeyIndex(int keyIndex) { this.keyIndex = keyIndex; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public UUID getTargetMenuId() { return targetMenuId; }
    public void setTargetMenuId(UUID targetMenuId) { this.targetMenuId = targetMenuId; }
}


