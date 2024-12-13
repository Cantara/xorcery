package dev.xorcery.function;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static dev.xorcery.function.LazySupplier.lazy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LazySupplierTest {

    @Test
    public void testLazySupplier(){
        Supplier<String> someValue = lazy(()->"This is an expensive value to calculate");

        assertEquals("This is an expensive value to calculate", someValue.get());
    }
}