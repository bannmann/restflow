package com.github.bannmann.restflow;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import com.github.bannmann.restflow.util.Types;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class RequestHandle
{
    private final HttpRequest request;
    private final ClientConfig clientConfig;

    public <T> FetchHandle<T> returning(Class<T> responseClass)
    {
        var responseBodyConfig = new ResponseBodyConfig<>(HttpResponse.BodyHandlers.ofString(),
            s -> clientConfig.getJsonb()
                .fromJson(s, responseClass));
        var spec = new RequestSpecification<>(request, responseBodyConfig, clientConfig);
        return new FetchHandle<>(spec);
    }

    public <T> FetchHandle<T> returning(Type runtimeType)
    {
        var responseBodyConfig = new ResponseBodyConfig<String, T>(HttpResponse.BodyHandlers.ofString(),
            s -> clientConfig.getJsonb()
                .fromJson(s, runtimeType));
        var spec = new RequestSpecification<>(request, responseBodyConfig, clientConfig);
        return new FetchHandle<>(spec);
    }

    public <T> FetchHandle<List<T>> returningListOf(Class<T> elementClass)
    {
        return returning(Types.listOf(elementClass));
    }

    public FetchHandle<JsonObject> returningJsonObject()
    {
        return returning(JsonObject.class);
    }

    public FetchHandle<JsonArray> returningJsonArray()
    {
        return returning(JsonArray.class);
    }

    public FetchHandle<String> returningString()
    {
        var responseBodyConfig = new ResponseBodyConfig<>(HttpResponse.BodyHandlers.ofString(), string -> string);
        var spec = new RequestSpecification<>(request, responseBodyConfig, clientConfig);
        return new FetchHandle<>(spec);
    }

    public FetchHandle<InputStream> returningInputStream()
    {
        var responseBodyConfig = new ResponseBodyConfig<>(HttpResponse.BodyHandlers.ofInputStream(),
            inputStream -> inputStream);
        var spec = new RequestSpecification<>(request, responseBodyConfig, clientConfig);
        return new FetchHandle<>(spec);
    }

    public ExecuteHandle returningNothing()
    {
        // Note: we use ofString() handler because discarding() would also discard the body of an error response.
        var responseBodyConfig = new ResponseBodyConfig<String, Void>(HttpResponse.BodyHandlers.ofString(), v -> null);
        var spec = new RequestSpecification<>(request, responseBodyConfig, clientConfig);
        return new ExecuteHandle(spec);
    }
}
