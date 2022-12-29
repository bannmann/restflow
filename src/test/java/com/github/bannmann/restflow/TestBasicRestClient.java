package com.github.bannmann.restflow;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import java.io.StringReader;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.json.Json;
import javax.json.bind.JsonbBuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockserver.integration.ClientAndServer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.TimeoutExceededException;

@Slf4j
public class TestBasicRestClient extends AbstractNameableTest
{
    private interface ReturnSpec<T> extends Function<RequestHandle, FetchHandle<T>>
    {
    }

    private static final int METHOD_TIMEOUT = 5 * 1000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(100);

    private static final Timeout<HttpResponse<?>> TIMEOUT_POLICY = Timeout.of(REQUEST_TIMEOUT);
    private static final RetryPolicy<HttpResponse<?>>
        RETRY_ONCE_POLICY
        = new RetryPolicy<HttpResponse<?>>().withMaxRetries(1);

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

    private final ClientAndServer mockedServer = new ClientAndServer(TestData.PORT);

    @BeforeMethod
    public void setUp()
    {
        mockedServer.reset();
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testExecute() throws Exception
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(TestData.Responses.NO_CONTENT);

        BasicRestClient client = makeClient();
        client.make(TestData.Requests.Outgoing.POST)
            .returningNothing()
            .execute()
            .get();

        mockedServer.verify(TestData.Requests.Incoming.POST);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testExecuteNowhere()
    {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(TestData.FAKE_SERVER_URL.toUri())
            .build();
        CompletableFuture<Void> responseFuture = makeClient().make(request)
            .returningNothing()
            .execute();

        assertThatThrownBy(responseFuture::get).isExactlyInstanceOf(ExecutionException.class)
            .extracting(Throwable::getCause, as(InstanceOfAssertFactories.THROWABLE))
            .isExactlyInstanceOf(RequestFailureException.class)
            .hasMessageContaining(TestData.FAKE_SERVER_URL.toString())
            .hasRootCauseExactlyInstanceOf(ConnectException.class);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testExecuteMissing()
    {
        CompletableFuture<Void> responseFuture = makeClient().make(TestData.Requests.Outgoing.POST_MISSING)
            .returningNothing()
            .execute();

        assertThrowsRequestStatusException(responseFuture, 404, TestData.Strings.PATH_MISSING, "", "POST");
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testFetchMissing()
    {
        CompletableFuture<String> responseFuture = makeClient().make(TestData.Requests.Outgoing.POST_MISSING)
            .returningString()
            .fetch();

        assertThrowsRequestStatusException(responseFuture, 404, TestData.Strings.PATH_MISSING, "", "POST");
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testTryFetchMissing() throws Exception
    {
        Optional<String> fakeRequestResponse = makeClient().make(TestData.Requests.Outgoing.POST_MISSING)
            .returningString()
            .tryFetch()
            .get();

        assertThat(fakeRequestResponse).isEmpty();
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testExecuteServerError()
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(TestData.Responses.INTERNAL_SERVER_ERROR);

        CompletableFuture<Void> responseFuture = makeClient().make(TestData.Requests.Outgoing.POST)
            .returningNothing()
            .execute();

        assertThrowsInternalServerError(responseFuture, "POST");
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testFetchServerError()
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(TestData.Responses.INTERNAL_SERVER_ERROR);

        CompletableFuture<String> responseFuture = makeClient().make(TestData.Requests.Outgoing.POST)
            .returningString()
            .fetch();

        assertThrowsInternalServerError(responseFuture, "POST");
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testTryFetchServerError()
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(TestData.Responses.INTERNAL_SERVER_ERROR);

        CompletableFuture<Optional<String>> responseFuture = makeClient().make(TestData.Requests.Outgoing.POST)
            .returningString()
            .tryFetch();

        assertThrowsInternalServerError(responseFuture, "POST");
    }

    private void assertThrowsInternalServerError(CompletableFuture<?> responseFuture, String method)
    {
        assertThrowsRequestStatusException(responseFuture,
            500,
            TestData.Strings.PATH,
            TestData.Responses.Body.INTERNAL_SERVER_ERROR_BODY,
            method);
    }

    private void assertThrowsRequestStatusException(
        CompletableFuture<?> responseFuture, int status, String path, String body, String method)
    {
        String message = String.format("Got status %d with message '%s' for %s %s%s",
            status,
            body,
            method,
            TestData.BASE_URL,
            path);
        RequestStatusException expectedCause = RequestStatusException.builder()
            .message(message)
            .build();

        assertThatThrownBy(responseFuture::get).isExactlyInstanceOf(ExecutionException.class)
            .hasCause(expectedCause);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Greeting
    {
        private String greeting;
    }

    @DataProvider
    public Object[][] getFetchTypeData()
    {
        return new Object[][]{
            makeFetchDataParameters(TestData.Responses.HELLO_WORLD_OBJECT,
                RequestHandle::returningString,
                TestData.Responses.Body.HELLO_WORLD_OBJECT,
                "String"),

            makeFetchDataParameters(TestData.Responses.HELLO_WORLD_OBJECT,
                RequestHandle::returningJsonObject,
                Json.createReader(new StringReader(TestData.Responses.Body.HELLO_WORLD_OBJECT))
                    .readObject(),
                "JsonObject"),

            makeFetchDataParameters(TestData.Responses.HELLO_WORLD_ARRAY,
                RequestHandle::returningJsonArray,
                Json.createReader(new StringReader(TestData.Responses.Body.HELLO_WORLD_JSON_ARRAY))
                    .readArray(),
                "JsonArray"),

            makeFetchDataParameters(TestData.Responses.HELLO_WORLD_OBJECT,
                handle -> handle.returning(Greeting.class),
                new Greeting("Hello, world!"),
                "Greeting"),

            makeFetchDataParameters(TestData.Responses.HELLO_WORLD_ARRAY,
                handle -> handle.returningListOf(Greeting.class),
                List.of(new Greeting("Hello, world!")),
                "List of Greeting")
        };
    }

    /**
     * Enables type inference on data provider rows.
     */
    private <T> Object[] makeFetchDataParameters(
        org.mockserver.model.HttpResponse mockResponse, ReturnSpec<T> returnSpec, T expectedResult, String remark)
    {
        return new Object[]{ mockResponse, returnSpec, expectedResult, remark };
    }

    @Test(timeOut = METHOD_TIMEOUT, dataProvider = "getFetchTypeData")
    public <T> void testFetch(
        org.mockserver.model.HttpResponse mockResponse,
        ReturnSpec<T> returnSpec,
        T expectedResult,
        @UseAsTestName @SuppressWarnings("unused") String remark) throws Exception
    {
        T response = prepareFetchClientServer(mockResponse, returnSpec).fetch()
            .get();

        assertThat(response).isEqualTo(expectedResult);
    }

    @Test(timeOut = METHOD_TIMEOUT, dataProvider = "getFetchTypeData")
    public <T> void testTryFetch(
        org.mockserver.model.HttpResponse mockResponse,
        ReturnSpec<T> returnSpec,
        T expectedResult,
        @UseAsTestName @SuppressWarnings("unused") String remark) throws Exception
    {
        Optional<T> response = prepareFetchClientServer(mockResponse, returnSpec).tryFetch()
            .get();

        assertThat(response).contains(expectedResult);
    }

    private <T> FetchHandle<T> prepareFetchClientServer(
        org.mockserver.model.HttpResponse mockResponse, ReturnSpec<T> returnSpec)
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(mockResponse);

        BasicRestClient client = makeClient();
        var requestHandle = client.make(TestData.Requests.Outgoing.POST);
        return returnSpec.apply(requestHandle);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testTimeoutKept() throws Exception
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(TestData.Responses.HELLO_WORLD_OBJECT.clone()
                .withDelay(TimeUnit.MILLISECONDS, 50));

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(TIMEOUT_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(TestData.Requests.Outgoing.POST)
            .returning(Greeting.class)
            .fetch()
            .get();

        mockedServer.verify(TestData.Requests.Incoming.POST);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testTimeoutExceeded()
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(TestData.Responses.SERVER_BUSY);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(TIMEOUT_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        var future = client.make(TestData.Requests.Outgoing.POST)
            .returning(Greeting.class)
            .fetch();

        assertThatThrownBy(future::get).hasRootCauseInstanceOf(TimeoutExceededException.class);

        mockedServer.verify(TestData.Requests.Incoming.POST);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testRetryOnTimeout() throws Exception
    {
        mockedServer.when(TestData.Requests.Incoming.POST, once())
            .respond(TestData.Responses.SERVER_BUSY);
        mockedServer.when(TestData.Requests.Incoming.POST, once())
            .respond(TestData.Responses.HELLO_WORLD_OBJECT);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(RETRY_ONCE_POLICY)
            .policy(TIMEOUT_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(TestData.Requests.Outgoing.POST)
            .returning(Greeting.class)
            .fetch()
            .get();

        mockedServer.verify(TestData.Requests.Incoming.POST, exactly(2));
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testRetryOnError() throws Exception
    {
        mockedServer.when(TestData.Requests.Incoming.POST, once())
            .respond(TestData.Responses.INTERNAL_SERVER_ERROR);

        mockedServer.when(TestData.Requests.Incoming.POST, once())
            .respond(TestData.Responses.HELLO_WORLD_OBJECT);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .policy(RETRY_ONCE_POLICY)
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(TestData.Requests.Outgoing.POST)
            .returning(Greeting.class)
            .fetch()
            .get();

        mockedServer.verify(TestData.Requests.Incoming.POST, exactly(2));
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testExceptionDetails()
    {
        mockedServer.when(TestData.Requests.Incoming.POST)
            .respond(response().withStatusCode(418)
                .withBody("Incompatible equipment."));

        ClientConfig clientConfig = makeClientConfig();
        BasicRestClient client = makeClient(clientConfig);

        var executeFuture = client.make(TestData.Requests.Outgoing.POST)
            .returningNothing()
            .execute();
        var fetchFuture = client.make(TestData.Requests.Outgoing.POST)
            .returningString()
            .fetch();
        var tryFetchFuture = client.make(TestData.Requests.Outgoing.POST)
            .returningString()
            .tryFetch();

        String expectedMessage = "Got status 418 with message 'Incompatible equipment.' for POST " +
            TestData.BASE_URL +
            TestData.Strings.PATH;
        assertThatThrownBy(executeFuture::get).hasRootCauseMessage(expectedMessage);
        assertThatThrownBy(fetchFuture::get).hasRootCauseMessage(expectedMessage);
        assertThatThrownBy(tryFetchFuture::get).hasRootCauseMessage(expectedMessage);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testGlobalRequestCustomizer() throws Exception
    {
        mockedServer.when(TestData.Requests.Incoming.POST, once())
            .respond(TestData.Responses.NO_CONTENT);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .requestCustomizer(authorizationHeaderSetter(TestData.Strings.BASIC_FOOBAR))
            .requestCustomizer(authorizationHeaderSetter(TestData.Strings.BEARER_IDDQD))
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(TestData.Requests.Outgoing.POST)
            .returningNothing()
            .execute()
            .get();

        mockedServer.verify(TestData.Requests.Incoming.POST_AUTHORIZED);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testIndividualRequestCustomizer() throws Exception
    {
        mockedServer.when(TestData.Requests.Incoming.POST, once())
            .respond(TestData.Responses.NO_CONTENT);

        ClientConfig clientConfig = makeClientConfig();
        BasicRestClient client = makeClient(clientConfig);
        client.make(TestData.Requests.Outgoing.POST)
            .returningNothing()
            .customizingRequest(authorizationHeaderSetter(TestData.Strings.BEARER_IDDQD))
            .execute()
            .get();

        mockedServer.verify(TestData.Requests.Incoming.POST_AUTHORIZED);
    }

    @Test(timeOut = METHOD_TIMEOUT)
    public void testIndividualRequestCustomizerOverridesGlobal() throws Exception
    {
        mockedServer.when(TestData.Requests.Incoming.POST, once())
            .respond(TestData.Responses.NO_CONTENT);

        ClientConfig clientConfig = makeClientConfig().toBuilder()
            .requestCustomizer(authorizationHeaderSetter(TestData.Strings.BASIC_FOOBAR))
            .build();
        BasicRestClient client = makeClient(clientConfig);
        client.make(TestData.Requests.Outgoing.POST)
            .returningNothing()
            .customizingRequest(authorizationHeaderSetter(TestData.Strings.BEARER_IDDQD))
            .execute()
            .get();

        mockedServer.verify(TestData.Requests.Incoming.POST_AUTHORIZED);
    }

    private RequestCustomizer authorizationHeaderSetter(String value)
    {
        return builder -> builder.setHeader(TestData.Strings.AUTHORIZATION, value);
    }
}
