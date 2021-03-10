package com.github.bannmann.restflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.bind.JsonbBuilder;

import lombok.extern.slf4j.Slf4j;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.TimeoutExceededException;

@Slf4j
public class TestFailsafeTimeoutingWithLiveUrls extends AbstractNameableTest
{
    private static final int REQUEST_TIMEOUT_MILLIS = 2_000;
    private static final int METHOD_TIMEOUT_MILLIS = (int) (1.5 * REQUEST_TIMEOUT_MILLIS);

    private static final int MAX_RANDOM_DELAY = (int) (REQUEST_TIMEOUT_MILLIS * 1.1);

    private static final int NUMBER_OF_RUNS = 1_000;

    @BeforeMethod
    public void setUp()
    {
    }

    /**
     * 2000 TimeoutExceededException (Failsafe)
     * 2500 SpecialTimeoutException (Custom)
     * 3000 TestNG timeout
     */
    @Test(dataProvider = "liveUrls", timeOut = METHOD_TIMEOUT_MILLIS)
    public void testLiveUrl(String uriString) throws Exception
    {
        URI uri = URI.create(uriString);
        CompletableFuture<String> timeoutingFuture = launch(Timeout.of(Duration.ofMillis(REQUEST_TIMEOUT_MILLIS)), uri);
        try
        {
            timeoutingFuture.get();
        }
        catch (ExecutionException e)
        {
            assertThat(e).getRootCause()
                .isInstanceOf(TimeoutExceededException.class);
            log.info("Timeout occurred for URL {}", uri);
        }
    }

    /**
     * 300-500 random delay
     * 500 TimeoutExceededException (Failsafe)
     * 2500 SpecialTimeoutException (Custom)
     * 3000 TestNG timeout
     */
    @Test(dataProvider = "urlsDelayedRandomlyBy350To500", timeOut = METHOD_TIMEOUT_MILLIS)
    public void testRandomlyDelayedUrl(int runNumber, String uriString) throws Exception
    {
        URI uri = URI.create(uriString);
        CompletableFuture<String> timeoutingFuture = launch(Timeout.of(Duration.ofMillis(500)), uri);
        assertThatThrownBy(timeoutingFuture::get).getRootCause().isInstanceOf(TimeoutExceededException.class);
    }

    private CompletableFuture<String> launch(Timeout<Object> timeoutPolicy, URI uri)
    {
        CompletableFuture<String> originalFuture = makeRequest(uri);

        CompletableFuture<String> timeoutingFuture = Failsafe.with(timeoutPolicy)
            .getStageAsync(() -> originalFuture);
        return timeoutingFuture;
    }

    private CompletableFuture<String> makeRequest(URI uri)
    {
        return makeClient().make(HttpRequest.newBuilder()
            .uri(uri)
            .build())
            .returningString()
            .fetch();
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

    @DataProvider
    public static Iterator<Object[]> liveUrls() throws IOException
    {
        Stream<Object[]> strings = Resources.readLines(Resources.getResource(TestFailsafeTimeoutingWithLiveUrls.class,
            "live-urls.txt"), StandardCharsets.UTF_8)
            .stream()
            .limit(1000)
            .map(s -> new Object[]{ s });
        return strings.iterator();
    }

    @DataProvider
    public static Iterator<Object[]> urlsDelayedRandomlyBy350To500() throws IOException
    {
        int lowerBound = 350;
        int upperBound = 500;

        Random random = new Random();
        return IntStream.range(0, NUMBER_OF_RUNS)
            .mapToObj(runNumber -> new Object[]{
                runNumber,
                String.format(
                    "http://slowwly.robertomurray.co.uk/delay/%d/url/https://jsonplaceholder.typicode.com/posts/%d",
                    lowerBound + random.nextInt(upperBound - lowerBound),
                    runNumber % 100 + 1)
            })
            .iterator();
    }
}
