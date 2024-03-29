package dev.bannmann.restflow;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;

import lombok.experimental.UtilityClass;

import com.github.mizool.core.UrlRef;

@UtilityClass
class TestData
{
    public final int PORT = 1080;

    public final UrlRef BASE_URL = new UrlRef("http://localhost:" + PORT);
    public final UrlRef FAKE_SERVER_URL = new UrlRef("http://localhost:12345");

    @UtilityClass
    public class Strings
    {
        public final String PATH = "/foo";
        public final String PATH_MISSING = "/nonexisting";

        public final String AUTHORIZATION = "Authorization";
        public final String BEARER_IDDQD = "Bearer IDDQD";
        public final String BASIC_FOOBAR = "Basic foobar";
    }

    @UtilityClass
    public class Requests
    {
        @UtilityClass
        public class Outgoing
        {
            public final HttpRequest POST = createRequest(Strings.PATH).POST(noBody())
                .build();

            public final HttpRequest POST_MISSING = createRequest(Strings.PATH_MISSING).POST(noBody())
                .build();

            private static HttpRequest.Builder createRequest(String path)
            {
                return HttpRequest.newBuilder()
                    .uri(BASE_URL.resolve(path)
                        .toUri());
            }
        }

        @UtilityClass
        public class Incoming
        {
            public final org.mockserver.model.HttpRequest POST = request(Strings.PATH).withMethod("POST");
            public final org.mockserver.model.HttpRequest POST_AUTHORIZED = POST.clone()
                .withHeader(Strings.AUTHORIZATION, Strings.BEARER_IDDQD);
        }
    }

    @UtilityClass
    public class Responses
    {
        @UtilityClass
        public class Body
        {
            public final String HELLO_WORLD_OBJECT = "{\"greeting\":\"Hello, world!\"}";

            public final String HELLO_WORLD_JSON_ARRAY = "[" + HELLO_WORLD_OBJECT + "]";

            public final String
                INTERNAL_SERVER_ERROR_BODY
                = "Detected a slight field variance in the thera-magnetic caesium portal housing.";

            public final String
                HTML_UNEXPECTED_ERROR
                = "<html><body>Sorry, an unexpected error occurred. Please try again later.</body></html>";
        }

        public final org.mockserver.model.HttpResponse NO_CONTENT = response().withStatusCode(204);

        public final org.mockserver.model.HttpResponse HELLO_WORLD_OBJECT = response().withStatusCode(200)
            .withBody(Body.HELLO_WORLD_OBJECT);

        public final org.mockserver.model.HttpResponse DELAYED_HELLO_WORLD_OBJECT = HELLO_WORLD_OBJECT.clone()
            .withDelay(TimeUnit.MILLISECONDS, 750);

        public final org.mockserver.model.HttpResponse HELLO_WORLD_ARRAY = response().withStatusCode(200)
            .withBody(Body.HELLO_WORLD_JSON_ARRAY);

        public final org.mockserver.model.HttpResponse HTML_ERROR_PAGE_WITH_SUCCESS_STATUS = response().withStatusCode(
                200)
            .withBody(Body.HTML_UNEXPECTED_ERROR);

        public final org.mockserver.model.HttpResponse SERVER_BUSY = response().withStatusCode(429)
            .withBody("Please try again later")
            .withDelay(TimeUnit.MILLISECONDS, 500);

        public final org.mockserver.model.HttpResponse INTERNAL_SERVER_ERROR = response().withStatusCode(500)
            .withBody(Body.INTERNAL_SERVER_ERROR_BODY);
    }
}
