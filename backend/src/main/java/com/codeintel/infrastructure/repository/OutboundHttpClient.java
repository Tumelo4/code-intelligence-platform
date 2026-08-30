package com.codeintel.infrastructure.repository;

import java.net.URI;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface OutboundHttpClient {
    Response exchange(String method, URI uri, Map<String, String> headers, String body);

    record Response(int statusCode, Map<String, List<String>> headers, String body) {
        public String firstHeader(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .flatMap(entry -> entry.getValue().stream())
                    .findFirst().orElse("");
        }
    }
}
