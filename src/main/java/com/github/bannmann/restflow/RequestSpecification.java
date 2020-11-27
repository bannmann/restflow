package com.github.bannmann.restflow;

import java.net.http.HttpRequest;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.github.bannmann.restflow.util.HttpRequests;

@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RequestSpecification<B, R>
{
    private final HttpRequest request;

    @Getter
    private final ResponseBodyConfig<B, R> responseBodyConfig;

    @Getter
    private final ClientConfig clientConfig;

    public RequestSpecification<B, R> withCustomizer(RequestCustomizer requestCustomizer)
    {
        ClientConfig newClientConfig = getClientConfig().toBuilder()
            .requestCustomizer(requestCustomizer)
            .build();

        return toBuilder().clientConfig(newClientConfig)
            .build();
    }

    public HttpRequest createFinalRequest()
    {
        HttpRequest.Builder builder = HttpRequests.toBuilder(request);
        for (RequestCustomizer customizer : clientConfig.getRequestCustomizers())
        {
            customizer.customize(builder);
        }
        return builder.build();
    }
}
