package com.github.bannmann.restflow;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.json.bind.Jsonb;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import dev.failsafe.Policy;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientConfig
{
    private final Supplier<Map<String, Object>> diagnosticsDataSupplier;

    private final @NonNull HttpClient httpClient;

    @Singular
    private final List<Policy<HttpResponse<?>>> policies;

    private final @NonNull Jsonb jsonb;

    @Singular
    private final List<RequestCustomizer> requestCustomizers;
}
