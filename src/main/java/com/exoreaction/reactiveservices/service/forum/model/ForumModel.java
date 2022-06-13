package com.exoreaction.reactiveservices.service.forum.model;

public interface ForumModel {

    enum Label
    {
        Post,
        Comment
    }

    enum Relationship
    {
        HAS_COMMENT
    }

    enum Post
    {
        id,
        created_on,
        last_updated,
        title,
        body
    }

    enum Comment
    {
        id,
        body
    }
}
