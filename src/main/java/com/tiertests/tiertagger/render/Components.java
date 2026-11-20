package com.tiertests.tiertagger.render;

import com.tiertests.tiertagger.TierTaggerClient;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.text.Tag;
import com.tiertests.tiertagger.core.text.Text;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
//? if >=1.21.9
import net.minecraft.network.chat.FontDescription;

import java.util.UUID;

/** Converts core text into Minecraft components and puts tags around names. */
public final class Components {
    //? if >=1.21.9 {
    private static final FontDescription ICON_FONT = new FontDescription.Resource(TierTaggerClient.id("icons"));
    //?} else
    /*private static final Identifier ICON_FONT = TierTaggerClient.id("icons");*/

    /** Empty sibling that marks a component as already tagged, so nothing gets tagged twice. */
    private static final Component MARKER = Component.literal("");

    private Components() {
    }

    public static MutableComponent of(Text text) {
        MutableComponent out = Component.empty();
        for (Text.Span span : text.spans()) {
            Style style = Style.EMPTY;
            if (span.color() >= 0) style = style.withColor(span.color());
            if (span.icon()) style = style.withFont(ICON_FONT);
            out.append(Component.literal(span.text()).setStyle(style));
        }
        return out;
    }

    public static MutableComponent icon(char glyph) {
        return Component.literal(String.valueOf(glyph)).setStyle(Style.EMPTY.withFont(ICON_FONT));
    }

    public static boolean isTagged(Component component) {
        if (component == null) return false;
        for (Component sibling : component.getSiblings()) {
            if (sibling == MARKER) return true;
        }
        return false;
    }

    /** The name with the player's tag around it, or null when there is no tag (yet). */
    public static Component decorate(UUID uuid, String name, Component displayName, boolean compact) {
        TierTagger tagger = TierTagger.get();
        if (tagger == null || uuid == null || displayName == null || isTagged(displayName)) return null;
        Tag tag = tagger.tag(uuid, name, compact);
        if (tag == null || tag.isEmpty()) return null;
        return Component.empty()
                .append(MARKER)
                .append(of(tag.prefix()))
                .append(displayName)
                .append(of(tag.suffix()));
    }
}
