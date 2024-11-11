package dev.xorcery.test.context;

import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.command.annotation.Create;
import dev.xorcery.domainevents.command.annotation.Delete;
import dev.xorcery.domainevents.command.annotation.Update;

public interface ThingCommands {

    @Create
    record CreateThing(String id, String foo)
            implements Command {
    }

    @Update
    record UpdateThing(String id, String foo)
            implements Command {
    }

    @Update
    record UpdateBar(String id, String bar)
            implements Command {
    }

    @Delete
    record DeleteThing(String id)
            implements Command {
    }
}
