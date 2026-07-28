package com.louissimonmcnicoll.simpleframe.display;

import java.util.Calendar;

/** Pure time calculations shared by the activity, alarm scheduler, and unit tests. */
public final class NightSchedule {
    private static final int MINUTES_PER_DAY = 24 * 60;

    private NightSchedule() {
    }

    /** Returns the local wall-clock time as minutes after midnight. */
    public static int currentMinuteOfDay() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }

    /**
     * Tests whether a time falls in a configured night interval.
     *
     * <p>Intervals may cross midnight. Equal start and end values intentionally mean disabled
     * rather than a 24-hour blackout.</p>
     */
    public static boolean isNight(boolean enabled, int minuteOfDay, int startMinutes, int endMinutes) {
        if (!enabled) {
            return false;
        }

        int current = normalize(minuteOfDay);
        int start = normalize(startMinutes);
        int end = normalize(endMinutes);
        if (start == end) {
            return false;
        }
        if (start < end) {
            return current >= start && current < end;
        }
        return current >= start || current < end;
    }

    /** Returns the next local occurrence of a time, always strictly in the future. */
    public static long nextOccurrenceMillis(int minuteOfDay) {
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        int normalized = normalize(minuteOfDay);
        next.set(Calendar.HOUR_OF_DAY, normalized / 60);
        next.set(Calendar.MINUTE, normalized % 60);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return next.getTimeInMillis();
    }

    private static int normalize(int minutes) {
        int normalized = minutes % MINUTES_PER_DAY;
        return normalized < 0 ? normalized + MINUTES_PER_DAY : normalized;
    }
}
