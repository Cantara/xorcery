package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.model.StandardModel;

public interface ForumModel
        extends StandardModel {

    enum Label {
        Post,
        Comment
    }

    enum Relationship {
        HAS_COMMENT
    }

    enum Post {
        title,
        body,
        is_comments_enabled
    }

    enum Comment {
        body
    }
}
