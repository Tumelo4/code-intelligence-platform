package com.codeintel.infrastructure.repository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class JavaNetOutboundHttpClient implements OutboundHttpClient {
    private final HttpClient client;
    private final Duration timeout;

    public JavaNetOutboundHttpClient(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public Response exchange(String method, URI uri, Map<String, String> headers, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(timeout);
        headers.forEach(builder::header);
        builder.method(method, body == null || body.isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = client.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException exception) {
            throw new RepositoryAccessDeniedException("repository access request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RepositoryAccessDeniedException("repository access request interrupted", exception);
        }
    }
}
