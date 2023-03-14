package com.exoreaction.xorcery.coordinator;

import picocli.CommandLine;

public class Main
{
    public static void main(String[] args ) {
        System.exit(new CommandLine(new com.exoreaction.xorcery.core.Main()).execute(args));
    }
}
