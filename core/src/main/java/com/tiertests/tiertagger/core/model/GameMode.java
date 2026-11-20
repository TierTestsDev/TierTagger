package com.tiertests.tiertagger.core.model;

import java.util.Locale;

/**
 * A gamemode as one tier list names it.
 * {@link #id()} is the list's own key (lower case); {@link #kind()} groups the same gamemode across lists.
 */
public final class GameMode {
    private final String id;
    private final String title;
    private final ModeKind kind;
    private final int color;

    public GameMode(String id, String title, int color) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.title = title == null || title.isEmpty() ? Strings.titleCase(id) : title;
        this.kind = ModeKind.of(id);
        this.color = color >= 0 ? color : kind.color();
    }

    public GameMode(String id, String title) {
        this(id, title, -1);
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public ModeKind kind() {
        return kind;
    }

    public int color() {
        return color;
    }

    public boolean isSameKind(GameMode other) {
        return other != null && (kind != ModeKind.OTHER ? kind == other.kind() : id.equals(other.id));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GameMode && id.equals(((GameMode) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
