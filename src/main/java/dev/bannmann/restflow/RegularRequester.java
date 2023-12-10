package dev.bannmann.restflow;

import java.net.http.HttpResponse;

final class RegularRequester<B, R> extends AbstractRequester<B, R>
{
    public static <B, R> Requester<R> forSpec(RequestSpecification<B, R> spec)
    {
        return new RegularRequester<>(spec);
    }

    private final RequestSpecification<B, R> spec;

    private RegularRequester(RequestSpecification<B, R> spec)
    {
        super(spec.createFinalRequest(), spec.getClientConfig());
        this.spec = spec;
    }

    @Override
    protected HttpResponse.BodyHandler<B> getBodyHandler()
    {
        return spec.getResponseBodyConfig()
            .getBodyHandler();
    }

    @Override
    protected void verifyNoErrors(HttpResponse<B> response)
    {
        int responseStatus = response.statusCode();
        if (isFailure(responseStatus))
        {
            throw createException(response);
        }
    }

    @Override
    protected R doExtractValue(HttpResponse<B> response)
    {
        return spec.getResponseBodyConfig()
            .getResponseConverter()
            .apply(response.body());
    }
}
