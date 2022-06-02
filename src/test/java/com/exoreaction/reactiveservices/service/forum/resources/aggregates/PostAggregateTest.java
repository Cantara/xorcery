package com.exoreaction.reactiveservices.service.forum.resources.aggregates;

import com.exoreaction.reactiveservices.cqrs.DomainEvents;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.forum.resources.events.PostEvents;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class PostAggregateTest {

    @Test
    public void testCreate() throws Throwable {
        // Given
        PostAggregate aggregate = new PostAggregate();

        PostAggregate.PostSnapshot snapshot = aggregate.getSnapshot();
        snapshot.title = "Foo";

        // When
        DomainEvents events = aggregate.handle(new Metadata.Builder().build(), snapshot,
                new PostAggregate.CreatePost("New post", "This is a test post"));

        // Then
        assertThat(events.events().get(0).getClass(), equalTo(PostEvents.CreatedPost.class));
        assertThat(events.events().get(1).getClass(), equalTo(PostEvents.UpdatedPost.class));
        aggregate.getSnapshot().title = "New post";

    }

}