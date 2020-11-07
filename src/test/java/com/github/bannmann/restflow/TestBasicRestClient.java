package com.github.bannmann.restflow;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.json.bind.JsonbBuilder;

import org.mockserver.integration.ClientAndServer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.github.mizool.core.exception.CodeInconsistencyException;

public class TestBasicRestClient
{
    private static final int TIMEOUT = 2000;
    private static final int PORT = 1080;
    private static final URL BASE_URL = makeUrl("http://localhost:" + PORT);

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

    private static URI pathToFullUri(String path)
    {
        try
        {
            URI result = new URL(BASE_URL, path).toURI();
            return result;
        }
        catch (URISyntaxException | MalformedURLException e)
        {
            throw new CodeInconsistencyException(e);
        }
    }

    private static HttpRequest.Builder createRequest(String path)
    {
        return HttpRequest.newBuilder()
            .uri(pathToFullUri(path));
    }

    private final ClientAndServer mockedServer = new ClientAndServer(PORT);

    @BeforeMethod
    public void setUp()
    {
        mockedServer.reset();
    }

    @Test(timeOut = TIMEOUT)
    public void testExecute() throws Exception
    {
        BasicRestClient client = makeClient();

        mockedServer.when(request("/foo").withMethod("POST"))
            .respond(response().withStatusCode(204));

        var request = createRequest("/foo").POST(noBody())
            .build();

        client.make(request)
            .execute()
            .get();

        mockedServer.verify(request().withPath("/foo"));
    }

    @Test(timeOut = 1000)
    public void testTimeoutKept() throws Exception
    {
        ClientConfig clientConfig = makeClientConfig().withTimeout(Duration.ofMillis(200));
        BasicRestClient client = makeClient(clientConfig);

        mockedServer.when(request("/foo").withMethod("POST"))
            .respond(response().withDelay(TimeUnit.MILLISECONDS, 100)
                .withStatusCode(204));

        var request = createRequest("/foo").POST(noBody())
            .build();

        client.make(request)
            .execute()
            .get();

        mockedServer.verify(request().withPath("/foo"));
    }

    @Test(timeOut = 1000)
    public void testTimeoutExceeded()
    {
        ClientConfig clientConfig = makeClientConfig().withTimeout(Duration.ofMillis(200));
        BasicRestClient client = makeClient(clientConfig);

        mockedServer.when(request("/foo").withMethod("POST"))
            .respond(response().withDelay(TimeUnit.SECONDS, 5)
                .withStatusCode(204));

        var request = createRequest("/foo").POST(noBody())
            .build();

        var future = client.make(request)
            .execute();

        assertThatThrownBy(future::get).hasRootCauseInstanceOf(TimeoutException.class);

        mockedServer.verify(request().withPath("/foo"));
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
                .build())
            .jsonb(JsonbBuilder.create())
            .build();
    }
}
