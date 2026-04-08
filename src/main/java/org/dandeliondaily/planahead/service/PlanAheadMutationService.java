package org.dandeliondaily.planahead.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.dandeliondaily.planahead.model.PlanAheadBoardModel;
import org.dandeliondaily.planahead.model.PlanAheadMutationResult;
import org.dandeliondaily.planahead.render.PlanAheadPageRenderer;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;

public class PlanAheadMutationService {

    static final String PHASE_ACTIVE = "Acti";
    static final String BILLABLE_YES = "Y";
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private final PlanAheadBoardService boardService = new PlanAheadBoardService();
    private final PlanAheadPageRenderer renderer = new PlanAheadPageRenderer();

    public PlanAheadMutationResult moveCard(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        String targetDateString = appReq.getRequest().getParameter("targetDate");
        String targetRow = n(appReq.getRequest().getParameter("targetRow")).trim().toLowerCase();

        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        Date targetDate = parseDay(targetDateString);
        if (targetDate == null) {
            result.setSuccess(false);
            result.setMessage("targetDate must be in yyyy-MM-dd format");
            return result;
        }

        String targetActionType = boardService.resolveActionTypeForRowKey(targetRow);
        TimeSlot targetTimeSlot = boardService.resolveTimeSlotForRowKey(targetRow);
        if (!personalMode && targetActionType.length() == 0) {
            result.setSuccess(false);
            result.setMessage("targetRow is invalid");
            return result;
        }
        if (personalMode && targetTimeSlot == null) {
            result.setSuccess(false);
            result.setMessage("targetRow is invalid");
            return result;
        }

        String todayKey = toDayKey(stripToDate(appReq.getWebUser().getToday(), appReq));
        if (isBeforeDay(targetDate, todayKey)) {
            result.setSuccess(false);
            result.setMessage("Cannot move cards to a past date");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        String sourceDayKey;
        String sourceRowKey;
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action is not available for this workspace");
                return result;
            }
            if (!isActionCompatibleWithMode(action, appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage(personalMode ? "Only personal actions are available in Personal mode"
                        : "Only work actions are available in Work mode");
                return result;
            }

            sourceDayKey = toDayKey(action.getNextActionDate());
            sourceRowKey = resolveRowKeyForAction(action, appReq);

            if (personalMode) {
                action.setTimeSlot(targetTimeSlot);
                if (n(action.getNextActionType()).trim().length() == 0) {
                    action.setNextActionType(ProjectNextActionType.WILL);
                }
            } else {
                action.setNextActionType(targetActionType);
            }
            action.setNextActionDate(targetDate);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);

        Set<String> cellIds = new LinkedHashSet<String>();
        if (sourceDayKey.length() > 0 && sourceRowKey.length() > 0) {
            cellIds.add(PlanAheadPageRenderer.kanbanCellDomId(sourceDayKey, sourceRowKey));
        }
        String targetDayKey = toDayKey(targetDate);
        cellIds.add(PlanAheadPageRenderer.kanbanCellDomId(targetDayKey, targetRow));

        for (PlanAheadBoardModel.RowModel rowModel : boardModel.getRows()) {
            for (PlanAheadBoardModel.CellModel cellModel : rowModel.getCells()) {
                String domId = PlanAheadPageRenderer.kanbanCellDomId(cellModel.getDayKey(), cellModel.getRowKey());
                if (cellIds.contains(domId)) {
                    result.getAffectedCellsHtml().put(domId,
                            renderer.renderKanbanCellHtml(cellModel, boardModel.isWorkMode()));
                }
            }
        }

        Set<String> headerDayKeys = new LinkedHashSet<String>();
        if (sourceDayKey.length() > 0) {
            headerDayKeys.add(sourceDayKey);
        }
        headerDayKeys.add(targetDayKey);
        for (PlanAheadBoardModel.DayHeaderModel dayHeaderModel : boardModel.getDayHeaders()) {
            if (headerDayKeys.contains(dayHeaderModel.getDayKey())) {
                result.getAffectedHeadersHtml().put(
                        PlanAheadPageRenderer.dayHeaderDomId(dayHeaderModel.getDayKey()),
                        renderer.renderDayHeader(dayHeaderModel));
            }
        }

        result.setSuccess(true);
        result.setMessage("Card moved");
        return result;
    }

    public PlanAheadMutationResult loadCardEdit(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
        if (action == null) {
            result.setSuccess(false);
            result.setMessage("Action not found");
            return result;
        }
        if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
            result.setSuccess(false);
            result.setMessage("Action is not available for this workspace");
            return result;
        }
        if (!isActionCompatibleWithMode(action, appReq)) {
            result.setSuccess(false);
            result.setMessage(personalMode ? "Only personal actions are available in Personal mode"
                    : "Only work actions are available in Work mode");
            return result;
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("actionNextId", action.getActionNextId());
        data.put("nextActionDate", toDayKey(action.getNextActionDate()));
        data.put("nextActionType", n(action.getNextActionType()));
        data.put("timeSlot", action.getTimeSlot() == null ? TimeSlot.AFTERNOON.getId() : action.getTimeSlot().getId());
        data.put("projectName", action.getProject() == null ? "" : n(action.getProject().getProjectName()));
        data.put("nextDescription", n(action.getNextDescription()));
        data.put("nextTimeEstimate", action.getNextTimeEstimate() == null ? 0 : action.getNextTimeEstimate());
        data.put("nextTargetDate", toDayKey(action.getNextTargetDate()));
        data.put("nextDeadlineDate", toDayKey(action.getNextDeadlineDate()));
        data.put("linkUrl", n(action.getLinkUrl()));
        data.put("nextNote", n(action.getNextNotes()));
        data.put("nextContactId",
                action.getNextContactId() == null ? "" : String.valueOf(action.getNextContactId().intValue()));

        result.setSuccess(true);
        result.setMessage("OK");
        result.setData(data);
        return result;
    }

