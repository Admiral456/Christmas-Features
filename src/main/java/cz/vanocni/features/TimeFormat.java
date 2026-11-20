package cz.vanocni.features;

final class TimeFormat {
    private TimeFormat() {}

    static String mmss(long seconds) {
        long safe = Math.max(0L, seconds);
        long minutes = safe / 60L;
        long remainder = safe % 60L;
        return String.format("%02d:%02d", minutes, remainder);
    }
}
