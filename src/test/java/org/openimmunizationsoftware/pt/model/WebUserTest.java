package org.openimmunizationsoftware.pt.model;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

public class WebUserTest {

    @Test
    public void toLocalDateSupportsJdbcDate() {
        WebUser webUser = new WebUser();

        LocalDate result = webUser.toLocalDate(java.sql.Date.valueOf("2026-08-03"));

        assertEquals(LocalDate.of(2026, 8, 3), result);
    }

    @Test
    public void toLocalDateTimeSupportsJdbcDate() {
        WebUser webUser = new WebUser();

        LocalDateTime result = webUser.toLocalDateTime(java.sql.Date.valueOf("2026-08-03"));

        assertEquals(LocalDateTime.of(2026, 8, 3, 0, 0), result);
    }
}