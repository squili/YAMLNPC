package live.squi.yamlnpc;

import net.jitse.npclib.api.NPC;

import java.util.ArrayList;

public class NPCMeta {
    private final NPC npc;
    private final ArrayList<String> dialog = new ArrayList<>();
    private final String name;
    private Integer skinId;

    public NPCMeta(NPC npc, String name) {
        this.npc = npc;
        this.name = name;
    }

    public NPC getNpc() {
        return npc;
    }

    public ArrayList<String> getDialog() {
        return dialog;
    }

    public String getName() {
        return name;
    }

    public Integer getSkinId() {
        return skinId;
    }

    public void setSkinId(Integer skinId) {
        this.skinId = skinId;
    }
}
