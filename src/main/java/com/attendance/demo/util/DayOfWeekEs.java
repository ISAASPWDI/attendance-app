package com.attendance.demo.util;

import java.time.LocalDate;

public final class DayOfWeekEs {

    private DayOfWeekEs() {
    }

    public static String label(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}
