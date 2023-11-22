package dev.bannmann.restflow;

import java.net.http.HttpResponse;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
final class ResponseBodyConfig<B, R>
{
    private final HttpResponse.BodyHandler<B> bodyHandler;
    private final Function<B, R> responseConverter;
}
