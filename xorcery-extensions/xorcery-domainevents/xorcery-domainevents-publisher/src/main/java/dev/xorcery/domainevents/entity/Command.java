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
package dev.xorcery.domainevents.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.xorcery.domainevents.entity.annotation.Create;
import dev.xorcery.domainevents.entity.annotation.Delete;
import dev.xorcery.domainevents.entity.annotation.Update;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonIgnoreProperties({"create", "update", "delete"})
public interface Command {

    default String id()
    {
        return null;
    }

    static String getName(Command command)
    {
        return command.getClass().getSimpleName();
    }

    static String getName(Class<? extends Command> commandClass)
    {
        return commandClass.getSimpleName();
    }

    static boolean isCreate(Class<? extends Command> commandClass) {
        return commandClass.getAnnotation(Create.class) != null;
    }

    static boolean isUpdate(Class<? extends Command> commandClass) {
        return commandClass.getAnnotation(Update.class) != null;
    }

    static boolean isDelete(Class<? extends Command> commandClass) {
        return commandClass.getAnnotation(Delete.class) != null;
    }
}
