package org.dandeliondaily.mcp.service;

import java.sql.Timestamp;
import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

public class McpActionContextSupportTest {

    @Test
    public void formatsSqlDateWithoutThrowing() {
        // Hibernate type="date" properties (e.g. ActionNext.nextActionDate) come back
        // as java.sql.Date, whose toInstant() always throws UnsupportedOperationException.
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2026, 9, 30));
        String iso = McpActionContextSupport.toIso(sqlDate);
        Assert.assertNotNull(iso);
        Assert.assertTrue(iso.startsWith("2026-09-30") || iso.startsWith("2026-09-29"));
    }

    @Test
    public void formatsSqlTimestampWithoutThrowing() {
        Timestamp timestamp = new Timestamp(0L);
        Assert.assertEquals("1970-01-01T00:00:00Z", McpActionContextSupport.toIso(timestamp));
    }

    @Test
    public void returnsNullForNullDate() {
        Assert.assertNull(McpActionContextSupport.toIso(null));
    }
}
