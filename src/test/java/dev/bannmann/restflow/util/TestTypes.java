package dev.bannmann.restflow.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.List;

import org.testng.annotations.Test;

public class TestTypes
{
    public final List<String> listForTypeComparison = null;

    @Test
    public void testListOf()
    {
        Type synthetic = Types.listOf(String.class);
        Type real = getClass().getFields()[0].getGenericType();

        assertThat(synthetic).isEqualTo(real);
    }
}
