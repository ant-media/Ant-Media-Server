package io.antmedia.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all unit tests
 * @param <T> class being tested
 */
public abstract class UnitTestBase<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected T classUnderTest;

    @BeforeEach
    public void logTestStart(TestInfo testInfo) {
        logger.info("Starting test: {}", testInfo.getDisplayName());
    }

    @AfterEach
    public void logTestFinish(TestInfo testInfo) {
        logger.info("Finishing test: {}", testInfo.getDisplayName());
    }

    @Test
    public void testShouldBeInTheSamePackage() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        assertTrue(
                genericSuperclass instanceof ParameterizedType,
                "Subclass " + getClass().getName() + " must directly parameterize UnitTestBase<T>");

        Type typeArg = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        assertTrue(
                typeArg instanceof Class<?>,
                "Type parameter T must be a concrete Class, but was " + typeArg);

        Class<?> classUnderTestType = (Class<?>) typeArg;
        String testPackage = getClass().getPackage().getName();
        String cutPackage = classUnderTestType.getPackage().getName();

        assertEquals(
                cutPackage,
                testPackage,
                "Test " + getClass().getName()
                        + " must live in the same package as class under test "
                        + classUnderTestType.getName());
    }

}
