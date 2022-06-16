MATCH (comment:Comment {id:$id})<-[:HAS_COMMENT]-(post)
SET
comment.body=$body,
comment.last_updated_on=$metadata.timestamp,
post.last_updated_on=$metadata.timestamp
