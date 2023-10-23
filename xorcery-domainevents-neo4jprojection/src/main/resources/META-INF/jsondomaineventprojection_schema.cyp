CREATE CONSTRAINT AggregateId IF NOT EXISTS FOR (node:Aggregate) REQUIRE node.id IS UNIQUE;
CREATE INDEX EntityId IF NOT EXISTS FOR (node:Entity) ON (node.id);
CREATE INDEX EntityAggregateId IF NOT EXISTS FOR (n:Entity) ON (n.aggregateId)
