package com.exoreaction.reactiveservices.service.forum.resources.aggregates;

import com.exoreaction.reactiveservices.cqrs.aggregate.Aggregate;
import com.exoreaction.reactiveservices.cqrs.aggregate.AggregateSnapshot;
import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.annotations.Create;
import com.exoreaction.reactiveservices.cqrs.annotations.Update;
import com.exoreaction.reactiveservices.service.forum.resources.events.PostEvents;

public class PostAggregate
        extends Aggregate<PostAggregate.PostSnapshot> {

    @Create
    public record CreatePost(String title, String body)
            implements Command {
    }

    @Update
    public record UpdatePost(String title, String body)
            implements Command {
    }

    public static class PostSnapshot
            implements AggregateSnapshot {
        public String title;
    }

    private PostSnapshot snapshot = new PostSnapshot();

    @Override
    public PostSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    protected void setSnapshot(PostSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public void handle(CreatePost post) {
        add(new PostEvents.CreatedPost());
        add(new PostEvents.UpdatedPost(post.title(), post.body()));
    }

    public void handle(UpdatePost post) {
        add(new PostEvents.UpdatedPost(post.title(), post.body()));
    }

    protected void apply(PostEvents.UpdatedPost updatedPost) {
        snapshot.title = updatedPost.title();
    }
}
