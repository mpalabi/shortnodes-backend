package com.app.shortnodes.flow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "flows")
public class Flow {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(nullable = false)
    private String jsonDefinition;

    public Flow() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJsonDefinition() {
        return jsonDefinition;
    }

    public void setJsonDefinition(String jsonDefinition) {
        this.jsonDefinition = jsonDefinition;
    }
}

