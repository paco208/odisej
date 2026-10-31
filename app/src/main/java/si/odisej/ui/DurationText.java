package si.odisej.ui;

/** "<1 min", "12 min", "1 h 5 min". Čista Java; oblike nizov podamo od zunaj (lokalizacija). */
public final class DurationText {

    private DurationText() {
    }

    public static String format(int seconds, String underMinute, String minutesFmt,
                                String hoursFmt) {
        if (seconds < 60) {
            return underMinute;
        }
        int minutes = seconds / 60;
        if (minutes < 60) {
            return String.format(minutesFmt, minutes);
        }
        return String.format(hoursFmt, minutes / 60, minutes % 60);
    }
}
