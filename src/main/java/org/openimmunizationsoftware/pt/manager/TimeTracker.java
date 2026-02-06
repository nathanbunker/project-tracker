package org.openimmunizationsoftware.pt.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.WebUser;

public class TimeTracker {
  private int totalMins = 0;
  private WebUser webUser = null;
  private boolean runningClock = false;
  private Integer billEntryId = null;
  private int billEntryProjectId = 0;
  private String billEntryCategoryCode = null;
  private String billEntryBillable = null;
  private String billEntryBillCode = null;
  private Integer billEntryActionId = null;
  private Date billEntryStartTime = null;
  private Date billEntryEndTime = null;
  private Integer billEntryBillMins = null;
  private HashMap<Integer, Integer> totalMinsForProjectMap;
  private HashMap<String, Integer> totalMinsForClientMap;
  private HashMap<String, Integer> totalMinsForBillCodeMap;
  private Date startDate = null;
  private Date endDate = null;

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public HashMap<String, Integer> getTotalMinsForBillCodeMap() {
    return totalMinsForBillCodeMap;
  }

  public List<TimeEntry> createTimeEntryList() {
    List<TimeEntry> timeEntryList;
    timeEntryList = new ArrayList<TimeEntry>();
    for (String billCodeString : totalMinsForBillCodeMap.keySet()) {
      timeEntryList.add(new TimeEntry(billCodeString, totalMinsForBillCodeMap.get(billCodeString),
          billCodeString));
    }
    Collections.sort(timeEntryList);
    return timeEntryList;
  }

  public synchronized int getTotalMinsForAction(ProjectAction projectAction) {
    int totalMins = 0;
    if (hasRunningEntry()) {
      if (billEntryActionId != null && projectAction != null
          && billEntryActionId.intValue() == projectAction.getActionId()) {
        totalMins += billEntryBillMins == null ? 0 : billEntryBillMins;
      }
    }
    return totalMins;
  }

  public synchronized int getTotalMinsForProject(Project project) {
    int totalMins = 0;
    if (hasRunningEntry()) {
      if (billEntryProjectId == project.getProjectId()) {
        totalMins += billEntryBillMins == null ? 0 : billEntryBillMins;
      }
    }
    return totalMins;
  }

  public synchronized HashMap<Integer, Integer> getTotalMinsForProjectMap() {
    HashMap<Integer, Integer> totalMinsForProjectMapCopy = new HashMap<Integer, Integer>(totalMinsForProjectMap);
    if (hasRunningEntry()) {
      if (billEntryProjectId > 0) {
        Integer mins = totalMinsForProjectMapCopy.get(billEntryProjectId);
        int m = 0;
        if (mins != null) {
          m = mins;
        }
        m += billEntryBillMins == null ? 0 : billEntryBillMins;
        totalMinsForProjectMapCopy.put(billEntryProjectId, m);
      }
    }
    return totalMinsForProjectMapCopy;
  }

  public synchronized HashMap<String, Integer> getTotalMinsForClientMap() {
    HashMap<String, Integer> totalMinsForClientMapCopy = new HashMap<String, Integer>(totalMinsForClientMap);
    if (hasRunningEntry()) {
      if (billEntryCategoryCode != null) {
        Integer mins = totalMinsForClientMapCopy.get(billEntryCategoryCode);
        int m = 0;
        if (mins != null) {
          m = mins;
        }
        m += billEntryBillMins == null ? 0 : billEntryBillMins;
        totalMinsForClientMapCopy.put(billEntryCategoryCode, m);
      }
    }
    return totalMinsForClientMapCopy;
  }

  public boolean isRunningClock() {
    return runningClock;
  }

  public void setRunningClock(boolean runningClock) {
    this.runningClock = runningClock;
  }

  public int getTotalMinsBillable() {
    if (hasRunningEntry() && billEntryBillable != null && billEntryBillable.equals("Y")) {
      return totalMins + (billEntryBillMins == null ? 0 : billEntryBillMins);
    }
    return totalMins;
  }

  public String getTotalMinsBillableForDisplay() {
    return formatTime(getTotalMinsBillable());
  }

