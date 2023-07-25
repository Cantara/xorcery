package com.exoreaction.xorcery.jsonapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LinksTest {

    @Test
    public void getByRel()
    {
        // Given
        Links links = new Links.Builder()
                .link("somerel", "http://localhost/1")
                .link("thisrel somerelx", "http://localhost/2")
                .link("somerels", "http://localhost/3")
                .build();

        //When/Then
        Assertions.assertNull(links.getByRel("somexrel").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/1", links.getByRel("somerel").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/2", links.getByRel("thisrel somerelx").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/2", links.getByRel("somerelx thisrel").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/3", links.getByRel("somerels").map(Link::getHref).orElse(null));

    }
}
