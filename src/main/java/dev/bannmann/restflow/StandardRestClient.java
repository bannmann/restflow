package dev.bannmann.restflow;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import javax.json.JsonMergePatch;
import javax.json.JsonPatch;
import javax.json.JsonValue;

import lombok.Builder;
import lombok.NonNull;

public final class StandardRestClient
{
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_JSON_PATCH_JSON = "application/json-patch+json";
    private static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

    private final ClientConfig clientConfig;
    private final RequestTemplate requestTemplate;

    /**
     * @throws IllegalStateException if no URI has been set on {@code requestTemplate}
     */
    @Builder
    private StandardRestClient(@NonNull ClientConfig clientConfig, @NonNull HttpRequest.Builder requestTemplate)
    {
        this.clientConfig = clientConfig;
        this.requestTemplate = new RequestTemplate(requestTemplate);
    }

    public RequestHandle get(@NonNull String resourcePath)
    {
        HttpRequest request = requestTemplate.newBuilder(resourcePath)
            .build();
        return new RequestHandle(request, clientConfig);
    }

    public RequestBodyHandle post(@NonNull Object body)
    {
        return post(clientConfig.getJsonb()
            .toJson(body), APPLICATION_JSON);
    }

    public RequestBodyHandle post(@NonNull JsonValue body)
    {
        return post(body.toString(), APPLICATION_JSON);
    }

    public RequestBodyHandle post(@NonNull String body, @NonNull String contentType)
    {
        return new RequestBodyHandle(clientConfig, requestTemplate, "POST", contentType, getBodyPublisher(body));
    }

    public RequestBodyHandle put(@NonNull Object body)
    {
        return put(clientConfig.getJsonb()
            .toJson(body), APPLICATION_JSON);
    }

    public RequestBodyHandle put(@NonNull JsonValue body)
    {
        return put(body.toString(), APPLICATION_JSON);
    }

    public RequestBodyHandle put(@NonNull String body, @NonNull String contentType)
    {
        return new RequestBodyHandle(clientConfig, requestTemplate, "PUT", contentType, getBodyPublisher(body));
    }

    public RequestHandle delete(@NonNull String resourcePath)
    {
        HttpRequest request = requestTemplate.newBuilder(resourcePath)
            .DELETE()
            .build();
        return new RequestHandle(request, clientConfig);
    }

    public RequestBodyHandle patch(@NonNull JsonPatch patch)
    {
        return new RequestBodyHandle(clientConfig,
            requestTemplate,
            "PATCH",
            APPLICATION_JSON_PATCH_JSON,
            getBodyPublisher(patch.toJsonArray()));
    }

    public RequestBodyHandle patch(@NonNull JsonMergePatch patch)
    {
        return new RequestBodyHandle(clientConfig,
            requestTemplate,
            "PATCH",
            APPLICATION_MERGE_PATCH_JSON,
            getBodyPublisher(patch.toJsonValue()));
    }

    public RequestBodyHandle patch(@NonNull Object body)
    {
        return patch(clientConfig.getJsonb()
            .toJson(body), APPLICATION_JSON);
    }

    public RequestBodyHandle patch(@NonNull JsonValue body)
    {
        return patch(body.toString(), APPLICATION_JSON);
    }

    public RequestBodyHandle patch(@NonNull String body, @NonNull String contentType)
    {
        return new RequestBodyHandle(clientConfig, requestTemplate, "PATCH", contentType, getBodyPublisher(body));
    }

    private HttpRequest.BodyPublisher getBodyPublisher(@NonNull JsonValue body)
    {
        return getBodyPublisher(body.toString());
    }

    private HttpRequest.BodyPublisher getBodyPublisher(@NonNull String body)
    {
        return HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
    }
}
