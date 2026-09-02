package bz.fxcore.modules.chat;

public class ChatChannel {
    private String name;
    private String command;
    private String format;
    private double radius;
    private int permissionLevel;
    private double slow;

    public ChatChannel(String name, String command, String format, double radius, int permissionLevel, double slow) {
        this.name = name;
        this.command = command;
        this.format = format;
        this.radius = radius;
        this.permissionLevel = permissionLevel;
        this.slow = slow;
    }

    public String getName() { return name; }
    public String getCommand() { return command; }
    public String getFormat() { return format; }
    public double getRadius() { return radius; }
    public int getPermissionLevel() { return permissionLevel; }
    public double getSlow() { return slow; }
    public void setSlow(double slow) { this.slow = slow; }
}