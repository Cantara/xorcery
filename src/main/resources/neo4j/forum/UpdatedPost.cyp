MATCH (post:Post {id:$metadata.aggregateId})
SET
post.title=$title,
post.body=$body
