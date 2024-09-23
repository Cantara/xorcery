MERGE (entity:Person {id:$event.updated.id})
SET entity.name=$event.attributes.name
