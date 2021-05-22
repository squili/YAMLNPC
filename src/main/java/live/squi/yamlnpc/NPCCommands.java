package live.squi.yamlnpc;

import net.jitse.npclib.api.skin.MineSkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NPCCommands implements CommandExecutor, TabExecutor {
    private YAMLNPC instance = null;
    private final HashMap<UUID, DialogEditor> editors = new HashMap<>();

    private YAMLNPC getInstance() {
        if (instance == null)
            instance = YAMLNPC.getInstance();
        return instance;
    }

    public HashMap<UUID, DialogEditor> getEditors() {
        return editors;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return false;
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage("No subcommand");
            return true;
        }
        switch (args[0]) {
            case "create": {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /npc create (name) (tag)");
                    return true;
                }
                if (getInstance().getNpcData().containsKey(args[1])) {
                    sender.sendMessage("Name already used");
                    return true;
                }
                getInstance().createNPC(args[1], player.getLocation(),
                        String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                sender.sendMessage("Success");
                break;
            }
            case "delete": {
                if (args.length == 1) {
                    sender.sendMessage("Usage: /npc delete (name)");
                    return true;
                }
                NPCMeta meta = getInstance().getNpcData().get(args[1]);
                if (meta == null) {
                    sender.sendMessage("NPC not found");
                    return true;
                }
                getInstance().deleteNPC(meta);
                sender.sendMessage("Success");
                break;
            }
            case "skin": {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /npc skin (name) (mineskin id)");
                    return true;
                }
                NPCMeta meta = getInstance().getNpcData().get(args[1]);
                if (meta == null) {
                    sender.sendMessage("NPC not found");
                    return true;
                }
                int id;
                try {
                     id = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid skin id");
                    return true;
                }
                sender.sendMessage("Fetching skin...");
                MineSkinFetcher.fetchSkinFromIdAsync(id, skin ->
                    Bukkit.getScheduler().runTask(getInstance(), () -> {
                        meta.getNpc().setSkin(skin);
                        meta.setSkinId(id);
                        getInstance().updateNPC(meta);
                        YAMLNPC.reloadNPC(meta.getNpc());
                        sender.sendMessage("Success");
                    }));
                break;
            }
            case "dialog": {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /npc dialog (name)");
                    return true;
                }
                NPCMeta meta = getInstance().getNpcData().get(args[1]);
                if (meta == null) {
                    sender.sendMessage("NPC not found");
                    return true;
                }
                UUID uuid = UUID.randomUUID();
                DialogEditor editor = new DialogEditor(this, player, meta, uuid);
                editors.put(uuid, editor);
                editor.print();
                break;
            }
            case "tag": {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /npc tag (name) (tag)");
                    return true;
                }
                NPCMeta meta = getInstance().getNpcData().get(args[1]);
                if (meta == null) {
                    sender.sendMessage("NPC not found");
                    return true;
                }
                meta.getNpc().setText(List.of(String.join(" ", Arrays.copyOfRange(args, 2, args.length))));
                instance.updateNPC(meta);
                sender.sendMessage("Success");
                break;
            }
            case "ui": {
                // internal command - handle text ui
                if (args.length < 4) {
                    sender.sendMessage("Internal error (missing args)");
                    return true;
                }
                UUID uuid;
                try {
                    uuid = UUID.fromString(args[1]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("Internal error (malformed uuid)");
                    return true;
                }
                DialogEditor editor = editors.get(uuid);
                if (editor == null) {
                    sender.sendMessage("Expired");
                    return true;
                }
                int ord;
                try {
                    ord = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Internal error (malformed action)");
                    return true;
                }
                for (DialogEditor.DialogAction action : DialogEditor.DialogAction.values()) {
                    if (action.ordinal() == ord) {
                        int index;
                        try {
                            index = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("Internal error (malformed index)");
                            return true;
                        }
                        editor.action(action, index);
                        return true;
                    }
                }
                sender.sendMessage("Internal error (invalid action)");
                break;
            }
            default:
                sender.sendMessage("Unknown subcommand");
        }
        return true;
    }

    private List<String> getNPCNames() {
        return getInstance().getNpcData().values().stream().map(NPCMeta::getName).collect(Collectors.toList());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1)
            return List.of("create", "delete", "dialog", "skin", "tag");
        switch (args[0]) {
            case "create":
                if (args.length == 2)
                    return List.of("<name>");
                else
                    return List.of("<tag>");
            case "delete":
            case "dialog":
                if (args.length == 2)
                    return getNPCNames();
                break;
            case "skin":
                if (args.length == 2)
                    return getNPCNames();
                else if (args.length == 3)
                    return List.of("<mineskin id>");
                break;
            case "tag":
                if (args.length == 2)
                    return getNPCNames();
                else
                    return List.of("<tag>");
        }
        return List.of();
    }
}
