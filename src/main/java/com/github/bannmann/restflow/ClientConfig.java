package com.github.bannmann.restflow;

import java.net.http.HttpClient;
import java.time.Duration;

import javax.json.bind.Jsonb;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientConfig
{
    private final @NonNull HttpClient httpClient;

    @With
    private final Duration timeout;

    private final @NonNull Jsonb jsonb;
}