  public void setTotalMins(int totalMins) {
    this.totalMins = totalMins;
  }

  public TimeTracker(WebUser webUser, Session dataSession) {
    this.webUser = webUser;
    init(webUser, dataSession);
  }

  public TimeTracker(WebUser webUser, Date date, Session dataSession) {
    this.webUser = webUser;
    Calendar t = webUser.getCalendar();
    t.setTime(date);
    removeTime(t);
    init(webUser, dataSession, t);
  }

  public TimeTracker(WebUser webUser, Date date, int calendarField, Session dataSession) {
    this.webUser = webUser;
    if (webUser.getParentWebUser() != null) {
      this.webUser = webUser.getParentWebUser();
    }
    Calendar t = webUser.getCalendar();
    t.setTime(date);
    removeTime(t);
    init(webUser, dataSession, t, calendarField);

  }

  public void init(WebUser webUser, Session dataSession) {
    Calendar t = createToday(webUser);
    init(webUser, dataSession, t);
  }

  @SuppressWarnings("unchecked")
  private synchronized void init(WebUser webUser, Session dataSession, Calendar t) {
    totalMinsForProjectMap = new HashMap<Integer, Integer>();
    totalMinsForClientMap = new HashMap<String, Integer>();
    totalMinsForBillCodeMap = new HashMap<String, Integer>();
    totalMins = 0;
    Date today = t.getTime();
    t.add(Calendar.DAY_OF_MONTH, 1);
    Date tomorrow = t.getTime();
    startDate = today;
    endDate = tomorrow;
    Query query = dataSession
        .createQuery("from BillEntry where username = ? and startTime >= ? and startTime < ?");
    query.setParameter(0, webUser.getUsername());
    query.setParameter(1, today);
    query.setParameter(2, tomorrow);
    List<BillEntry> billEntryList = query.list();
    for (BillEntry billEntry : billEntryList) {
      addToTotals(billEntry);
    }
  }

  @SuppressWarnings("unchecked")
  private synchronized void init(WebUser webUser, Session dataSession, Calendar t,
      int calendarField) {
    totalMinsForProjectMap = new HashMap<Integer, Integer>();
    totalMinsForClientMap = new HashMap<String, Integer>();
    totalMinsForBillCodeMap = new HashMap<String, Integer>();
    totalMins = 0;
    Date today;
    if (calendarField == Calendar.WEEK_OF_YEAR) {
      while (t.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
        t.add(Calendar.DAY_OF_MONTH, -1);
      }
      today = t.getTime();
      t.add(Calendar.DAY_OF_MONTH, 7);
    } else if (calendarField == Calendar.MONTH) {
      t.set(Calendar.DAY_OF_MONTH, 1);
      today = t.getTime();
      t.add(Calendar.MONTH, 1);
    } else if (calendarField == Calendar.YEAR) {
      t.set(Calendar.DAY_OF_MONTH, 1);
      t.add(Calendar.YEAR, -1);
      today = t.getTime();
      t.add(Calendar.YEAR, 1);
    } else {
      today = t.getTime();
      t.add(Calendar.DAY_OF_MONTH, 1);
    }
    Date tomorrow = t.getTime();
    startDate = today;
    endDate = tomorrow;
    Query query = dataSession
        .createQuery("from BillEntry where username = ? and startTime >= ? and startTime < ?");
    query.setParameter(0, webUser.getUsername());
    query.setParameter(1, today);
    query.setParameter(2, tomorrow);
    List<BillEntry> billEntryList = query.list();
    for (BillEntry billEntry : billEntryList) {
      addToTotals(billEntry);
    }
  }

