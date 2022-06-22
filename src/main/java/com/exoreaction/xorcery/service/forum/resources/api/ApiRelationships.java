package com.exoreaction.xorcery.service.forum.resources.api;

import com.exoreaction.xorcery.jsonapi.schema.annotations.Cardinality;
import com.exoreaction.xorcery.jsonapi.schema.annotations.RelationshipSchema;

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
