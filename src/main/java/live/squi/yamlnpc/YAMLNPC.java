package live.squi.yamlnpc;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.jitse.npclib.NPCLib;
import net.jitse.npclib.api.NPC;
import net.jitse.npclib.api.events.NPCInteractEvent;
import net.jitse.npclib.api.skin.MineSkinFetcher;
import net.jitse.npclib.internal.NPCManager;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

@SuppressWarnings("SpellCheckingInspection")
public class YAMLNPC extends JavaPlugin implements Listener {
    private static YAMLNPC instance;
    private NPCLib npcLib;
    private final FileConfiguration config = getConfig();
    private final HashMap<String, NPCMeta> npcData = new HashMap<>();
    private final HashMap<String, String> idToName = new HashMap<>();
    private final HashMap<UUID, Callback<String>> callbackMap = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        npcLib = new NPCLib(this);
        npcLib.setAutoHideDistance(config.getDouble("auto-hide-distance"));
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("npc").setExecutor(new NPCCommands());

        // load npcs
        for (Map<?, ?> map : config.getMapList("npcs")) {
            List<String> tag = null;
            if (map.get("tag") != null)
                tag = List.of((String) map.get("tag"));
            NPC npc = npcLib.createNPC(tag);
            npc.setLocation(new Location(
                    Bukkit.getWorld(UUID.fromString((String) map.get("world"))),
                    (double) map.get("x"),
                    (double) map.get("y"),
                    (double) map.get("z"),
                    (float) (double) map.get("h"),
                    (float) (double) map.get("v")
            ));
            idToName.put(npc.getId(), (String) map.get("name"));
            NPCMeta meta = new NPCMeta(npc, (String) map.get("name"));
            npcData.put((String) map.get("name"), meta);
            if (map.get("skin") != null)
                MineSkinFetcher.fetchSkinFromIdAsync((Integer) map.get("skin"), skin ->
                        Bukkit.getScheduler().runTask(getInstance(), () -> {
                            npc.setSkin(skin);
                            meta.setSkinId((Integer) map.get("skin"));
                            reloadNPC(npc);
                        }));
            //noinspection unchecked
            for (String text : (List<String>) map.get("dialog")) {
                meta.getDialog().add(text);
            }
        }
    }

    @EventHandler
    public void onNPCInteract(NPCInteractEvent event) {
        ArrayList<String> dialog = npcData.get(idToName.get(event.getNPC().getId())).getDialog();
        event.getWhoClicked().sendMessage(dialog.get((int) Math.round(Math.random() * (dialog.size() - 1))));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (NPC npc : NPCManager.getAllNPCs()) {
            if (!npc.isCreated())
                npc.create();
            npc.show(event.getPlayer());
        }
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!callbackMap.containsKey(uuid))
            return;
        if (!(event.message() instanceof TextComponent))
            return;
        TextComponent message = (TextComponent) event.message();
        Callback<String> callback = callbackMap.remove(uuid);
        if (event.isAsynchronous())
            Bukkit.getScheduler().runTask(this, () -> callback.call(message.content()));
        else
            callback.call(message.content());
        event.setCancelled(true);
    }

    private void serializeNPC(Map<String, Object> map, NPCMeta meta) {
        map.put("world", meta.getNpc().getLocation().getWorld().getUID().toString());
        map.put("x", meta.getNpc().getLocation().getX());
        map.put("y", meta.getNpc().getLocation().getY());
        map.put("z", meta.getNpc().getLocation().getZ());
        map.put("h", meta.getNpc().getLocation().getYaw());
        map.put("v", meta.getNpc().getLocation().getPitch());
        List<String> text = meta.getNpc().getText();
        if (text.size() > 0)
            map.put("tag", text.get(0));
        else
            map.put("tag", null);
        map.put("name", meta.getName());
        map.put("dialog", meta.getDialog());
        map.put("skin", meta.getSkinId());
    }

    public void createNPC(String name, Location location, String displayName) {
        NPC npc = npcLib.createNPC(List.of(displayName)).setLocation(location).create();
        NPCMeta meta = new NPCMeta(npc, name);
        idToName.put(npc.getId(), name);
        npcData.put(name, meta);
        HashMap<String, Object> map = new HashMap<>();
        serializeNPC(map, meta);
        List<Map<?, ?>> data = config.getMapList("npcs");
        data.add(map);
        config.set("npcs", data);
        for (Player player : Bukkit.getOnlinePlayers())
            npc.show(player);
        saveConfig();
    }

    public void deleteNPC(NPCMeta meta) {
        meta.getNpc().destroy();
        List<Map<?, ?>> data = config.getMapList("npcs");
        for (int index = 0; index < data.size(); ++index)
            if (data.get(index).get("name").equals(meta.getName())) {
                data.remove(index);
                break;
            }
        config.set("npcs", data);
        npcData.remove(meta.getName());
        idToName.remove(meta.getNpc().getId());
        saveConfig();
    }

    public void updateNPC(NPCMeta meta) {
        List<Map<?, ?>> data = config.getMapList("npcs");
        for (Map<?, ?> map : data)
            if (map.get("name").equals(meta.getName())) {
                //noinspection unchecked
                serializeNPC((Map<String, Object>) map, meta);
                break;
            }
        config.set("npcs", data);
        saveConfig();
    }

    public static void reloadNPC(NPC npc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (npc.isShown(player))
                npc.hide(player);
            npc.show(player);
        }
    }

    public HashMap<String, NPCMeta> getNpcData() {
        return npcData;
    }

    public static YAMLNPC getInstance() {
        return instance;
    }

    public HashMap<UUID, Callback<String>> getCallbackMap() {
        return callbackMap;
    }
}
