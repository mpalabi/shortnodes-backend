package com.app.shortnodes.api;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class ApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> execute(ApiConfig cfg, String body) {
        HttpHeaders headers = new HttpHeaders();
        if (cfg.getHeaders() != null) {
            cfg.getHeaders().forEach(headers::add);
        }

        boolean isPost = "POST".equalsIgnoreCase(cfg.getMethod());
        HttpEntity<String> entity = isPost ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
        HttpMethod method = isPost ? HttpMethod.POST : HttpMethod.GET;

        try {
            ResponseEntity<String> response = restTemplate.exchange(cfg.getUrl(), method, entity, String.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            return ResponseEntity.status(504).body("Upstream timeout or network error");
        } catch (Exception ex) {
            return ResponseEntity.status(502).body("Upstream call failed");
        }
    }
}
