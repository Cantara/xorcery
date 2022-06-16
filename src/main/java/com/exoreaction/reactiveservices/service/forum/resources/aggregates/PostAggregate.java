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

    public record AddComment(String id, String body)
            implements Command {
    }

    @Update
    public record UpdateComment(String id, String body)
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

    public void handle(CreatePost command) {
        add(new PostEvents.CreatedPost());
        add(new PostEvents.UpdatedPost(command.title(), command.body()));
    }

    public void handle(UpdatePost command) {
        add(new PostEvents.UpdatedPost(command.title(), command.body()));
    }

    public void handle(AddComment command) {
        add(new PostEvents.AddedComment(command.id()));
        add(new PostEvents.UpdatedComment(command.id(), command.body()));
    }

    public void handle(UpdateComment command) {
        add(new PostEvents.UpdatedComment(command.id(), command.body()));
    }

    protected void apply(PostEvents.UpdatedPost updatedPost) {
        snapshot.title = updatedPost.title();
    }
}
