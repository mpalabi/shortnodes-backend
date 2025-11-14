package com.app.shortnodes.app;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "menus")
public class Menu {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID appId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean isEntry = false;

    // Optional grouping/subflow label
    private String subflowId; // UUID string of virtual subflow grouping (front-end defined)
    
    // Node position fields
    @Column(nullable = true)
    private Double positionX;
    
    @Column(nullable = true)
    private Double positionY;
    
    // Menu mode: 'menu' for options, 'input' for input prompt only
    @Column(nullable = false)
    private String mode = "menu";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAppId() { return appId; }
    public void setAppId(UUID appId) { this.appId = appId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isEntry() { return isEntry; }
    public void setEntry(boolean entry) { isEntry = entry; }
    public String getSubflowId() { return subflowId; }
    public void setSubflowId(String subflowId) { this.subflowId = subflowId; }
    
    // Position getters and setters
    public Double getPositionX() { return positionX; }
    public void setPositionX(Double positionX) { this.positionX = positionX; }
    public Double getPositionY() { return positionY; }
    public void setPositionY(Double positionY) { this.positionY = positionY; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}


