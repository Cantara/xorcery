package dev.xorcery.domainevents.test.context;

import dev.xorcery.domainevents.entity.Command;
import dev.xorcery.domainevents.entity.annotation.Create;
import dev.xorcery.domainevents.entity.annotation.Delete;
import dev.xorcery.domainevents.entity.annotation.Update;
import jakarta.validation.constraints.NotBlank;

public interface ThingCommands {

    @Create
    record CreateThing(@NotBlank String id, @NotBlank String foo)
            implements Command {
    }

    @Update
    record UpdateThing(@NotBlank String id, @NotBlank String foo)
            implements Command {
    }

    @Update
    record UpdateBar(@NotBlank String id, @NotBlank String bar)
            implements Command {
    }

    @Delete
    record DeleteThing(@NotBlank String id)
            implements Command {
    }
}
