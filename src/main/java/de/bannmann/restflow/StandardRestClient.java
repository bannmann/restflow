package de.bannmann.restflow;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import javax.json.JsonValue;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public final class StandardRestClient extends AbstractRestClient
{
    private static final String APPLICATION_JSON = "application/json";

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public final class RequestBodyHandle
    {
        private final String method;
        private final String contentType;
        private final HttpRequest.BodyPublisher bodyPublisher;

        public RequestHandle to(@NonNull String resourcePath)
        {
            HttpRequest request = createUploadRequest(resourcePath);
            return make(request);
        }

        private HttpRequest createUploadRequest(String resourcePath)
        {
            return newRequestBuilder(resourcePath).header("Content-Type", contentType)
                .method(method, bodyPublisher)
                .build();
        }
    }

    private final HttpRequest.Builder requestTemplate;
    private final URL baseUrl;

    /**
     * @throws IllegalStateException if no URI has been set on {@code requestTemplate}
     */
    @Builder
    protected StandardRestClient(@NonNull ClientConfig clientConfig, @NonNull HttpRequest.Builder requestTemplate)
    {
        super(clientConfig);

        this.requestTemplate = requestTemplate.copy();

        baseUrl = obtainBaseUrl(requestTemplate);
    }

    /**
     * @throws IllegalStateException if no URI has been set on {@code requestTemplate}
     */
    private URL obtainBaseUrl(HttpRequest.Builder requestTemplate)
    {
        try
        {
            String spec = requestTemplate.build()
                .uri()
                .toString();

            if (!spec.endsWith("/"))
            {
                // Ensure the last path segment is not overwritten when building URLs based on this base URL
                spec = spec + "/";
            }

            return new URL(spec);
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public RequestHandle get(@NonNull String resourcePath)
    {
        HttpRequest request = createGetRequest(resourcePath);
        return make(request);
    }

    private RequestHandle make(HttpRequest request)
    {
        return new RequestHandle(request);
    }

    private HttpRequest createGetRequest(String resourcePath)
    {
        return newRequestBuilder(resourcePath).build();
    }

    private HttpRequest.Builder newRequestBuilder(String resourcePath)
    {
        URI uri = createUri(resourcePath);

        return requestTemplate.copy()
            .uri(uri);
    }

    private URI createUri(String resourcePath)
    {
        try
        {
            return new URL(baseUrl, resourcePath).toURI();
        }
        catch (URISyntaxException | MalformedURLException e)
        {
            throw new IllegalArgumentException(e);
        }
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
        return new RequestBodyHandle("POST", contentType, getStringBodyPublisher(body));
    }

    private HttpRequest.BodyPublisher getStringBodyPublisher(@NonNull String body)
    {
        return HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
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
        return new RequestBodyHandle("PUT", contentType, getStringBodyPublisher(body));
    }
}
