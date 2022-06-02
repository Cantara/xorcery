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
        title,
        body
    }
}
