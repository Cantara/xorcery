package com.exoreaction.reactiveservices.cqrs.aggregate;

import com.exoreaction.reactiveservices.cqrs.annotations.Create;
import com.exoreaction.reactiveservices.cqrs.annotations.Delete;
import com.exoreaction.reactiveservices.cqrs.annotations.Update;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonIgnoreProperties({"create", "update", "delete"})
public interface Command {

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
