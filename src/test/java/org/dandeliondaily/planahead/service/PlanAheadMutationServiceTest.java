package org.dandeliondaily.planahead.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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