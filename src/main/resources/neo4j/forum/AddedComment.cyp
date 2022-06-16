MATCH (post:Post {id:$metadata.aggregateId})
MERGE (post)-[:HAS_COMMENT]->(comment:Comment {id:$id})
SET
comment.created_on=$metadata.timestamp,
comment.last_updated_on=$metadata.timestamp,
post.last_updated_on=$metadata.timestamp
