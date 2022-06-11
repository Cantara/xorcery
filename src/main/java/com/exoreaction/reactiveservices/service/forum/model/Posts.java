package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;

import java.util.function.BiConsumer;

import static com.exoreaction.reactiveservices.service.neo4j.client.WhereClauseBuilder.where;

public class Posts {

    private static final String POSTS = String.format(
            "MATCH (%s:%s)",
            ForumModel.Post.class.getSimpleName(), ForumModel.Label.Post);

    private final static BiConsumer<GraphQuery, StringBuilder> whereClauses = where()
            .parameter(ForumModel.Post.id, String.class, "Post.id=$post_id");

    private final GraphDatabase db;

    public Posts(GraphDatabase db) {
        this.db = db;
    }

    public GraphQuery postById(String postId) {
        return posts().parameter(ForumModel.Post.id, postId);
    }

    public GraphQuery posts() {
        return db.query(POSTS).extend(whereClauses);
    }
}
