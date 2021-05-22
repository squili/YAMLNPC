package live.squi.yamlnpc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DialogEditor {
    private final YAMLNPC instance;
    private final NPCCommands commands;
    private final Player player;
    private final NPCMeta meta;
    private final UUID uuid;
    private int dieAt;

    public enum DialogAction {
        ADD,
        DELETE,
        EDIT,
        MOVE_UP,
        MOVE_DOWN
    }

    public DialogEditor(NPCCommands commands, Player player, NPCMeta meta, UUID uuid) {
        instance = YAMLNPC.getInstance();
        this.commands = commands;
        this.player = player;
        this.meta = meta;
        this.uuid = uuid;
        dieAt = Bukkit.getCurrentTick() + 1200;
        timeout();
    }

    private void timeout() {
        if (Bukkit.getCurrentTick() >= dieAt) {
            commands.getEditors().remove(uuid);
        } else {
            Bukkit.getScheduler().runTaskLater(instance, this::timeout, dieAt - Bukkit.getCurrentTick());
        }
    }

    private Component generateButton(String symbol, NamedTextColor color,
                                     String hoverText, DialogAction action, int index) {
        return Component.text(symbol + " ", color)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText, color)))
                .clickEvent(ClickEvent.runCommand("/npc ui " + uuid.toString() +
                        " " + action.ordinal() + " " + index));
    }

    public void print() {
        // clear chat
        player.sendMessage(Component.text("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"));
        for (int i = 0; i < meta.getDialog().size(); ++i) {
            Component component = Component.empty()
                    .append(generateButton("✖", NamedTextColor.RED,
                            "Delete", DialogAction.DELETE, i))
                    .append(generateButton("✎", NamedTextColor.YELLOW,
                            "Edit", DialogAction.EDIT, i));

            if (i == 0 && meta.getDialog().size() > 1)
                //noinspection ResultOfMethodCallIgnored
                component.append(generateButton("↓", NamedTextColor.GRAY,
                        "Move Down", DialogAction.MOVE_DOWN, i));

            if (i == meta.getDialog().size() - 1 && meta.getDialog().size() > 1)
                //noinspection ResultOfMethodCallIgnored
                component.append(generateButton("↑", NamedTextColor.GRAY,
                        "Move Up", DialogAction.MOVE_UP, i));

            player.sendMessage(component.append(Component.text(meta.getDialog().get(i))));
        }
        player.sendMessage(generateButton("+", NamedTextColor.GREEN,
                "New", DialogAction.ADD, 0));
        dieAt = Bukkit.getCurrentTick() + 1200;
    }

    private void textInput(Callback<String> callback) {
        player.sendMessage("Enter text:");
        instance.getCallbackMap().put(player.getUniqueId(), text -> {
            if (Bukkit.getCurrentTick() >= dieAt)
                player.sendMessage("Expired");
            else {
                callback.call(text);
                instance.updateNPC(meta);
                print();
            }
        });
    }

    public void action(DialogAction actionType, int index) {
        switch (actionType) {
            case ADD:
                textInput(text -> meta.getDialog().add(text));
                return;

            case EDIT:
                textInput(text -> {
                    try {
                        meta.getDialog().set(index, text);
                    } catch (IndexOutOfBoundsException e) {
                        player.sendMessage("Internal error (set out of bounds)");
                    }
                });
                return;

            case DELETE:
                try {
                    meta.getDialog().remove(index);
                } catch (IndexOutOfBoundsException e) {
                    player.sendMessage("Internal error (remove out of bounds)");
                }
                break;

            case MOVE_UP:
                try {
                    meta.getDialog().set(index, meta.getDialog().set(index - 1, meta.getDialog().get(index)));
                } catch (IndexOutOfBoundsException e) {
                    player.sendMessage("Internal error (move out of bounds)");
                }
                break;

            case MOVE_DOWN:
                try {
                    meta.getDialog().set(index, meta.getDialog().set(index+1, meta.getDialog().get(index)));
                } catch (IndexOutOfBoundsException e) {
                    player.sendMessage("Internal error (move out of bounds)");
                }
                break;
        }
        instance.updateNPC(meta);
        instance.getCallbackMap().remove(player.getUniqueId());
        print();
    }
}