    public PlanAheadMutationResult saveCardEdit(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        String nextActionDateString = appReq.getRequest().getParameter("nextActionDate");
        Date nextActionDate = parseDay(nextActionDateString);
        if (nextActionDate == null) {
            result.setSuccess(false);
            result.setMessage("nextActionDate must be in yyyy-MM-dd format");
            return result;
        }

        String todayKey = toDayKey(stripToDate(appReq.getWebUser().getToday(), appReq));
        if (isBeforeDay(nextActionDate, todayKey)) {
            result.setSuccess(false);
            result.setMessage("Cannot schedule action to a past date");
            return result;
        }

        String nextActionType = n(appReq.getRequest().getParameter("nextActionType")).trim();
        if (!personalMode
                && (nextActionType.length() == 0
                        || boardService.resolveRowKeyForActionType(nextActionType).length() == 0)) {
            result.setSuccess(false);
            result.setMessage("nextActionType is invalid for Plan Ahead");
            return result;
        }

        TimeSlot nextTimeSlot = TimeSlot.getTimeSlot(n(appReq.getRequest().getParameter("timeSlot")).trim());
        if (personalMode && nextTimeSlot == null) {
            nextTimeSlot = TimeSlot.AFTERNOON;
        }

        Integer nextTimeEstimate = parseIntegerOrNull(appReq.getRequest().getParameter("nextTimeEstimate"));
        if (nextTimeEstimate != null && nextTimeEstimate.intValue() < 0) {
            result.setSuccess(false);
            result.setMessage("nextTimeEstimate cannot be negative");
            return result;
        }

        Date nextTargetDate = parseOptionalDay(appReq.getRequest().getParameter("nextTargetDate"));
        Date nextDeadlineDate = parseOptionalDay(appReq.getRequest().getParameter("nextDeadlineDate"));
        if (n(appReq.getRequest().getParameter("nextTargetDate")).trim().length() > 0 && nextTargetDate == null) {
            result.setSuccess(false);
            result.setMessage("nextTargetDate must be in yyyy-MM-dd format");
            return result;
        }
        if (n(appReq.getRequest().getParameter("nextDeadlineDate")).trim().length() > 0 && nextDeadlineDate == null) {
            result.setSuccess(false);
            result.setMessage("nextDeadlineDate must be in yyyy-MM-dd format");
            return result;
        }

        Integer nextContactId = parseIntegerOrNull(appReq.getRequest().getParameter("nextContactId"));

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        String sourceDayKey;
        String sourceRowKey;
        String targetDayKey;
        String targetRowKey;
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action is not available for this workspace");
                return result;
            }
            if (!isActionCompatibleWithMode(action, appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage(personalMode ? "Only personal actions are available in Personal mode"
                        : "Only work actions are available in Work mode");
                return result;
            }

            sourceDayKey = toDayKey(action.getNextActionDate());
            sourceRowKey = resolveRowKeyForAction(action, appReq);

            action.setNextActionDate(nextActionDate);
            if (personalMode) {
                action.setTimeSlot(nextTimeSlot);
                if (n(action.getNextActionType()).trim().length() == 0) {
                    action.setNextActionType(ProjectNextActionType.WILL);
                }
            } else {
                action.setNextActionType(nextActionType);
            }
            action.setNextDescription(clip(n(appReq.getRequest().getParameter("nextDescription")), 12000));
            action.setNextTimeEstimate(personalMode ? Integer.valueOf(0)
                    : (nextTimeEstimate == null ? Integer.valueOf(0) : nextTimeEstimate));
            action.setNextTargetDate(nextTargetDate);
            action.setNextDeadlineDate(nextDeadlineDate);
            action.setLinkUrl(clip(n(appReq.getRequest().getParameter("linkUrl")), 1200));
            action.setNextNotes(clip(n(appReq.getRequest().getParameter("nextNote")), 4000));
            action.setNextContactId(nextContactId);
            action.setNextChangeDate(new Date());

            targetDayKey = toDayKey(action.getNextActionDate());
            targetRowKey = resolveRowKeyForAction(action, appReq);

            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);

        Set<String> cellIds = new LinkedHashSet<String>();
        if (sourceDayKey.length() > 0 && sourceRowKey.length() > 0) {
            cellIds.add(PlanAheadPageRenderer.kanbanCellDomId(sourceDayKey, sourceRowKey));
        }
        if (targetDayKey.length() > 0 && targetRowKey.length() > 0) {
            cellIds.add(PlanAheadPageRenderer.kanbanCellDomId(targetDayKey, targetRowKey));
        }

