package com.attendance.demo.util;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

/** Official Peru non-working national holidays. Movable/decree-based dates — must be reviewed and extended every year. */
public final class PeruHolidays {

    private static final Map<LocalDate, String> HOLIDAYS_2026 = Map.ofEntries(
            Map.entry(LocalDate.of(2026, Month.JANUARY, 1), "Año Nuevo"),
            Map.entry(LocalDate.of(2026, Month.APRIL, 2), "Jueves Santo"),
            Map.entry(LocalDate.of(2026, Month.APRIL, 3), "Viernes Santo"),
            Map.entry(LocalDate.of(2026, Month.MAY, 1), "Día del Trabajo"),
            Map.entry(LocalDate.of(2026, Month.JUNE, 7), "Batalla de Arica y Día de la Bandera"),
            Map.entry(LocalDate.of(2026, Month.JUNE, 29), "San Pedro y San Pablo"),
            Map.entry(LocalDate.of(2026, Month.JULY, 23), "Día de la Fuerza Aérea del Perú"),
            Map.entry(LocalDate.of(2026, Month.JULY, 28), "Fiestas Patrias"),
            Map.entry(LocalDate.of(2026, Month.JULY, 29), "Fiestas Patrias"),
            Map.entry(LocalDate.of(2026, Month.AUGUST, 6), "Batalla de Junín"),
            Map.entry(LocalDate.of(2026, Month.AUGUST, 30), "Santa Rosa de Lima"),
            Map.entry(LocalDate.of(2026, Month.OCTOBER, 8), "Combate de Angamos"),
            Map.entry(LocalDate.of(2026, Month.NOVEMBER, 1), "Día de Todos los Santos"),
            Map.entry(LocalDate.of(2026, Month.DECEMBER, 8), "Inmaculada Concepción"),
            Map.entry(LocalDate.of(2026, Month.DECEMBER, 9), "Batalla de Ayacucho"),
            Map.entry(LocalDate.of(2026, Month.DECEMBER, 25), "Navidad")
    );

    private PeruHolidays() {
    }

    public static boolean isHoliday(LocalDate date) {
        return HOLIDAYS_2026.containsKey(date);
    }

    public static String nameOf(LocalDate date) {
        return HOLIDAYS_2026.get(date);
    }
}
