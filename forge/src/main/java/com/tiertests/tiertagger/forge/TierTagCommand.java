package com.tiertests.tiertagger.forge;

import com.tiertests.tiertagger.forge.gui.LegacyConfigScreen;
import com.tiertests.tiertagger.forge.gui.LegacySearchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;

/** {@code /tiertagger} opens the settings, {@code /tiertagger <name>} searches a player. */
final class TierTagCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "tiertagger";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("tiertag");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tiertagger [player]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 1) {
            sender.addChatMessage(new ChatComponentText("§cUsage: " + getCommandUsage(sender)));
            return;
        }
        final String query = args.length == 1 ? args[0] : null;
        // The chat screen closes after the command runs, so open the GUI on the next tick.
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onTick(TickEvent.ClientTickEvent event) {
                if (event.phase != TickEvent.Phase.END) return;
                MinecraftForge.EVENT_BUS.unregister(this);
                Minecraft.getMinecraft().displayGuiScreen(query == null
                        ? new LegacyConfigScreen(null) : new LegacySearchScreen(null, query));
            }
        });
    }
}
