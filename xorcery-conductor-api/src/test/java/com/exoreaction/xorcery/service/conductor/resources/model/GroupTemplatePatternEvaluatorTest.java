package com.exoreaction.xorcery.service.conductor.resources.model;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
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