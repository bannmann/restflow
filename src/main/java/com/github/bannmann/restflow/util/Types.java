package com.github.bannmann.restflow.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Types
{
    @ToString
    private final static class ParameterizedTypeImpl implements ParameterizedType
    {
        private final Type rawType;
        private final Type[] actualTypeArguments;

        public ParameterizedTypeImpl(@NonNull Type rawType, @NonNull Type... arguments)
        {
            this.rawType = rawType;
            this.actualTypeArguments = arguments.clone();
        }

        @Override
        public Type[] getActualTypeArguments()
        {
            return actualTypeArguments.clone();
        }

        @Override
        public Type getRawType()
        {
            return rawType;
        }

        @Override
        public Type getOwnerType()
        {
            return null;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o instanceof ParameterizedType)
            {
                ParameterizedType that = (ParameterizedType) o;
                return Arrays.equals(actualTypeArguments, that.getActualTypeArguments()) &&
                    rawType.equals(that.getRawType()) &&
                    that.getOwnerType() == null;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int result = Objects.hash(rawType);
            result = 31 * result + Arrays.hashCode(actualTypeArguments);
            return result;
        }
    }

    public ParameterizedType listOf(@NonNull Class<?> elementClass)
    {
        return new ParameterizedTypeImpl(List.class, elementClass);
    }
}
