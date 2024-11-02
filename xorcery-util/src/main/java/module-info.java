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
module xorcery.util {
    exports dev.xorcery.concurrent;
    exports dev.xorcery.util;
    exports dev.xorcery.function;
    exports dev.xorcery.builders;
    exports dev.xorcery.process;
    exports dev.xorcery.lang;
    exports dev.xorcery.io;
    exports dev.xorcery.net;
    exports dev.xorcery.collections;
    exports dev.xorcery.hk2;

    requires org.glassfish.hk2.api;
    requires jakarta.annotation;
}