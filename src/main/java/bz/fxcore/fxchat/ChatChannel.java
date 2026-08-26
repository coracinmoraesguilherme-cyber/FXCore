package bz.fxcore.fxchat;

import net.neoforged.neoforge.common.ModConfigSpec;

//arquivo config
public class ChatChannel {
    private String name;
    private String command;
    private String format;
    private double radius; //-1 para global, > 0 local
    private int permissionLevel; //0 para todos, 2 para staff/op

    public ChatChannel(String name, String command, String format, double radius, int permissionLevel) {
        this.name = name;
        this.command = command;
        this.format = format;
        this.radius = radius;
        this.permissionLevel = permissionLevel;
    }
    public String getName() { return name; }
    public String getCommand() { return command; }
    public String getFormat() { return format; }
    public double getRadius() { return radius; }
    public int getPermissionLevel() { return permissionLevel; }
}