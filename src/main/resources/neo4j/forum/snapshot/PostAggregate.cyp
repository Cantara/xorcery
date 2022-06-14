MATCH (entity:Post {id:$entity_id})
RETURN entity.title as title
