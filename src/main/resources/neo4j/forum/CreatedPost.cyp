MERGE(post:Post {id:$metadata.aggregateId})
SET
post.created_on=$metadata.timestamp,
post.last_updated_on=$metadata.timestamp,
post.is_comments_enabled=true
