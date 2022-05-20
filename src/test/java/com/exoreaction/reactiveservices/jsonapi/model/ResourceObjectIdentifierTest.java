package com.exoreaction.reactiveservices.jsonapi.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class ResourceObjectIdentifierTest {

    @Test
    void testEquals() {
        ResourceObjectIdentifier roi1 = new ResourceObjectIdentifier.Builder("type1", "id1").build();
        ResourceObjectIdentifier roi12 = new ResourceObjectIdentifier.Builder("type1", "id1").build();
        ResourceObjectIdentifier roi2 = new ResourceObjectIdentifier.Builder("type2", "id2").build();

        assertThat(roi1.equals(roi2), equalTo(false));
        assertThat(roi1.equals(roi12), equalTo(true));
    }
}