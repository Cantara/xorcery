CREATE CONSTRAINT ProjectionId IF NOT EXISTS FOR (node:Projection) REQUIRE node.id IS UNIQUE;
