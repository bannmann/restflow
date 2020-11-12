package com.github.bannmann.restflow;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.json.bind.JsonbBuilder;

import lombok.extern.slf4j.Slf4j;

import org.mockserver.integration.ClientAndServer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.github.mizool.core.exception.CodeInconsistencyException;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.TimeoutExceededException;

@Slf4j
public class TestBasicRestClient
{
    private static final int METHOD_TIMEOUT = 30 * 1000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(200);

    private static final int PORT = 1080;
    private static final URL BASE_URL = makeUrl("http://localhost:" + PORT);

    public static final String PATH = "/foo";

    private static final HttpRequest OUTGOING_POST_REQUEST = createRequest(PATH).POST(noBody())
        .build();

    private static final org.mockserver.model.HttpRequest POST_REQUEST = request(PATH).withMethod("POST");
    private static final org.mockserver.model.HttpResponse RESPONSE_NO_CONTENT = response().withStatusCode(204);
    private static final org.mockserver.model.HttpResponse SERVER_BUSY_RESPONSE = response().withStatusCode(429)
        .withBody("Please try again later")
        .withDelay(TimeUnit.SECONDS, 5);

    private static final RetryPolicy<HttpResponse<?>>
        RETRY_ONCE_POLICY
        = new RetryPolicy<HttpResponse<?>>().withMaxRetries(1);
    private static final Timeout<HttpResponse<?>> TIMEOUT_POLICY = Timeout.of(REQUEST_TIMEOUT);

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
                .build())
            .jsonb(JsonbBuilder.create())
            .build();
    }

    private static URL makeUrl(String spec)
    {
        try
        {
            return new URI(spec).toURL();
        }
        catch (URISyntaxException | MalformedURLException e)
        {
            throw new CodeInconsistencyException(e);
        }
    }

    private static URI makeUriFromPath(String path)
    {
        try
        {
            return new URL(BASE_URL, path).toURI();
        }
        catch (URISyntaxException | MalformedURLException e)
        {
            throw new CodeInconsistencyException(e);
        }
    }

    private static HttpRequest.Builder createRequest(String path)
    {
        return HttpRequest.newBuilder()
            .uri(makeUriFromPath(path));
    }

    private final ClientAndServer mockedServer = new ClientAndServer(PORT);

    @BeforeMethod
    public void setUp()
    {
        mockedServer.reset();
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testExecute() throws Exception
    {
        mockedServer.when(POST_REQUEST)
            .respond(RESPONSE_NO_CONTENT);

        BasicRestClient client = makeClient();
        client.make(OUTGOING_POST_REQUEST)
            .execute()
            .get();

        mockedServer.verify(POST_REQUEST);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testTimeoutKept() throws Exception
    {
        mockedServer.when(POST_REQUEST)
            .respond(RESPONSE_NO_CONTENT.withDelay(TimeUnit.MILLISECONDS, 100));

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(TIMEOUT_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(OUTGOING_POST_REQUEST)
            .execute()
            .get();

        mockedServer.verify(POST_REQUEST);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testTimeoutExceeded()
    {
        mockedServer.when(POST_REQUEST)
            .respond(SERVER_BUSY_RESPONSE);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(TIMEOUT_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        var future = client.make(OUTGOING_POST_REQUEST)
            .execute();

        assertThatThrownBy(future::get).hasRootCauseInstanceOf(TimeoutExceededException.class);

        mockedServer.verify(POST_REQUEST);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testRetryOnTimeout() throws Exception
    {
        mockedServer.when(POST_REQUEST, once())
            .respond(SERVER_BUSY_RESPONSE);
        mockedServer.when(POST_REQUEST, once())
            .respond(RESPONSE_NO_CONTENT);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(RETRY_ONCE_POLICY)
            .policy(TIMEOUT_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(OUTGOING_POST_REQUEST)
            .execute()
            .get();

        mockedServer.verify(POST_REQUEST, exactly(2));
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testRetryOnError() throws Exception
    {
        mockedServer.when(POST_REQUEST, once())
            .respond(response().withStatusCode(500)
                .withBody("Detected a slight field variance in the thera-magnetic caesium portal housing."));

        mockedServer.when(POST_REQUEST, once())
            .respond(RESPONSE_NO_CONTENT);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(RETRY_ONCE_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(OUTGOING_POST_REQUEST)
            .execute()
            .get();

        mockedServer.verify(POST_REQUEST, exactly(2));
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testExceptionDetails()
    {
        mockedServer.when(POST_REQUEST)
            .respond(response().withStatusCode(418)
                .withBody("Incompatible equipment."));

        ClientConfig clientConfig = makeClientConfig();
        BasicRestClient client = makeClient(clientConfig);

        var executeFuture = client.make(OUTGOING_POST_REQUEST)
            .execute();
        var fetchFuture = client.make(OUTGOING_POST_REQUEST)
            .returningString()
            .fetch();
        var tryFetchFuture = client.make(OUTGOING_POST_REQUEST)
            .returningString()
            .tryFetch();

        String expectedMessage = "Got status 418 with message 'Incompatible equipment.' for URL " + BASE_URL + PATH;
        assertThatThrownBy(executeFuture::get).hasRootCauseMessage(expectedMessage);
        assertThatThrownBy(fetchFuture::get).hasRootCauseMessage(expectedMessage);
        assertThatThrownBy(tryFetchFuture::get).hasRootCauseMessage(expectedMessage);
    }
}
