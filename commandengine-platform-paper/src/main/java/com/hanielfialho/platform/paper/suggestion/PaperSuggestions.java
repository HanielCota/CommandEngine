package com.hanielfialho.platform.paper.suggestion;

/**
 * Namespace for Paper suggestion providers.
 */
public final class PaperSuggestions {

    private PaperSuggestions() {}

    public static PlayerSuggestionProvider players() {
        return new PlayerSuggestionProvider();
    }

    public static WorldSuggestionProvider worlds() {
        return new WorldSuggestionProvider();
    }
}
