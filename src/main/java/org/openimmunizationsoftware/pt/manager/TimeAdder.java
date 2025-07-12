package org.openimmunizationsoftware.pt.manager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

public class TimeAdder {

    private int completedAct = 0;
    private int committedEst = 0;
    private int committedAct = 0;
    private int willEst = 0;
    private int willAct = 0;
    private int willMeetEst = 0;
    private int willMeetAct = 0;
    private int mightEst = 0;
    private int mightAct = 0;
    private int otherEst = 0;
    private int otherAct = 0;

    public int getCommittedAct() {
        return committedAct;
    }

    public int getCompletedAct() {
        return completedAct;
    }

    public int getCommittedEst() {
        return committedEst;
    }

    public int getWillAct() {
        return willAct;
    }

    public int getWillEst() {
        return willEst;
    }

    public int getWillMeetAct() {
        return willMeetAct;
    }

    public int getWillMeetEst() {
        return willMeetEst;
    }

    public int getMightAct() {
        return mightAct;
    }

    public int getMightEst() {
        return mightEst;
    }

    public int getOtherAct() {
        return otherAct;
    }

    public int getOtherEst() {
        return otherEst;
    }

    public TimeAdder(List<ProjectAction> projectActionList, AppReq appReq) {
        this(projectActionList, appReq, null);
    }

    public TimeAdder(List<ProjectAction> projectActionList, AppReq appReq, Date evaluationDate) {
        boolean isToday = true;
        if (evaluationDate == null) {
            evaluationDate = new Date();
        } else {
            isToday = false;
        }

        WebUser webUser = appReq.getWebUser();
        Calendar cIndicated = webUser.getCalendar();
        cIndicated.setTime(evaluationDate);
        completedAct = 0;
        if (isToday) {
            TimeTracker timeTracker = appReq.getTimeTracker();
            if (timeTracker != null) {
                completedAct = timeTracker.getTotalMinsBillable();
            }
        }
        committedEst = 0;
        willEst = 0;
        willMeetEst = 0;
        mightEst = 0;
        otherEst = 0;
        for (ProjectAction pa : projectActionList) {
            if (isToday && !webUser.sameDay(cIndicated, pa.getNextDue())) {
                continue;
            }
            if (pa.getNextTimeEstimate() != null) {
                int nextTimeEstimate = pa.getNextTimeEstimate();
                int nextTimeActual = pa.getNextTimeActual() == null ? 0 : pa.getNextTimeActual();
                if (nextTimeActual <= nextTimeEstimate) {
                    nextTimeEstimate = nextTimeEstimate - nextTimeActual;
                } else {
                    nextTimeEstimate = nextTimeActual;
                }
                if (ProjectNextActionType.COMMITTED_TO.equals(pa.getNextActionType())
                        || ProjectNextActionType.OVERDUE_TO.equals(pa.getNextActionType())) {
                    committedEst += nextTimeEstimate;
                } else if (ProjectNextActionType.WILL.equals(pa.getNextActionType())
                        || ProjectNextActionType.WILL_CONTACT.equals(pa.getNextActionType())
                        || ProjectNextActionType.WILL_DOCUMENT.equals(pa.getNextActionType())
                        || ProjectNextActionType.WILL_FOLLOW_UP.equals(pa.getNextActionType())
                        || ProjectNextActionType.WILL_REVIEW.equals(pa.getNextActionType())) {
                    willEst += nextTimeEstimate;
                } else if (ProjectNextActionType.WILL_MEET.equals(pa.getNextActionType())) {
                    willMeetEst += nextTimeEstimate;
                } else if (ProjectNextActionType.MIGHT.equals(pa.getNextActionType())) {
                    mightEst += nextTimeEstimate;
                } else {
                    otherEst += nextTimeEstimate;
                }
            }
        }
        committedAct = completedAct + committedEst;
        willMeetAct = committedAct + willMeetEst;
        willAct = willMeetAct + willEst;
        mightAct = willAct + mightEst;
        otherAct = mightAct + otherEst;
    }

}
