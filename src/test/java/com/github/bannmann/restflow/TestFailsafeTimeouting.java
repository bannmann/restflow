package com.github.bannmann.restflow;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.github.mizool.core.exception.UncheckedInterruptedException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.TimeoutExceededException;

@Slf4j
public class TestFailsafeTimeouting extends AbstractNameableTest
{
    private static final int REQUEST_TIMEOUT_MILLIS = 200;
    private static final int METHOD_TIMEOUT_MILLIS = (int) (2.5 * REQUEST_TIMEOUT_MILLIS);
    private static final Timeout<Object> TIMEOUT_POLICY = Timeout.of(Duration.ofMillis(REQUEST_TIMEOUT_MILLIS));

    @BeforeMethod
    public void setUp()
    {
    }

    @Test(timeOut = METHOD_TIMEOUT_MILLIS)
    public void testTimeoutKept() throws Exception
    {
        CompletableFuture<Void> slowFuture = getFutureDelayedByMillis(REQUEST_TIMEOUT_MILLIS / 2);

        CompletableFuture<Void> future = Failsafe.with(TIMEOUT_POLICY)
            .getStageAsync(() -> slowFuture);

        assertThatCode(future::get).doesNotThrowAnyException();
    }

    @Test(timeOut = METHOD_TIMEOUT_MILLIS)
    public void testTimeoutExceeded()
    {
        CompletableFuture<Void> slowFuture = getFutureDelayedByMillis(REQUEST_TIMEOUT_MILLIS * 2);

        CompletableFuture<Void> future = Failsafe.with(TIMEOUT_POLICY)
            .getStageAsync(() -> slowFuture);

        assertThatThrownBy(future::get).hasRootCauseInstanceOf(TimeoutExceededException.class);
    }

    private CompletableFuture<Void> getFutureDelayedByMillis(int millis)
    {
        return CompletableFuture.runAsync(() -> {
            try
            {
                Thread.sleep(millis);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread()
                    .interrupt();
                throw new UncheckedInterruptedException(e);
            }
        });
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
        final int numberOfRuns = 100;

        Random random = new Random();
        return IntStream.range(0, numberOfRuns)
            .mapToObj(value -> new Object[]{ value, random.nextInt(101) > 90 })
            .iterator();
    }
}
