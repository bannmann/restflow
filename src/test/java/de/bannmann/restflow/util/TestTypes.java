package de.bannmann.restflow.util;

import java.lang.reflect.Type;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class TestTypes
{
    public final List<String> listForTypeComparison = null;

    @Test
    public void testListOf()
    {
        Type synthetic = Types.listOf(String.class);
        Type real = getClass().getFields()[0].getGenericType();

        Assertions.assertThat(synthetic)
            .isEqualTo(real);
    }
}
