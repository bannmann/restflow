package dev.bannmann.restflow;

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

    /**
     * The number of caller stack frames to capture when starting a request. The captured frames will be included in
     * any {@link RequestException} (or subclass) instance thrown by restflow. This is useful if the application is
     * supposed to log intermediate exceptions, which will otherwise not carry any stack information relating to the
     * original thread (due to neither being thrown there nor being set as the cause of an exception thrown in that
     * thread).
     */
    @Builder.Default
    private final int callerFrameCount = 5;
}
