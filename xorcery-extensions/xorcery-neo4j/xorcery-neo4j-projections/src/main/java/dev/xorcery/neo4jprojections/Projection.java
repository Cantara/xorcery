/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.neo4jprojections;

/**
 * Fields stored in Neo4j for each projection. Updated on each transaction commit when new events have been projected.
 */
public enum Projection {
    id, // Id of projection
    createdOn, // Timestamp when projection was created
    projectionPosition, // Lastest position of event in incoming stream
    projectionTimestamp, // Lastest timestamp of event in incoming stream
    resourceUrl // URL of upstream source
}
