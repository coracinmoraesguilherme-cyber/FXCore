package bz.fxcore.modules.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamData {
    public String id;
    public String name;
    public String tag;
    public String color;
    public String tagColor;
    public UUID owner;
    public List<UUID> members = new ArrayList<>();
    
    // Armazena UUID do jogador removido -> Motivo da remoção
    public Map<UUID, String> offlineKicks = new HashMap<>();

    public TeamData(String id, String name, String tag, UUID owner) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.owner = owner;
        this.color = "§f";
        this.tagColor = "§7";
        this.members.add(owner);
    }
}