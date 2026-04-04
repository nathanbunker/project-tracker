package org.dandeliondaily.planahead.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;

public class PlanAheadBoardServiceTest {

    private PlanAheadBoardService service;

    @Before
    public void setUp() {
        service = new PlanAheadBoardService();
    }

    // --- resolveRowKeyForActionType ---

    @Test
    public void resolveRowKey_willMeet_returnsMeetings() {
        Assert.assertEquals("meetings", service.resolveRowKeyForActionType(ProjectNextActionType.WILL_MEET));
    }

    @Test
    public void resolveRowKey_committedTo_returnsCommitted() {
        Assert.assertEquals("committed", service.resolveRowKeyForActionType(ProjectNextActionType.COMMITTED_TO));
    }

    @Test
    public void resolveRowKey_will_returnsWill() {
        Assert.assertEquals("will", service.resolveRowKeyForActionType(ProjectNextActionType.WILL));
    }

    @Test
    public void resolveRowKey_willContact_returnsWill() {
        Assert.assertEquals("will", service.resolveRowKeyForActionType(ProjectNextActionType.WILL_CONTACT));
    }

    @Test
    public void resolveRowKey_willDocument_returnsWill() {
        Assert.assertEquals("will", service.resolveRowKeyForActionType(ProjectNextActionType.WILL_DOCUMENT));
    }

    @Test
    public void resolveRowKey_willFollowUp_returnsWill() {
        Assert.assertEquals("will", service.resolveRowKeyForActionType(ProjectNextActionType.WILL_FOLLOW_UP));
    }

    @Test
    public void resolveRowKey_willReview_returnsWill() {
        Assert.assertEquals("will", service.resolveRowKeyForActionType(ProjectNextActionType.WILL_REVIEW));
    }

    @Test
    public void resolveRowKey_might_returnsMight() {
        Assert.assertEquals("might", service.resolveRowKeyForActionType(ProjectNextActionType.MIGHT));
    }

    @Test
    public void resolveRowKey_goal_returnsEmpty() {
        Assert.assertEquals("", service.resolveRowKeyForActionType(ProjectNextActionType.GOAL));
    }

    @Test
    public void resolveRowKey_waiting_returnsEmpty() {
        Assert.assertEquals("", service.resolveRowKeyForActionType(ProjectNextActionType.WAITING));
    }

    @Test
    public void resolveRowKey_unknown_returnsEmpty() {
        Assert.assertEquals("", service.resolveRowKeyForActionType("UNKNOWN_TYPE"));
    }

    // --- resolveActionTypeForRowKey ---

    @Test
    public void resolveActionType_meetings_returnsWillMeet() {
        Assert.assertEquals(ProjectNextActionType.WILL_MEET, service.resolveActionTypeForRowKey("meetings"));
    }

    @Test
    public void resolveActionType_committed_returnsCommittedTo() {
        Assert.assertEquals(ProjectNextActionType.COMMITTED_TO, service.resolveActionTypeForRowKey("committed"));
    }

    @Test
    public void resolveActionType_will_returnsWill() {
        Assert.assertEquals(ProjectNextActionType.WILL, service.resolveActionTypeForRowKey("will"));
    }

    @Test
    public void resolveActionType_might_returnsMight() {
        Assert.assertEquals(ProjectNextActionType.MIGHT, service.resolveActionTypeForRowKey("might"));
    }

    @Test
    public void resolveActionType_unknown_returnsEmpty() {
        Assert.assertEquals("", service.resolveActionTypeForRowKey("unknown"));
    }

    @Test
    public void resolveActionType_emptyString_returnsEmpty() {
        Assert.assertEquals("", service.resolveActionTypeForRowKey(""));
    }
}
