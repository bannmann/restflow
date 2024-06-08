package dev.bannmann.restflow;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import dev.failsafe.Failsafe;
import dev.failsafe.Policy;

@RequiredArgsConstructor
@Slf4j
abstract class AbstractRequester<B, R> implements Requester<R>
{
    protected final HttpRequest request;
    protected final ClientConfig clientConfig;
    protected final ConcurrentMap<String, Object> diagnosticsData = new ConcurrentHashMap<>();

    private ImmutableList<StackWalker.StackFrame> callerFrames;

    @Override
    public final CompletableFuture<R> start()
    {
        var diagnosticsDataSupplier = clientConfig.getDiagnosticsDataSupplier();
        if (diagnosticsDataSupplier != null)
        {
            Map<String, Object> values = diagnosticsDataSupplier.get();
            diagnosticsData.putAll(values);
        }

        int callerFrameCount = clientConfig.getCallerFrameCount();
        if (callerFrameCount > 0)
        {
            var stackWalker = StackWalker.getInstance(Collections.emptySet(), callerFrameCount);
            callerFrames = stackWalker.walk(stream -> captureCallerFrames(callerFrameCount, stream));
        }

        return send().thenApply(this::extractValue);
    }

    private ImmutableList<StackWalker.StackFrame> captureCallerFrames(int count, Stream<StackWalker.StackFrame> stream)
    {
        return stream.dropWhile(stackFrame -> stackFrame.getClassName()
                .startsWith(getClass().getPackageName()))
            .limit(count)
            .collect(ImmutableList.toImmutableList());
    }

    private CompletableFuture<HttpResponse<B>> send()
    {
        List<Policy<HttpResponse<?>>> policies = clientConfig.getPolicies();
        if (!policies.isEmpty())
        {
            return Failsafe.with(policies)
                .getStageAsync(context -> sendOnce());
        }

        return sendOnce();
    }

    private CompletableFuture<HttpResponse<B>> sendOnce()
    {
        return clientConfig.getHttpClient()
            .sendAsync(request, getBodyHandler())
            .handle(this::addDetailsForLowLevelExceptions)
            .thenApply(this::failOrPassThrough);
    }

    protected abstract HttpResponse.BodyHandler<B> getBodyHandler();

    private <T> T addDetailsForLowLevelExceptions(T result, Throwable throwable)
    {
        if (throwable != null)
        {
            String message = String.format("Request to URL %s failed", request.uri());
            throw new RequestFailureException(request, message, throwable, diagnosticsData, callerFrames);
        }
        return result;
    }

    private HttpResponse<B> failOrPassThrough(HttpResponse<B> response)
    {
        verifyNoErrors(response);
        return response;
    }

    protected abstract void verifyNoErrors(HttpResponse<B> response);

    private R extractValue(HttpResponse<B> httpResponse)
    {
        try
        {
            return doExtractValue(httpResponse);
        }
        catch (RuntimeException e)
        {
            String message = String.format("Could not process response to %s %s:\n%s",
                request.method(),
                request.uri(),
                httpResponse.body());
            throw new ResponseBodyException(message, e, httpResponse, diagnosticsData, callerFrames);
        }
    }

    protected abstract R doExtractValue(HttpResponse<B> response);

    protected boolean isFailure(int responseStatus)
    {
        return !isSuccess(responseStatus);
    }

    protected boolean isSuccess(int responseStatus)
    {
        return responseStatus >= 200 && responseStatus < 300;
    }

    protected <T> ResponseStatusException createException(HttpResponse<T> response)
    {
        int status = response.statusCode();
        String body = getQuotedStringBody(response);
        String message = String.format("Got status %d with message %s for %s %s",
            status,
            body,
            request.method(),
            request.uri());

        return ResponseStatusException.builder()
            .message(message)
            .response(response)
            .diagnosticsData(diagnosticsData)
            .build();
    }

    private <T> String getQuotedStringBody(HttpResponse<T> response)
    {
        T body = response.body();
        if (body == null)
        {
            return null;
        }

        if (body instanceof InputStream)
        {
            try
            {
                byte[] bytes = ByteStreams.toByteArray((InputStream) body);
                return new String(bytes, StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                log.debug("Could not read body stream for error message; returning fallback value", e);
                return "<unreadable stream>";
            }
        }

        return String.format("»%s«",
            body.toString()
                .trim());
    }
}
