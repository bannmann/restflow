package com.github.bannmann.restflow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import javax.json.bind.JsonbBuilder;

import lombok.extern.slf4j.Slf4j;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.TimeoutExceededException;

@Slf4j
public class Test2FailsafeTimeoutingWithRemoteServer extends AbstractNameableTest
{
    private static final int REQUEST_TIMEOUT_MILLIS = 500;
    private static final int METHOD_TIMEOUT_MILLIS = (int) (1.5 * REQUEST_TIMEOUT_MILLIS);
    private static final int MAX_RANDOM_DELAY = REQUEST_TIMEOUT_MILLIS * 2;

    private static final Timeout<Object> TIMEOUT_POLICY = Timeout.of(Duration.ofMillis(REQUEST_TIMEOUT_MILLIS));
    public static final int NUMBER_OF_RUNS = 20;

    @DataProvider
    public static Iterator<Object[]> intermittentTimeouts()
    {
        Random random = new Random();
        return IntStream.range(0, NUMBER_OF_RUNS)
            .mapToObj(value -> new Object[]{ value, random.nextInt(MAX_RANDOM_DELAY) })
            .iterator();
    }

    @Test(dataProvider = "intermittentTimeouts", timeOut = METHOD_TIMEOUT_MILLIS)
    public void testViaFailsafe(int number, int millis) throws Exception
    {
        CompletableFuture<Void> future = getFutureDelayedByMillis(millis);

        try
        {
            Failsafe.with(TIMEOUT_POLICY)
                .getStageAsync(() -> future)
                .get();
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof TimeoutExceededException)
            {
                log.info("run {}: timeout exceeded", number, e);
            }
            else
            {
                throw e;
            }
        }
    }

    @Test(dataProvider = "intermittentTimeouts", timeOut = METHOD_TIMEOUT_MILLIS)
    public void testViaFutureGet(int number, int millis) throws Exception
    {
        CompletableFuture<Void> future = getFutureDelayedByMillis(millis);

        try
        {
            future.get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e)
        {
            log.info("run {}: timeout exceeded", number, e);
        }
    }

    private CompletableFuture<Void> getFutureDelayedByMillis(int millis)
    {
        URI uri = URI.create(String.format(
            "http://slowwly.robertomurray.co.uk/delay/%d/url/https://jsonplaceholder.typicode.com/posts/1",
            millis));

        return makeClient().make(HttpRequest.newBuilder()
            .uri(uri)
            .build())
            .returningNothing()
            .execute();
    }

    private BasicRestClient makeClient()
    {
        return makeClient(makeClientConfig());
    }

    private BasicRestClient makeClient(ClientConfig clientConfig)
    {
        return BasicRestClient.builder()
            .clientConfig(clientConfig)
            .build();
    }

    private ClientConfig makeClientConfig()
    {
        return ClientConfig.builder()
            .httpClient(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build())
            .jsonb(JsonbBuilder.create())
            .build();
    }
}