  private void addToTotals(BillEntry billEntry) {
    if (billEntry.getBillable() != null && billEntry.getBillable().equals("Y")) {
      totalMins += billEntry.getBillMins();
    }
    if (billEntry.getProjectId() > 0) {
      Integer mins = totalMinsForProjectMap.get(billEntry.getProjectId());
      int m = 0;
      if (mins != null) {
        m = mins;
      }
      m += billEntry.getBillMins();
      totalMinsForProjectMap.put(billEntry.getProjectId(), m);
    }
    if (billEntry.getCategoryCode() != null) {
      Integer mins = totalMinsForClientMap.get(billEntry.getCategoryCode());
      int m = 0;
      if (mins != null) {
        m = mins;
      }
      m += billEntry.getBillMins();
      totalMinsForClientMap.put(billEntry.getCategoryCode(), m);
    }
    if (billEntry.getBillCode() != null && billEntry.getBillable().equals("Y")) {
      Integer mins = totalMinsForBillCodeMap.get(billEntry.getBillCode());
      int m = 0;
      if (mins != null) {
        m = mins;
      }
      m += billEntry.getBillMins();
      totalMinsForBillCodeMap.put(billEntry.getBillCode(), m);
    }

  }

  public synchronized void update(Project project, ProjectAction action, Session dataSession) {
    if (runningClock) {
      if (hasRunningEntry()) {
        saveTime(dataSession);
        if (projectOrActionChanged(project, action)) {
          startTime(project, action, dataSession);
        }
      } else {
        startTime(project, action, dataSession);
      }
    }
  }

  public synchronized void update(ProjectAction projectAction, Session dataSession) {
    if (runningClock) {
      Project project = projectAction.getProject();
      if (hasRunningEntry()) {
        saveTime(dataSession);
        if (projectOrActionChanged(project, projectAction)) {
          startTime(project, projectAction, dataSession);
        }
      } else {
        startTime(project, projectAction, dataSession);
      }
    }
  }

  public synchronized void startClock(Project project, ProjectAction action, Session dataSession) {
    if (runningClock) {
      saveTime(dataSession);
      if (projectOrActionChanged(project, action)) {
        startTime(project, action, dataSession);
      }
    } else {
      startTime(project, action, dataSession);
      runningClock = true;
    }
  }

  private boolean projectOrActionChanged(Project project, ProjectAction action) {
    if (billEntryProjectId != project.getProjectId()) {
      return true;
    }
    if (action == null && billEntryActionId == null) {
      return false;
    }
    if (action == null || billEntryActionId == null) {
      return true;
    }
    return billEntryActionId.intValue() != action.getActionId();
  }

  public synchronized void stopClock(Session dataSession) {
    if (runningClock) {
      if (hasRunningEntry()) {
        saveTime(dataSession);
        addRunningEntryToTotals();
        clearRunningEntry();
      }
      runningClock = false;
    }
  }

  private void startTime(Project project, ProjectAction action, Session dataSession) {
    if (hasRunningEntry()) {
      addRunningEntryToTotals();
    }
    clearRunningEntry();

    if (project.getBillCode() != null && !project.getBillCode().equals("")) {
      BillCode billCode = (BillCode) dataSession.get(BillCode.class, project.getBillCode());
      if (billCode != null) {
        BillEntry billEntry = new BillEntry();
        billEntry.setProjectId(project.getProjectId());
        billEntry.setCategoryCode(project.getCategoryCode());
        billEntry.setAction(action);
        billEntry.setUsername(webUser.getUsername());
        billEntry.setStartTime(new Date());
        billEntry.setEndTime(new Date());
        billEntry.setBillMins(0);
        billEntry.setBillable(billCode.getBillable());
        billEntry.setBillCode(billCode.getBillCode());
        billEntry.setProvider(webUser.getProvider());
        Transaction trans = dataSession.beginTransaction();
        try {
          dataSession.save(billEntry);
        } finally {
          trans.commit();
        }
        billEntryId = billEntry.getBillId();
        billEntryProjectId = billEntry.getProjectId();
        billEntryCategoryCode = billEntry.getCategoryCode();
        billEntryBillable = billEntry.getBillable();
        billEntryBillCode = billEntry.getBillCode();
        billEntryActionId = action == null ? null : action.getActionId();
        billEntryStartTime = billEntry.getStartTime();
        billEntryEndTime = billEntry.getEndTime();
        billEntryBillMins = billEntry.getBillMins();
      }
    }
  }

