package com.exoreaction.util;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.GroupTemplatePatternEvaluator;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class GroupTemplatePatternEvaluatorTest {

    @Test
    public void testEval()
    {
        // Given
        ServiceResourceObject sro = new ServiceResourceObject(new ResourceObject.Builder("logappender", "server1")
                .attributes(new Attributes.Builder().attribute("environment", "development").attribute("tag", "sandbox"))
                .build());
        Link link = new Link("logevents", "http://localhost/ws/logevents");
        GroupTemplatePatternEvaluator evaluator = new GroupTemplatePatternEvaluator(sro, link.rel());

        // Then
        assertThat(evaluator.eval("rel=='logevents'"), equalTo(true));
        assertThat(evaluator.eval("type=='logappender'"), equalTo(true));

        assertThat(evaluator.eval("tag=='sandbox'"), equalTo(true));
        assertThat(evaluator.eval("environment=='development'"), equalTo(true));
    }
}