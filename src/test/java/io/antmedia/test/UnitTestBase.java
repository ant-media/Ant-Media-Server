package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all unit tests
 * @param <T> class being tested
 */
public abstract class UnitTestBase<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected T classUnderTest;

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            logger.info("Starting test: {}", description.getMethodName());
        }

        @Override
        protected void finished(Description description) {
            logger.info("Finishing test: {}", description.getMethodName());
        }
    };

    @Test
    public void testShouldBeInTheSamePackage() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        assertTrue(
                "Subclass " + getClass().getName() + " must directly parameterize UnitTestBase<T>",
                genericSuperclass instanceof ParameterizedType);

        Type typeArg = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        assertTrue(
                "Type parameter T must be a concrete Class, but was " + typeArg,
                typeArg instanceof Class<?>);

        Class<?> classUnderTestType = (Class<?>) typeArg;
        String testPackage = getClass().getPackage().getName();
        String cutPackage = classUnderTestType.getPackage().getName();

        assertEquals(
                "Test " + getClass().getName()
                        + " must live in the same package as class under test "
                        + classUnderTestType.getName(),
                cutPackage,
                testPackage);
    }

}