  private void saveTime(Session dataSession) {
    if (!hasRunningEntry()) {
      return;
    }
    if (billEntryStartTime == null) {
      return;
    }
    billEntryEndTime = new Date();
    billEntryBillMins = calculateMins(billEntryStartTime, billEntryEndTime);
    BillEntry billEntry = (BillEntry) dataSession.get(BillEntry.class, billEntryId);
    if (billEntry == null) {
      clearRunningEntry();
      return;
    }
    billEntry.setEndTime(billEntryEndTime);
    billEntry.setBillMins(billEntryBillMins);
    Transaction trans = dataSession.beginTransaction();
    try {
      dataSession.saveOrUpdate(billEntry);
    } finally {
      trans.commit();
    }
  }

  public static int calculateMins(BillEntry billEntry) {
    long elapsedTime = billEntry.getEndTime().getTime() - billEntry.getStartTime().getTime();
    int mins = (int) (elapsedTime / 60000.0 + 0.5);
    return mins;
  }

  private static int calculateMins(Date startTime, Date endTime) {
    long elapsedTime = endTime.getTime() - startTime.getTime();
    return (int) (elapsedTime / 60000.0 + 0.5);
  }

  public static Calendar createToday(WebUser webUser) {
    Calendar calendar = webUser.getCalendar();
    removeTime(calendar);
    return calendar;
  }

  // createTomorow
  public static Calendar createTomorrow(WebUser webUser) {
    Calendar calendar = webUser.getCalendar();
    removeTime(calendar);
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    return calendar;
  }

  private static void removeTime(Calendar calendar) {
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
  }

  public static int readTime(String timeString) {
    int min = 0;
    int pos = timeString.indexOf(":");
    if (pos != -1) {
      min = Integer.parseInt(timeString.substring(0, pos)) * 60
          + Integer.parseInt(timeString.substring(pos + 1));
    } else {
      min = Integer.parseInt(timeString);
    }
    return min;
  }

  public static String formatTime(Integer min) {
    if (min == null) {
      return "0:00";
    }
    int hour = min / 60;
    min = min % 60;
    if (min < 0) {
      min = -min;
    }
    if (min < 10) {
      return hour + ":0" + min;
    } else {
      return hour + ":" + min;
    }
  }

  public static int roundTime(int min, BillCode billCode) {
    return billCode.getBillRound()
        * (int) ((min + billCode.getBillRound() / 2) / billCode.getBillRound());
  }

  private boolean hasRunningEntry() {
    return billEntryId != null;
  }

  private void clearRunningEntry() {
    billEntryId = null;
    billEntryProjectId = 0;
    billEntryCategoryCode = null;
    billEntryBillable = null;
    billEntryBillCode = null;
    billEntryActionId = null;
    billEntryStartTime = null;
    billEntryEndTime = null;
    billEntryBillMins = null;
  }

  private void addRunningEntryToTotals() {
    if (!hasRunningEntry()) {
      return;
    }
    int runningMins = billEntryBillMins == null ? 0 : billEntryBillMins;
    if (billEntryBillable != null && billEntryBillable.equals("Y")) {
      totalMins += runningMins;
    }
    if (billEntryProjectId > 0) {
      Integer mins = totalMinsForProjectMap.get(billEntryProjectId);
      int m = 0;
      if (mins != null) {
        m = mins;
      }
      m += runningMins;
      totalMinsForProjectMap.put(billEntryProjectId, m);
    }
    if (billEntryCategoryCode != null) {
      Integer mins = totalMinsForClientMap.get(billEntryCategoryCode);
      int m = 0;
      if (mins != null) {
        m = mins;
      }
      m += runningMins;
      totalMinsForClientMap.put(billEntryCategoryCode, m);
    }
    if (billEntryBillCode != null && billEntryBillable != null && billEntryBillable.equals("Y")) {
      Integer mins = totalMinsForBillCodeMap.get(billEntryBillCode);
      int m = 0;
      if (mins != null) {
        m = mins;
      }
      m += runningMins;
      totalMinsForBillCodeMap.put(billEntryBillCode, m);
    }
  }
}
