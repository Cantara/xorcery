WITH coalesce($created,null) as created, coalesce($updated,null) as updated, coalesce($deleted,null) as deleted, coalesce($attributes,null) as attributes, coalesce($addedrelationships,null) as addedrelationships,coalesce($removedrelationships,null) as removedrelationships
CALL domainevent.jsondomainevent($metadata,created,updated,deleted,attributes,addedrelationships,removedrelationships)
RETURN true