        for (PlanAheadBoardModel.RowModel rowModel : boardModel.getRows()) {
            for (PlanAheadBoardModel.CellModel cellModel : rowModel.getCells()) {
                String domId = PlanAheadPageRenderer.kanbanCellDomId(cellModel.getDayKey(), cellModel.getRowKey());
                if (cellIds.contains(domId)) {
                    result.getAffectedCellsHtml().put(domId,
                            renderer.renderKanbanCellHtml(cellModel, boardModel.isWorkMode()));
                }
            }
        }

        Set<String> headerDayKeys = new LinkedHashSet<String>();
        if (sourceDayKey.length() > 0) {
            headerDayKeys.add(sourceDayKey);
        }
        if (targetDayKey.length() > 0) {
            headerDayKeys.add(targetDayKey);
        }
        for (PlanAheadBoardModel.DayHeaderModel dayHeaderModel : boardModel.getDayHeaders()) {
            if (headerDayKeys.contains(dayHeaderModel.getDayKey())) {
                result.getAffectedHeadersHtml().put(
                        PlanAheadPageRenderer.dayHeaderDomId(dayHeaderModel.getDayKey()),
                        renderer.renderDayHeader(dayHeaderModel));
            }
        }

        result.setSuccess(true);
        result.setMessage("Action saved");
        return result;
    }

    public PlanAheadMutationResult saveCardEstimate(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        if (personalMode) {
            result.setSuccess(false);
            result.setMessage("Estimate editing is only available in Work mode");
            return result;
        }
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        Integer nextTimeEstimate = parseIntegerOrNull(appReq.getRequest().getParameter("nextTimeEstimate"));
        if (nextTimeEstimate == null) {
            nextTimeEstimate = Integer.valueOf(0);
        }
        if (nextTimeEstimate.intValue() < 0) {
            result.setSuccess(false);
            result.setMessage("nextTimeEstimate cannot be negative");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        String dayKey;
        String rowKey;
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action is not available for this workspace");
                return result;
            }
            if (!isActionCompatibleWithMode(action, appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Only work actions are available in Work mode");
                return result;
            }

            dayKey = toDayKey(action.getNextActionDate());
            rowKey = boardService.resolveRowKeyForActionType(action.getNextActionType());
            if (rowKey.length() == 0) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action type is not editable in Plan Ahead");
                return result;
            }

            action.setNextTimeEstimate(nextTimeEstimate);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);

        String cellId = PlanAheadPageRenderer.kanbanCellDomId(dayKey, rowKey);
        for (PlanAheadBoardModel.RowModel rowModel : boardModel.getRows()) {
            for (PlanAheadBoardModel.CellModel cellModel : rowModel.getCells()) {
                String domId = PlanAheadPageRenderer.kanbanCellDomId(cellModel.getDayKey(), cellModel.getRowKey());
                if (cellId.equals(domId)) {
                    result.getAffectedCellsHtml().put(domId,
                            renderer.renderKanbanCellHtml(cellModel, boardModel.isWorkMode()));
                }
            }
        }

        for (PlanAheadBoardModel.DayHeaderModel dayHeaderModel : boardModel.getDayHeaders()) {
            if (dayKey.equals(dayHeaderModel.getDayKey())) {
                result.getAffectedHeadersHtml().put(
                        PlanAheadPageRenderer.dayHeaderDomId(dayHeaderModel.getDayKey()),
                        renderer.renderDayHeader(dayHeaderModel));
                break;
            }
        }

        result.setSuccess(true);
        result.setMessage("Estimate updated");
        return result;
    }

    public PlanAheadMutationResult saveCardDescriptionInline(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        String nextDescription = clip(n(appReq.getRequest().getParameter("nextDescription")), 12000);

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        String dayKey;
        String rowKey;
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action is not available for this workspace");
                return result;
            }
            if (!isActionCompatibleWithMode(action, appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage(personalMode ? "Only personal actions are available in Personal mode"
                        : "Only work actions are available in Work mode");
                return result;
            }
            if (action.getNextActionDate() == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action date is required for inline description editing");
                return result;
            }

            dayKey = toDayKey(action.getNextActionDate());
            rowKey = resolveRowKeyForAction(action, appReq);
            if (rowKey.length() == 0) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action type is not editable in Plan Ahead");
                return result;
            }

            action.setNextDescription(nextDescription);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);

        String cellId = PlanAheadPageRenderer.kanbanCellDomId(dayKey, rowKey);
        for (PlanAheadBoardModel.RowModel rowModel : boardModel.getRows()) {
            for (PlanAheadBoardModel.CellModel cellModel : rowModel.getCells()) {
                String domId = PlanAheadPageRenderer.kanbanCellDomId(cellModel.getDayKey(), cellModel.getRowKey());
                if (cellId.equals(domId)) {
                    result.getAffectedCellsHtml().put(domId,
                            renderer.renderKanbanCellHtml(cellModel, boardModel.isWorkMode()));
                }
            }
        }

        for (PlanAheadBoardModel.DayHeaderModel dayHeaderModel : boardModel.getDayHeaders()) {
            if (dayKey.equals(dayHeaderModel.getDayKey())) {
                result.getAffectedHeadersHtml().put(
                        PlanAheadPageRenderer.dayHeaderDomId(dayHeaderModel.getDayKey()),
                        renderer.renderDayHeader(dayHeaderModel));
                break;
            }
        }

        result.setSuccess(true);
        result.setMessage("Description updated");
        return result;
    }

    public PlanAheadMutationResult deleteCardEdit(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action is not available for this workspace");
                return result;
            }
            if (!isActionCompatibleWithMode(action, appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage(personalMode ? "Only personal actions can be deleted in Personal mode"
                        : "Only work actions can be deleted in Work mode");
                return result;
            }

            action.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        result.setSuccess(true);
        result.setMessage("Action deleted");
        return result;
    }

    public PlanAheadMutationResult undoDeleteCard(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Action is not available for this workspace");
                return result;
            }

            action.setNextActionStatus(ProjectNextActionStatus.READY);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        result.setSuccess(true);
        result.setMessage("Action restored");
        return result;
    }

    public PlanAheadMutationResult saveTemplateEstimate(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        if (isPersonalMode(appReq)) {
            result.setSuccess(false);
            result.setMessage("Template estimate editing is only available in Work mode");
            return result;
        }
        String templateActionNextIdString = appReq.getRequest().getParameter("templateActionNextId");
        int templateActionNextId;
        try {
            templateActionNextId = Integer.parseInt(n(templateActionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("templateActionNextId must be a whole number");
            return result;
        }

        Integer nextTimeEstimate = parseIntegerOrNull(appReq.getRequest().getParameter("nextTimeEstimate"));
        if (nextTimeEstimate == null) {
            nextTimeEstimate = Integer.valueOf(0);
        }
        if (nextTimeEstimate.intValue() < 0) {
            result.setSuccess(false);
            result.setMessage("nextTimeEstimate cannot be negative");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext templateAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
                    templateActionNextId);
            if (templateAction == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(templateAction.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action is not available for this workspace");
                return result;
            }
            if (!templateAction.isBillable() || templateAction.getTemplateType() == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Only work templates are editable here");
                return result;
            }

            templateAction.setNextTimeEstimate(nextTimeEstimate);
            templateAction.setNextChangeDate(new Date());
            dataSession.update(templateAction);

            Date today = stripToDate(appReq.getWebUser().getToday(), appReq);
            Query query = dataSession.createQuery(
                    "from ProjectActionNext pan where pan.workspaceId = :workspaceId "
                            + "and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                            + "and pan.billable = :billable "
                            + "and pan.templateActionNextId = :templateActionNextId "
                            + "and pan.nextActionDate is not null and pan.nextActionDate >= :today "
                            + "and pan.nextActionStatusString <> :cancelled "
                            + "and pan.nextActionStatusString <> :completed");
            query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
            query.setParameter("contactId", appReq.getWebUser().getContactId());
            query.setParameter("nextContactId", appReq.getWebUser().getContactId());
            query.setParameter("billable", true);
            query.setParameter("templateActionNextId", templateActionNextId);
            query.setParameter("today", today);
            query.setParameter("cancelled", ProjectNextActionStatus.CANCELLED.getId());
            query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
            @SuppressWarnings("unchecked")
            List<ProjectActionNext> generatedActions = query.list();
            for (ProjectActionNext generatedAction : generatedActions) {
                generatedAction.setNextTimeEstimate(nextTimeEstimate);
                generatedAction.setNextChangeDate(new Date());
                dataSession.update(generatedAction);
            }

            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);
        for (PlanAheadBoardModel.TemplateCardModel templateCard : boardModel.getTemplateRow().getTemplateCards()) {
            if (templateCard.getTemplateActionNextId() == templateActionNextId) {
                result.getAffectedCellsHtml().put(
                        PlanAheadPageRenderer.templateLabelDomId(templateActionNextId),
                        renderer.renderTemplateRowLabelCellHtml(templateCard, boardModel.isWorkMode()));
                break;
            }
        }
        for (PlanAheadBoardModel.DayHeaderModel dayHeaderModel : boardModel.getDayHeaders()) {
            result.getAffectedHeadersHtml().put(
                    PlanAheadPageRenderer.dayHeaderDomId(dayHeaderModel.getDayKey()),
                    renderer.renderDayHeader(dayHeaderModel));
        }

        result.setSuccess(true);
        result.setMessage("Template estimate updated");
        return result;
    }

    public PlanAheadMutationResult loadTemplateEdit(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String actionNextIdString = n(appReq.getRequest().getParameter("actionNextId")).trim();
        int actionNextId = 0;
        if (actionNextIdString.length() > 0) {
            try {
                actionNextId = Integer.parseInt(actionNextIdString);
            } catch (NumberFormatException nfe) {
                result.setSuccess(false);
                result.setMessage("actionNextId must be a whole number");
                return result;
            }
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        Session dataSession = appReq.getDataSession();
        if (actionNextId <= 0) {
            data.put("isAdd", Boolean.TRUE);
            data.put("actionNextId", 0);
            data.put("nextActionType", ProjectNextActionType.WILL);
            data.put("timeSlot", TimeSlot.AFTERNOON.getId());
            data.put("templateType", TemplateType.DAILY.getId());
            data.put("nextDescription", "");
            data.put("nextTimeEstimate", 0);
            data.put("linkUrl", "");
            data.put("processStage", "");
            data.put("nextNote", "");
            data.put("projects", listProjectsForProvider(dataSession, appReq));
            result.setSuccess(true);
            result.setMessage("OK");
            result.setData(data);
            return result;
        }

        ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
        if (action == null) {
            result.setSuccess(false);
            result.setMessage("Template action not found");
            return result;
        }
        if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
            result.setSuccess(false);
            result.setMessage("Template action is not available for this workspace");
            return result;
        }
        if (!isActionCompatibleWithMode(action, appReq) || action.getTemplateType() == null) {
            result.setSuccess(false);
            result.setMessage(personalMode ? "Only personal templates are editable in Personal mode"
                    : "Only work templates are editable in Work mode");
            return result;
        }

        data.put("isAdd", Boolean.FALSE);
        data.put("actionNextId", action.getActionNextId());
        data.put("projectName", action.getProject() == null ? "" : n(action.getProject().getProjectName()));
        data.put("nextActionType", n(action.getNextActionType()));
        data.put("timeSlot", action.getTimeSlot() == null ? TimeSlot.AFTERNOON.getId() : action.getTimeSlot().getId());
        data.put("templateType", action.getTemplateType().getId());
        data.put("nextDescription", n(action.getNextDescription()));
        data.put("nextTimeEstimate", action.getNextTimeEstimate() == null ? 0 : action.getNextTimeEstimate());
        data.put("linkUrl", n(action.getLinkUrl()));
        data.put("processStage", action.getProcessStage() == null ? "" : n(action.getProcessStage().getId()));
        data.put("nextNote", n(action.getNextNotes()));
        result.setSuccess(true);
        result.setMessage("OK");
        result.setData(data);
        return result;
    }

    public PlanAheadMutationResult saveTemplateEdit(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String formMode = n(appReq.getRequest().getParameter("mode")).trim().toLowerCase();
        boolean isAdd = "add".equals(formMode);

        String nextActionType = n(appReq.getRequest().getParameter("nextActionType")).trim();
        if (!personalMode
                && (nextActionType.length() == 0
                        || boardService.resolveRowKeyForActionType(nextActionType).length() == 0)) {
            result.setSuccess(false);
            result.setMessage("nextActionType is invalid for template");
            return result;
        }
        TimeSlot templateTimeSlot = TimeSlot.getTimeSlot(n(appReq.getRequest().getParameter("timeSlot")).trim());
        if (personalMode && templateTimeSlot == null) {
            templateTimeSlot = TimeSlot.AFTERNOON;
        }

        TemplateType templateType = TemplateType
                .getTemplateType(n(appReq.getRequest().getParameter("templateType")).trim());
        if (templateType == null) {
            result.setSuccess(false);
            result.setMessage("templateType is invalid");
            return result;
        }

        Integer nextTimeEstimate = parseIntegerOrNull(appReq.getRequest().getParameter("nextTimeEstimate"));
        if (nextTimeEstimate != null && nextTimeEstimate.intValue() < 0) {
            result.setSuccess(false);
            result.setMessage("nextTimeEstimate cannot be negative");
            return result;
        }

        String processStageId = n(appReq.getRequest().getParameter("processStage")).trim();
        ProcessStage processStage = null;
        if (processStageId.length() > 0) {
            processStage = ProcessStage.getProcessStage(processStageId);
            if (processStage == null) {
                result.setSuccess(false);
                result.setMessage("processStage is invalid");
                return result;
            }
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext action;
            if (isAdd) {
                int projectId;
                try {
                    projectId = Integer.parseInt(n(appReq.getRequest().getParameter("projectId")).trim());
                } catch (NumberFormatException nfe) {
                    transaction.rollback();
                    result.setSuccess(false);
                    result.setMessage("projectId is required and must be a whole number");
                    return result;
                }
                Project project = findProjectById(dataSession, appReq, projectId);
                if (project == null) {
                    transaction.rollback();
                    result.setSuccess(false);
                    result.setMessage("Selected project is invalid");
                    return result;
                }

                action = new ProjectActionNext();
                action.setProjectId(project.getProjectId());
                action.setProject(project);
                action.setContactId(appReq.getWebUser().getContactId());
                action.setContact(appReq.getWebUser().getProjectContact());
                action.setWorkspaceId(appReq.getActiveWorkspaceId());
                action.setNextActionDate(null);
                action.setNextActionStatus(ProjectNextActionStatus.READY);
                action.setTemplateType(templateType);
                action.setTemplateActionNextId(null);
                action.setPriorityLevel(0);
                action.setCompletionOrder(0);
                action.setBillable(!personalMode ? true : false);
            } else {
                int actionNextId;
                try {
                    actionNextId = Integer.parseInt(n(appReq.getRequest().getParameter("actionNextId")).trim());
                } catch (NumberFormatException nfe) {
                    transaction.rollback();
                    result.setSuccess(false);
                    result.setMessage("actionNextId must be a whole number");
                    return result;
                }
                action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
                if (action == null) {
                    transaction.rollback();
                    result.setSuccess(false);
                    result.setMessage("Template action not found");
                    return result;
                }
                if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                    transaction.rollback();
                    result.setSuccess(false);
                    result.setMessage("Template action is not available for this workspace");
                    return result;
                }
                if (!isActionCompatibleWithMode(action, appReq) || action.getTemplateType() == null) {
                    transaction.rollback();
                    result.setSuccess(false);
                    result.setMessage(personalMode ? "Only personal templates are editable in Personal mode"
                            : "Only work templates are editable in Work mode");
                    return result;
                }
            }

            action.setNextActionType(personalMode ? ProjectNextActionType.WILL : nextActionType);
            action.setTemplateType(templateType);
            action.setProcessStage(processStage);
            action.setNextDescription(clip(n(appReq.getRequest().getParameter("nextDescription")), 12000));
            action.setNextTimeEstimate(personalMode ? Integer.valueOf(0)
                    : (nextTimeEstimate == null ? Integer.valueOf(0) : nextTimeEstimate));
            action.setTimeSlot(personalMode ? templateTimeSlot : null);
            action.setLinkUrl(clip(n(appReq.getRequest().getParameter("linkUrl")), 1200));
            action.setNextNotes(clip(n(appReq.getRequest().getParameter("nextNote")), 4000));
            action.setNextActionDate(null);
            action.setNextChangeDate(new Date());

            if (isAdd) {
                dataSession.save(action);
            } else {
                dataSession.update(action);

                Date today = stripToDate(appReq.getWebUser().getToday(), appReq);
                Query query = dataSession.createQuery(
                        "from ProjectActionNext pan where pan.workspaceId = :workspaceId "
                                + "and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                                + "and pan.billable = :billable "
                                + "and pan.templateActionNextId = :templateActionNextId "
                                + "and pan.nextActionDate is not null and pan.nextActionDate >= :today "
                                + "and pan.nextActionStatusString <> :cancelled "
                                + "and pan.nextActionStatusString <> :completed");
                query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
                query.setParameter("contactId", appReq.getWebUser().getContactId());
                query.setParameter("nextContactId", appReq.getWebUser().getContactId());
                query.setParameter("billable", !personalMode ? true : false);
                query.setParameter("templateActionNextId", action.getActionNextId());
                query.setParameter("today", today);
                query.setParameter("cancelled", ProjectNextActionStatus.CANCELLED.getId());
                query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
                @SuppressWarnings("unchecked")
                List<ProjectActionNext> generatedActions = query.list();
                for (ProjectActionNext generatedAction : generatedActions) {
                    generatedAction.setNextActionType(action.getNextActionType());
                    generatedAction.setNextDescription(action.getNextDescription());
                    generatedAction.setNextTimeEstimate(action.getNextTimeEstimate());
                    generatedAction.setTimeSlot(action.getTimeSlot());
                    generatedAction.setLinkUrl(action.getLinkUrl());
                    generatedAction.setProcessStage(action.getProcessStage());
                    generatedAction.setNextChangeDate(new Date());
                    dataSession.update(generatedAction);
                }
            }
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        result.setSuccess(true);
        result.setMessage(isAdd ? "Template created" : "Template saved");
        return result;
    }

    public PlanAheadMutationResult deleteTemplateEdit(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(appReq.getRequest().getParameter("actionNextId")).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action is not available for this workspace");
                return result;
            }
            if (!isActionCompatibleWithMode(action, appReq) || action.getTemplateType() == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage(personalMode ? "Only personal templates are editable in Personal mode"
                        : "Only work templates are editable in Work mode");
                return result;
            }

            action.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        result.setSuccess(true);
        result.setMessage("Template deleted");
        return result;
    }

    public PlanAheadMutationResult undoDeleteTemplate(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        String actionNextIdString = appReq.getRequest().getParameter("actionNextId");
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(n(actionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("actionNextId must be a whole number");
            return result;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(action.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action is not available for this workspace");
                return result;
            }

            action.setNextActionStatus(ProjectNextActionStatus.READY);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        result.setSuccess(true);
        result.setMessage("Template restored");
        return result;
    }

    public PlanAheadMutationResult toggleTemplateDay(AppReq appReq) {
        PlanAheadMutationResult result = new PlanAheadMutationResult();
        boolean personalMode = isPersonalMode(appReq);
        String templateActionNextIdString = appReq.getRequest().getParameter("templateActionNextId");
        int templateActionNextId;
        try {
            templateActionNextId = Integer.parseInt(n(templateActionNextIdString).trim());
        } catch (NumberFormatException nfe) {
            result.setSuccess(false);
            result.setMessage("templateActionNextId must be a whole number");
            return result;
        }

        Date day = parseDay(appReq.getRequest().getParameter("billDate"));
        if (day == null) {
            result.setSuccess(false);
            result.setMessage("billDate must be in yyyy-MM-dd format");
            return result;
        }
        boolean checked = parseBoolean(appReq.getRequest().getParameter("checked"));

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext templateAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
                    templateActionNextId);
            if (templateAction == null) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action not found");
                return result;
            }
            if (!isOwnedByCurrentWorkspace(templateAction.getWorkspaceId(), appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage("Template action is not available for this workspace");
                return result;
            }
            if (!isActionCompatibleWithMode(templateAction, appReq)) {
                transaction.rollback();
                result.setSuccess(false);
                result.setMessage(personalMode ? "Only personal templates are available in Personal mode"
                        : "Only work templates are available in Work mode");
                return result;
            }

            ProjectActionNext generatedAction = findGeneratedTemplateAction(dataSession, appReq, templateActionNextId,
                    day);
            Date today = stripToDate(appReq.getWebUser().getToday(), appReq);
            String todayKey = toDayKey(today);

            if (checked) {
                String nextActionType = n(templateAction.getNextActionType()).trim();
                if (nextActionType.length() == 0) {
                    nextActionType = ProjectNextActionType.WILL;
                }
                if (generatedAction == null) {
                    generatedAction = new ProjectActionNext();
                    generatedAction.setProjectId(templateAction.getProjectId());
                    generatedAction.setProject(templateAction.getProject());
                    generatedAction.setContactId(appReq.getWebUser().getContactId());
                    generatedAction.setContact(appReq.getWebUser().getProjectContact());
                    generatedAction.setWorkspaceId(appReq.getActiveWorkspaceId());
                    generatedAction.setNextActionDate(day);
                    generatedAction.setNextActionType(personalMode ? ProjectNextActionType.WILL : nextActionType);
                    generatedAction.setNextActionStatus(ProjectNextActionStatus.READY);
                    generatedAction.setTemplateActionNextId(templateAction.getActionNextId());
                    generatedAction.setPriorityLevel(templateAction.getPriorityLevel());
                    generatedAction.setCompletionOrder(0);
                    generatedAction.setBillable(templateAction.isBillable());
                    generatedAction.setNextContactId(templateAction.getNextContactId());
                    generatedAction.setNextProjectContact(templateAction.getNextProjectContact());
                    generatedAction.setProcessStage(templateAction.getProcessStage());
                    generatedAction.setTimeSlot(templateAction.getTimeSlot());
                    generatedAction.setLinkUrl(clip(n(templateAction.getLinkUrl()), 1200));
                    generatedAction.setNextNotes(clip(n(templateAction.getNextNotes()), 4000));
                    generatedAction.setNextChangeDate(new Date());
                    generatedAction.setNextDescription(templateAction.getNextDescription());
                    generatedAction.setNextTimeEstimate(templateAction.getNextTimeEstimate() == null
                            ? Integer.valueOf(0)
                            : templateAction.getNextTimeEstimate());
                    dataSession.save(generatedAction);
                } else {
                    generatedAction.setNextActionStatus(ProjectNextActionStatus.READY);
                    generatedAction.setNextActionDate(day);
                    generatedAction.setNextActionType(personalMode ? ProjectNextActionType.WILL : nextActionType);
                    generatedAction.setProcessStage(templateAction.getProcessStage());
                    generatedAction.setLinkUrl(clip(n(templateAction.getLinkUrl()), 1200));
                    generatedAction.setNextNotes(clip(n(templateAction.getNextNotes()), 4000));
                    generatedAction.setNextDescription(templateAction.getNextDescription());
                    generatedAction.setNextTimeEstimate(templateAction.getNextTimeEstimate() == null
                            ? Integer.valueOf(0)
                            : templateAction.getNextTimeEstimate());
                    generatedAction.setNextChangeDate(new Date());
                    dataSession.update(generatedAction);
                }
            } else {
                if (generatedAction != null) {
                    String dayKey = toDayKey(day);
                    if (dayKey.compareTo(todayKey) < 0) {
                        transaction.rollback();
                        result.setSuccess(false);
                        result.setMessage("Cannot modify template actions in past days");
                        return result;
                    }
                    if (dayKey.equals(todayKey)) {
                        generatedAction.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                        generatedAction.setNextChangeDate(new Date());
                        dataSession.update(generatedAction);
                    } else {
                        dataSession.delete(generatedAction);
                    }
                }
            }

            transaction.commit();
        } catch (RuntimeException re) {
            transaction.rollback();
            throw re;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);
        String dayKey = toDayKey(day);

        PlanAheadBoardModel.DayHeaderModel targetDayHeader = null;
        for (PlanAheadBoardModel.DayHeaderModel dayHeaderModel : boardModel.getDayHeaders()) {
            if (dayKey.equals(dayHeaderModel.getDayKey())) {
                targetDayHeader = dayHeaderModel;
                break;
            }
        }
        if (targetDayHeader != null) {
            for (PlanAheadBoardModel.TemplateCardModel templateCard : boardModel.getTemplateRow().getTemplateCards()) {
                if (templateCard.getTemplateActionNextId() == templateActionNextId) {
                    String domId = PlanAheadPageRenderer.templateDayDomId(templateActionNextId, dayKey);
                    result.getAffectedCellsHtml().put(domId,
                            renderer.renderTemplateSelectionCellHtml(templateCard, targetDayHeader));
                    break;
                }
            }
        }
        for (PlanAheadBoardModel.DayHeaderModel dayHeaderModel : boardModel.getDayHeaders()) {
            if (dayKey.equals(dayHeaderModel.getDayKey())) {
                result.getAffectedHeadersHtml().put(
                        PlanAheadPageRenderer.dayHeaderDomId(dayHeaderModel.getDayKey()),
                        renderer.renderDayHeader(dayHeaderModel));
                break;
            }
        }
        result.setSuccess(true);
        result.setMessage("Template updated");
        return result;
    }

    private ProjectActionNext findGeneratedTemplateAction(Session dataSession, AppReq appReq, int templateActionNextId,
            Date day) {
        boolean billable = !isPersonalMode(appReq);
        Query query = dataSession.createQuery(
                "from ProjectActionNext pan where pan.workspaceId = :workspaceId "
                        + "and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.billable = :billable "
                        + "and pan.templateActionNextId = :templateActionNextId and pan.nextActionDate = :nextActionDate");
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("billable", billable);
        query.setParameter("templateActionNextId", templateActionNextId);
        query.setParameter("nextActionDate", day);
        @SuppressWarnings("unchecked")
        java.util.List<ProjectActionNext> list = query.list();
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    private List<Map<String, Object>> listProjectsForProvider(Session dataSession, AppReq appReq) {
        Query query = dataSession.createQuery(
                "from Project p where p.workspaceId = :workspaceId "
                        + "and (p.phaseCode is null or p.phaseCode = :phaseCode) "
                        + "order by p.priorityLevel desc, p.projectName");
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        query.setParameter("phaseCode", PHASE_ACTIVE);
        @SuppressWarnings("unchecked")
        List<Project> projects = query.list();
        List<Project> filteredProjects = filterProjectsForTemplateMode(projects,
                loadBillCodesForProvider(dataSession, appReq), isPersonalMode(appReq));
        List<Map<String, Object>> projectList = new ArrayList<Map<String, Object>>();
        for (Project project : filteredProjects) {
            Map<String, Object> p = new LinkedHashMap<String, Object>();
            p.put("projectId", project.getProjectId());
            p.put("projectName", n(project.getProjectName()));
            projectList.add(p);
        }
        return projectList;
    }

    Map<String, BillCode> loadBillCodesForProvider(Session dataSession, AppReq appReq) {
        Query query = dataSession.createQuery("from BillCode bc where bc.workspaceId = :workspaceId");
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        @SuppressWarnings("unchecked")
        List<BillCode> billCodes = query.list();
        Map<String, BillCode> billCodeMap = new HashMap<String, BillCode>();
        for (BillCode billCode : billCodes) {
            if (billCode != null && billCode.getBillCode() != null) {
                billCodeMap.put(billCode.getBillCode(), billCode);
            }
        }
        return billCodeMap;
    }

    List<Project> filterProjectsForTemplateMode(List<Project> projects, Map<String, BillCode> billCodeMap,
            boolean personalMode) {
        if (projects == null || projects.isEmpty()) {
            return Collections.emptyList();
        }
        List<Project> filteredProjects = new ArrayList<Project>();
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            boolean billableProject = isBillableProject(project, billCodeMap);
            if (personalMode ? !billableProject : billableProject) {
                filteredProjects.add(project);
            }
        }
        return filteredProjects;
    }

    boolean isBillableProject(Project project, Map<String, BillCode> billCodeMap) {
        if (project == null || project.getBillCode() == null || project.getBillCode().trim().length() == 0) {
            return false;
        }
        BillCode billCode = billCodeMap == null ? null : billCodeMap.get(project.getBillCode());
        return billCode != null && BILLABLE_YES.equalsIgnoreCase(n(billCode.getBillable()).trim());
    }

    private Project findProjectById(Session dataSession, AppReq appReq, int projectId) {
        Query query = dataSession.createQuery(
                "from Project p where p.projectId = :projectId and p.workspaceId = :workspaceId");
        query.setParameter("projectId", projectId);
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        @SuppressWarnings("unchecked")
        List<Project> projects = query.list();
        if (projects.size() > 0) {
            return projects.get(0);
        }
        return null;
    }

    private Date parseDay(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        sdf.setTimeZone(UTC_TIME_ZONE);
        try {
            return sdf.parse(value.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private Date parseOptionalDay(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return parseDay(value);
    }

    private Date stripToDate(Date date, AppReq appReq) {
        java.util.Calendar calendar = appReq.getWebUser().getCalendar();
        calendar.setTime(date);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String toDayKey(Date day) {
        if (day == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(UTC_TIME_ZONE);
        return sdf.format(day);
    }

    private boolean isBeforeDay(Date day, String referenceDayKey) {
        if (day == null || referenceDayKey == null) {
            return false;
        }
        String dayKey = toDayKey(day);
        if (dayKey.length() == 0 || referenceDayKey.length() == 0) {
            return false;
        }
        return dayKey.compareTo(referenceDayKey) < 0;
    }

    private boolean isOwnedByCurrentWorkspace(Integer actionWorkspaceId, AppReq appReq) {
        if (actionWorkspaceId == null || appReq.getWebUser() == null || appReq.getActiveWorkspaceId() == null) {
            return false;
        }
        return actionWorkspaceId.equals(appReq.getActiveWorkspaceId());
    }

    private String n(String value) {
        return value == null ? "" : value;
    }

    private Integer parseIntegerOrNull(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() <= max) {
            return v;
        }
        return v.substring(0, max);
    }

    private boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    private String resolveMode(AppReq appReq) {
        return boardService.resolveMode(appReq);
    }

    private boolean isPersonalMode(AppReq appReq) {
        return PlanAheadBoardService.MODE_PERSONAL.equalsIgnoreCase(resolveMode(appReq));
    }

    private boolean isActionCompatibleWithMode(ProjectActionNext action, AppReq appReq) {
        if (action == null) {
            return false;
        }
        boolean personalMode = isPersonalMode(appReq);
        return personalMode ? !action.isBillable() : action.isBillable();
    }

    private String resolveRowKeyForAction(ProjectActionNext action, AppReq appReq) {
        if (action == null) {
            return "";
        }
        if (isPersonalMode(appReq)) {
            return boardService.resolveRowKeyForTimeSlot(action.getTimeSlot());
        }
        return boardService.resolveRowKeyForActionType(action.getNextActionType());
    }
}
