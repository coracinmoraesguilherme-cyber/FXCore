package bz.fxcore.core.database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FXPlayerData {
    public UUID uuid;
    public String lastName = "";
    public List<String> ipHistory = new ArrayList<>();
    
    // Status e dados em tempo real
    public long lastConnectionTimestamp = 0L;
    public boolean isFrozen = false;
    public boolean spyEnabled = false;
    public boolean hasGivebackActive = false;
    
    // Punições
    public boolean isBanned = false;
    public String banReason = "";
    public String banAuthor = "";

    // Listas internas
    public List<NoteEntry> notes = new ArrayList<>();
    public List<HistoryEntry> history = new ArrayList<>();

    public static class NoteEntry {
        public int id;
        public String author;
        public String text;
        public long timestamp;

        public NoteEntry(int id, String author, String text, long timestamp) {
            this.id = id;
            this.author = author;
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    public static class HistoryEntry {
        public String type;
        public String reason;
        public String author;
        public String duration;

        public HistoryEntry(String type, String reason, String author, String duration) {
            this.type = type;
            this.reason = reason;
            this.author = author;
            this.duration = duration;
        }
    }
}