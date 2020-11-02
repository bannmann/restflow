package de.bannmann.restflow.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import lombok.experimental.UtilityClass;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;

@UtilityClass
public class Types
{
    public <R> ParameterizedType listOf(Class<R> elementClass)
    {
        TypeDescription.Generic genericTypeDescription = TypeDescription.Generic.Builder.parameterizedType(List.class,
            elementClass)
            .build();

        Type fieldClass = new ByteBuddy().subclass(Object.class)
            .defineField("dummyField", genericTypeDescription, Visibility.PUBLIC)
            .make()
            .load(Types.class.getClassLoader())
            .getLoaded()
            .getFields()[0].getGenericType();
        return (ParameterizedType) fieldClass;
    }
}
