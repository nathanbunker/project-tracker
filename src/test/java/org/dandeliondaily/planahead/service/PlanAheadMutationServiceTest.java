package org.dandeliondaily.planahead.service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;

public class PlanAheadMutationServiceTest {

    private PlanAheadMutationService service;

    @Before
    public void setUp() {
        service = new PlanAheadMutationService();
    }

    // -----------------------------------------------------------------------
    // filterProjectsForTemplateMode
    // -----------------------------------------------------------------------

    @Test
    public void filterProjectsForTemplateMode_workMode_returnsOnlyBillableProjects() {
        Project workProject = project(1, "Client Work", "DEV");
        Project personalProject = project(2, "Personal Errands", "HOME");
        Project noBillCodeProject = project(3, "No Bill Code", null);

        List<Project> filtered = service.filterProjectsForTemplateMode(
                Arrays.asList(workProject, personalProject, noBillCodeProject),
                billCodeMap(billCode("DEV", "Y"), billCode("HOME", "N")), false);

        Assert.assertEquals(1, filtered.size());
        Assert.assertSame(workProject, filtered.get(0));
    }

    @Test
    public void filterProjectsForTemplateMode_personalMode_returnsOnlyNonBillableProjects() {
        Project workProject = project(1, "Client Work", "DEV");
        Project personalProject = project(2, "Personal Errands", "HOME");
        Project noBillCodeProject = project(3, "No Bill Code", null);

        List<Project> filtered = service.filterProjectsForTemplateMode(
                Arrays.asList(workProject, personalProject, noBillCodeProject),
                billCodeMap(billCode("DEV", "Y"), billCode("HOME", "N")), true);

        Assert.assertEquals(2, filtered.size());
        Assert.assertSame(personalProject, filtered.get(0));
        Assert.assertSame(noBillCodeProject, filtered.get(1));
    }

    @Test
    public void isBillableProject_unresolvedBillCode_returnsFalse() {
        Assert.assertFalse(
                service.isBillableProject(project(1, "Unknown", "MISSING"), new HashMap<String, BillCode>()));
    }

    // -----------------------------------------------------------------------
    // parseDay
    // -----------------------------------------------------------------------

    @Test
    public void parseDay_validIsoDate_returnsDate() throws Exception {
        Date result = service.parseDay("2025-06-15");
        Assert.assertNotNull(result);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Assert.assertEquals("2025-06-15", sdf.format(result));
    }

    @Test
    public void parseDay_emptyString_returnsNull() {
        Assert.assertNull(service.parseDay(""));
    }

    @Test
    public void parseDay_nullValue_returnsNull() {
        Assert.assertNull(service.parseDay(null));
    }

    @Test
    public void parseDay_invalidFormat_returnsNull() {
        Assert.assertNull(service.parseDay("06/15/2025"));
    }

    @Test
    public void parseDay_invalidDayInMonth_returnsNull() {
        Assert.assertNull(service.parseDay("2025-02-30"));
    }

    @Test
    public void parseDay_partialDate_returnsNull() {
        Assert.assertNull(service.parseDay("2025-06"));
    }

    // -----------------------------------------------------------------------
    // toDayKey
    // -----------------------------------------------------------------------

    @Test
    public void toDayKey_validDate_returnsYyyyMmDd() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse("2025-03-20");
        Assert.assertEquals("2025-03-20", service.toDayKey(date));
    }

    @Test
    public void toDayKey_nullDate_returnsEmpty() {
        Assert.assertEquals("", service.toDayKey(null));
    }

    // -----------------------------------------------------------------------
    // isBeforeDay
    // -----------------------------------------------------------------------

    @Test
    public void isBeforeDay_dateBeforeKey_returnsTrue() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse("2025-01-01");
        Assert.assertTrue(service.isBeforeDay(date, "2025-06-01"));
    }

    @Test
    public void isBeforeDay_dateSameAsKey_returnsFalse() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse("2025-06-01");
        Assert.assertFalse(service.isBeforeDay(date, "2025-06-01"));
    }

    @Test
    public void isBeforeDay_dateAfterKey_returnsFalse() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse("2025-12-31");
        Assert.assertFalse(service.isBeforeDay(date, "2025-06-01"));
    }

    @Test
    public void isBeforeDay_nullDate_returnsFalse() {
        Assert.assertFalse(service.isBeforeDay(null, "2025-06-01"));
    }

    // -----------------------------------------------------------------------
    // parseIntegerOrNull
    // -----------------------------------------------------------------------

    @Test
    public void parseIntegerOrNull_validNumber_returnsInteger() {
        Assert.assertEquals(Integer.valueOf(42), service.parseIntegerOrNull("42"));
    }

    @Test
    public void parseIntegerOrNull_negativeNumber_returnsNegativeInteger() {
        Assert.assertEquals(Integer.valueOf(-5), service.parseIntegerOrNull("-5"));
    }

    @Test
    public void parseIntegerOrNull_zero_returnsZero() {
        Assert.assertEquals(Integer.valueOf(0), service.parseIntegerOrNull("0"));
    }

    @Test
    public void parseIntegerOrNull_emptyString_returnsNull() {
        Assert.assertNull(service.parseIntegerOrNull(""));
    }

    @Test
    public void parseIntegerOrNull_nullValue_returnsNull() {
        Assert.assertNull(service.parseIntegerOrNull(null));
    }

    @Test
    public void parseIntegerOrNull_nonNumericString_returnsNull() {
        Assert.assertNull(service.parseIntegerOrNull("abc"));
    }

    @Test
    public void parseIntegerOrNull_whitespaceOnly_returnsNull() {
        Assert.assertNull(service.parseIntegerOrNull("   "));
    }

    // -----------------------------------------------------------------------
    // clip
    // -----------------------------------------------------------------------

    @Test
    public void clip_shortString_returnsUnchanged() {
        Assert.assertEquals("hello", service.clip("hello", 100));
    }

    @Test
    public void clip_exactLength_returnsUnchanged() {
        Assert.assertEquals("hello", service.clip("hello", 5));
    }

    @Test
    public void clip_longString_truncatesToMax() {
        Assert.assertEquals("hello", service.clip("hello world", 5));
    }

    @Test
    public void clip_nullValue_returnsEmpty() {
        Assert.assertEquals("", service.clip(null, 10));
    }

    @Test
    public void clip_trimsWhitespace() {
        Assert.assertEquals("hi", service.clip("  hi  ", 10));
    }

    // -----------------------------------------------------------------------
    // n (null-safe string)
    // -----------------------------------------------------------------------

    @Test
    public void n_nullValue_returnsEmptyString() {
        Assert.assertEquals("", service.n(null));
    }

    @Test
    public void n_nonNullValue_returnsValue() {
        Assert.assertEquals("hello", service.n("hello"));
    }

    @Test
    public void n_emptyString_returnsEmptyString() {
        Assert.assertEquals("", service.n(""));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Project project(int projectId, String name, String billCode) {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setProjectName(name);
        project.setBillCode(billCode);
        return project;
    }

    private BillCode billCode(String code, String billable) {
        BillCode billCode = new BillCode();
        billCode.setBillCode(code);
        billCode.setBillable(billable);
        return billCode;
    }

    private Map<String, BillCode> billCodeMap(BillCode... billCodes) {
        Map<String, BillCode> map = new HashMap<String, BillCode>();
        for (BillCode billCode : billCodes) {
            map.put(billCode.getBillCode(), billCode);
        }
        return map;
    }
}
