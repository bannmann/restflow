package com.github.bannmann.restflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import javax.json.bind.JsonbBuilder;

import lombok.extern.slf4j.Slf4j;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.TimeoutExceededException;

@Slf4j
public class TestFailsafeTimeoutingWithRemoteServer extends AbstractNameableTest
{
    private static final int REQUEST_TIMEOUT_MILLIS = 500;
    private static final int METHOD_TIMEOUT_MILLIS = (int) (1.5 * REQUEST_TIMEOUT_MILLIS);
    private static final Timeout<Object> TIMEOUT_POLICY = Timeout.of(Duration.ofMillis(REQUEST_TIMEOUT_MILLIS));

    @BeforeMethod
    public void setUp()
    {
    }

    @Test()
    public void testFoo() throws Exception
    {
        CompletableFuture<String> slowFuture = getFutureDelayedByMillis(REQUEST_TIMEOUT_MILLIS / 2);

        String result = Failsafe.with(TIMEOUT_POLICY)
            .getStageAsync(() -> slowFuture)
            .get();
        System.out.println(result);

        //assertThatCode(future::get).doesNotThrowAnyException();
    }

    @Test(timeOut = METHOD_TIMEOUT_MILLIS)
    public void testTimeoutKept() throws Exception
    {
        CompletableFuture<String> slowFuture = getFutureDelayedByMillis(REQUEST_TIMEOUT_MILLIS / 2);

        String result = Failsafe.with(TIMEOUT_POLICY)
            .getStageAsync(() -> slowFuture)
            .get();

        //assertThatCode(future::get).doesNotThrowAnyException();
    }

    @Test(timeOut = METHOD_TIMEOUT_MILLIS)
    public void testTimeoutExceeded()
    {
        CompletableFuture<String> slowFuture = getFutureDelayedByMillis(REQUEST_TIMEOUT_MILLIS * 2);

        CompletableFuture<String> future = Failsafe.with(TIMEOUT_POLICY)
            .getStageAsync(() -> slowFuture);

        assertThatThrownBy(future::get).hasRootCauseInstanceOf(TimeoutExceededException.class);
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

    private CompletableFuture<String> getFutureDelayedByMillis(int millis)
    {
        URI uri = URI.create(String.format(
            "http://slowwly.robertomurray.co.uk/delay/%d/url/https://jsonplaceholder.typicode.com/posts/1",
            millis));

        return makeClient().make(HttpRequest.newBuilder()
            .uri(uri)
            .build())
            .returningString()
            .fetch();
    }

    @Test(dataProvider = "intermittentTimeouts", timeOut = METHOD_TIMEOUT_MILLIS)
    public void testIntermittentTimeouts(int number, boolean exceed) throws Exception
    {
        if (exceed)
        {
            testTimeoutExceeded();
        }
        else
        {
            testTimeoutKept();
        }
    }

    @DataProvider
    public static Iterator<Object[]> intermittentTimeouts()
    {
        final int numberOfRuns = 50;

        Random random = new Random();
        return IntStream.range(0, numberOfRuns)
            .mapToObj(value -> new Object[]{ value, random.nextInt(101) > 90 })
            .iterator();
    }
}
