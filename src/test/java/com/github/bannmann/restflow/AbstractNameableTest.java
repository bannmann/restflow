package com.github.bannmann.restflow;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.testng.ITest;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractNameableTest implements ITest
{
    private final ThreadLocal<String> testName = new ThreadLocal<>();

    @Override
    public String getTestName()
    {
        return testName.get();
    }

    @BeforeMethod
    public void setTestName(Method method, Object[] testData)
    {
        testName.set(method.getName());

        if (testData.length > 0)
        {
            int parameterIndex = 0;
            for (Parameter parameter : method.getParameters())
            {
                if (parameter.isAnnotationPresent(UseAsTestName.class))
                {
                    testName.set(method.getName() + "➞" + testData[parameterIndex]);
                    break;
                }
                parameterIndex++;
            }
        }
    }
}
