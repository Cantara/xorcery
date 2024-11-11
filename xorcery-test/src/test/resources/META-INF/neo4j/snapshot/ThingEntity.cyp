MATCH (node:Thing)
WHERE node.id=$id
RETURN properties(node) as snapshot, node.lastUpdatedOn as lastUpdatedOn
