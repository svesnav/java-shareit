package ru.practicum.shareit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Component
public class BaseClient {
    public static final String USER_HEADER = "X-Sharer-User-Id";

    private final RestTemplate rest;

    public BaseClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        rest = builder.requestFactory(() -> requestFactory)
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .errorHandler(new NoOpResponseErrorHandler())
                .build();
    }

    public ResponseEntity<byte[]> send(HttpMethod method, String path, Long userId, Object body, Object... variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (userId != null) {
            headers.set(USER_HEADER, userId.toString());
        }

        ResponseEntity<byte[]> response = rest.exchange(path, method, new HttpEntity<>(body, headers),
                byte[].class, variables);
        ResponseEntity.BodyBuilder result = ResponseEntity.status(response.getStatusCode());
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null) {
            result.contentType(contentType);
        }
        return result.body(response.getBody());
    }
}
