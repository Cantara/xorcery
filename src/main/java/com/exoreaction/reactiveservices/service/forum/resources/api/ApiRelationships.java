package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.jsonapi.schema.annotations.Cardinality;
import com.exoreaction.reactiveservices.jsonapi.schema.annotations.RelationshipSchema;

public interface ApiRelationships {
    enum Post {
        @RelationshipSchema(
                title = "Comments",
                description = "Comments for this post",
                cardinality = Cardinality.many
        )
        comments;
    }
}
