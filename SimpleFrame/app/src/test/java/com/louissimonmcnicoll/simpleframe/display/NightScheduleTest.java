package com.louissimonmcnicoll.simpleframe.display;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NightScheduleTest {
    @Test
    public void overnightScheduleWrapsAcrossMidnight() {
        int start = 22 * 60;
        int end = 7 * 60;

        assertTrue(NightSchedule.isNight(true, 23 * 60, start, end));
        assertTrue(NightSchedule.isNight(true, 6 * 60 + 59, start, end));
        assertFalse(NightSchedule.isNight(true, 12 * 60, start, end));
        assertFalse(NightSchedule.isNight(true, end, start, end));
    }

    @Test
    public void daytimeScheduleUsesSimpleRange() {
        int start = 9 * 60;
        int end = 17 * 60;

        assertFalse(NightSchedule.isNight(true, start - 1, start, end));
        assertTrue(NightSchedule.isNight(true, start, start, end));
        assertTrue(NightSchedule.isNight(true, end - 1, start, end));
        assertFalse(NightSchedule.isNight(true, end, start, end));
    }

    @Test
    public void disabledOrEqualTimesNeverActivate() {
        assertFalse(NightSchedule.isNight(false, 23 * 60, 22 * 60, 7 * 60));
        assertFalse(NightSchedule.isNight(true, 12 * 60, 7 * 60, 7 * 60));
    }
}
