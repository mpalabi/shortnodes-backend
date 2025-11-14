package com.app.shortnodes.app;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "conditions")
public class Condition {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID optionId;

    @Column(nullable = false)
    private String code;

    private UUID invalidTargetMenuId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOptionId() { return optionId; }
    public void setOptionId(UUID optionId) { this.optionId = optionId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public UUID getInvalidTargetMenuId() { return invalidTargetMenuId; }
    public void setInvalidTargetMenuId(UUID invalidTargetMenuId) { this.invalidTargetMenuId = invalidTargetMenuId; }
}


