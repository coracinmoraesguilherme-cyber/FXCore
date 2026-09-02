package bz.fxcore.modules.staff;

public class TimeUtil {

    public static long parseTime(String input) {
        if (input == null || input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanente")) {
            return -1;
        }

        try {
            long value = Long.parseLong(input.replaceAll("[^0-9]", ""));
            char unit = input.toLowerCase().charAt(input.length() - 1);

            return switch (unit) {
                case 's' -> value * 1000L;
                case 'm' -> value * 60 * 1000L;
                case 'h' -> value * 60 * 60 * 1000L;
                case 'd' -> value * 24 * 60 * 60 * 1000L;
                default -> value * 60 * 1000L; // Padrão em minutos
            };
        } catch (Exception e) {
            return -1;
        }
    }

    public static String formatTime(long millis) {
        if (millis == Long.MAX_VALUE || millis < 0) return "Permanente";

        long seconds = millis / 1000 % 60;
        long minutes = millis / (60 * 1000) % 60;
        long hours = millis / (60 * 60 * 1000) % 24;
        long days = millis / (24 * 60 * 60 * 1000);

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}