package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.model.CommonModel;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;

import java.text.MessageFormat;
import java.util.function.BiConsumer;

import static com.exoreaction.reactiveservices.service.neo4j.client.WhereClauseBuilder.where;

public record Comments(GraphDatabase db) {
    private static final String COMMENTS = MessageFormat.format(
            "MATCH ({0}:{0}) WITH {0}, {0} as {1}",
            ForumModel.Label.Comment, CommonModel.Label.Entity);

    private final static BiConsumer<GraphQuery, StringBuilder> clauses = where()
            .parameter(CommonModel.Entity.id, String.class, "Comment.id=$entity_id");

    private static final String POST_COMMENTS = MessageFormat.format(
            "MATCH ({0}:{0})-[:{1}]->({2}:{2}) WITH {2}, {2} as {3}",
            ForumModel.Label.Post, ForumModel.Relationship.HAS_COMMENT, ForumModel.Label.Comment, CommonModel.Label.Entity);

    private final static BiConsumer<GraphQuery, StringBuilder> byPostClauses = where()
            .parameter(CommonModel.Aggregate.id, String.class, "Post.id=$aggregate_id");

    public GraphQuery comments() {
        return db.query(COMMENTS).where(clauses);
    }

    public GraphQuery commentsByPost(String postId)
    {
        return db.query(POST_COMMENTS).where(byPostClauses).parameter(CommonModel.Aggregate.id, postId);
    }
}
