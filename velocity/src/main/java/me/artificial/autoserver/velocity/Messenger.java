package me.artificial.autoserver.velocity;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Messenger {
    public static void send(Player player, String message) {
        send(player, message, null);
    }

    public static void send(Player player, String message, String serverName) {
        if (message == null) {
            return;
        }
        if (serverName != null) {
            message = message.replace("%serverName%", serverName);
        }
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

    public static void send(Player player, String message, long seconds) {
        if (message == null) {
            return;
        }
        message = message.replace("%time%", String.valueOf(seconds));
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }
}
