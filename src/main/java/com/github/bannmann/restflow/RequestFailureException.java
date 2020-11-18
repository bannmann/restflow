package com.github.bannmann.restflow;

/**
 * Thrown when the request failed without a status code.
 */
public class RequestFailureException extends RuntimeException
{
    public RequestFailureException()
    {
        super();
    }

    public RequestFailureException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RequestFailureException(String message)
    {
        super(message);
    }

    public RequestFailureException(Throwable cause)
    {
        super(cause);
    }
}
