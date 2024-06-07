package com.exoreaction.xorcery.domainevents.test.context;

import com.exoreaction.xorcery.domainevents.entity.Command;
import com.exoreaction.xorcery.domainevents.entity.annotation.Create;
import com.exoreaction.xorcery.domainevents.entity.annotation.Delete;
import com.exoreaction.xorcery.domainevents.entity.annotation.Update;
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
