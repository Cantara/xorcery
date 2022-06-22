package com.exoreaction.xorcery.service.forum.model;

import com.exoreaction.xorcery.cqrs.model.CommonModel;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphQuery;

import java.text.MessageFormat;
import java.util.function.BiConsumer;

import static com.exoreaction.xorcery.service.neo4j.client.WhereClauseBuilder.where;

public record Posts(GraphDatabase db) {

    private static final String POSTS = MessageFormat.format(
            "MATCH ({0}:{0}) WITH {0}, {0} as {1}",
            ForumModel.Label.Post, CommonModel.Label.Entity);

    private final static BiConsumer<GraphQuery, StringBuilder> clauses = where()
            .parameter(CommonModel.Entity.id, String.class, "Post.id=$entity_id");

    public GraphQuery posts() {
        return db.query(POSTS).where(clauses);
    }
}
