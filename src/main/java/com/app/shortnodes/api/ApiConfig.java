package com.app.shortnodes.api;

import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "api_configs")
public class ApiConfig {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String method; // GET or POST

    @Column(nullable = false)
    private String url;

    @ElementCollection
    @CollectionTable(name = "api_config_headers", joinColumns = @JoinColumn(name = "api_config_id"))
    @MapKeyColumn(name = "header_key")
    @Column(name = "header_value")
    private Map<String, String> headers;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
}

