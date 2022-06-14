package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.model.StandardModel;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;

import java.text.MessageFormat;
import java.util.function.BiConsumer;

import static com.exoreaction.reactiveservices.service.neo4j.client.WhereClauseBuilder.where;

public class Posts {

    private static final String POSTS = MessageFormat.format(
            "MATCH ({0}:{0}) WITH {0}, {0} as {1}",
            ForumModel.Label.Post, StandardModel.Label.Entity);

    private final static BiConsumer<GraphQuery, StringBuilder> clauses = where()
            .parameter(StandardModel.Entity.id, String.class, "Post.id=$entity_id");

    private final GraphDatabase db;

    public Posts(GraphDatabase db) {
        this.db = db;
    }

    public GraphQuery posts() {
        return db.query(POSTS).where(clauses);
    }
}
