package dev.xorcery.function;

import org.junit.jupiter.api.Test;

import static dev.xorcery.function.LazyReference.lazyReference;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LazyReferenceTest {

    LazyReference<String> someValue = lazyReference();

    @Test
    public void testLazyReference()
    {
        assertEquals("This is an expensive value to calculate", getSomeValue());
        assertEquals("This is an expensive value to calculate", getSomeValue());
    }

    String getSomeValue(){
        return someValue.apply(()->"This is an expensive value to calculate");
    }

}