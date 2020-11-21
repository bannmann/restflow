package com.github.bannmann.restflow;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;

import lombok.experimental.UtilityClass;

import com.github.mizool.core.exception.CodeInconsistencyException;

@UtilityClass
class TestData
{
    public static final int PORT = 1080;

    public static final URL BASE_URL = makeUrl("http://localhost:" + PORT);

    public static final String PATH = "/foo";

    public static final URI FAKE_SERVER_URL = makeUri("http://localhost:12345");

    public static final String PATH_MISSING = "/nonexisting";

    private static URL makeUrl(String spec)
    {
        try
        {
            return makeUri(spec).toURL();
        }
        catch (MalformedURLException e)
        {
            throw new CodeInconsistencyException(e);
        }
    }

    private static URI makeUri(String spec)
    {
        try
        {
            return new URI(spec);
        }
        catch (URISyntaxException e)
        {
            throw new CodeInconsistencyException(e);
        }
    }

    @UtilityClass
    public class Requests
    {
        @UtilityClass
        public class Outgoing
        {
            public static final HttpRequest POST = createRequest(PATH).POST(noBody())
                .build();

            public static final HttpRequest POST_MISSING = createRequest(PATH_MISSING).POST(noBody())
                .build();

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
        }

        @UtilityClass
        public class Incoming
        {
            public static final org.mockserver.model.HttpRequest POST = request(PATH).withMethod("POST");
        }
    }

    @UtilityClass
    public class Responses
    {
        @UtilityClass
        public class Body
        {
            public static final String HELLO_WORLD_OBJECT = "{\"greeting\":\"Hello, world!\"}";

            public static final String HELLO_WORLD_JSON_ARRAY = "[" + HELLO_WORLD_OBJECT + "]";

            public static final String
                INTERNAL_SERVER_ERROR_BODY
                = "Detected a slight field variance in the thera-magnetic caesium portal housing.";
        }

        public static final org.mockserver.model.HttpResponse NO_CONTENT = response().withStatusCode(204);

        public static final org.mockserver.model.HttpResponse HELLO_WORLD_OBJECT = response().withStatusCode(200)
            .withBody(Body.HELLO_WORLD_OBJECT);

        public static final org.mockserver.model.HttpResponse HELLO_WORLD_ARRAY = response().withStatusCode(200)
            .withBody(Body.HELLO_WORLD_JSON_ARRAY);

        public static final org.mockserver.model.HttpResponse SERVER_BUSY = response().withStatusCode(429)
            .withBody("Please try again later")
            .withDelay(TimeUnit.MILLISECONDS, 500);

        public static final org.mockserver.model.HttpResponse INTERNAL_SERVER_ERROR = response().withStatusCode(500)
            .withBody(Body.INTERNAL_SERVER_ERROR_BODY);
    }
}
