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
import org.openimmunizationsoftware.pt.model.WebUser;

public class TimeTracker {
  private int totalMins = 0;
  private WebUser webUser = null;
  private boolean runningClock = false;
  private BillEntry billEntry = null;
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

  public synchronized HashMap<Integer, Integer> getTotalMinsForProjectMap() {
    HashMap<Integer, Integer> totalMinsForProjectMapCopy =
        new HashMap<Integer, Integer>(totalMinsForProjectMap);
    if (billEntry != null) {
      if (billEntry.getProjectId() > 0) {
        Integer mins = totalMinsForProjectMapCopy.get(billEntry.getProjectId());
        int m = 0;
        if (mins != null) {
          m = mins;
        }
        m += billEntry.getBillMins();
        totalMinsForProjectMapCopy.put(billEntry.getProjectId(), m);
      }
    }
    return totalMinsForProjectMapCopy;
  }

  public synchronized HashMap<String, Integer> getTotalMinsForClientMap() {
    HashMap<String, Integer> totalMinsForClientMapCopy =
        new HashMap<String, Integer>(totalMinsForClientMap);
    if (billEntry != null) {
      if (billEntry.getClientCode() != null) {
        Integer mins = totalMinsForClientMapCopy.get(billEntry.getClientCode());
        int m = 0;
        if (mins != null) {
          m = mins;
        }
        m += billEntry.getBillMins();
        totalMinsForClientMapCopy.put(billEntry.getClientCode(), m);
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
    if (billEntry != null && billEntry.getBillable() != null
        && billEntry.getBillable().equals("Y")) {
      return totalMins + billEntry.getBillMins();
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
    Calendar t = Calendar.getInstance();
    t.setTime(date);
    removeTime(t);
    init(webUser, dataSession, t);
  }

  public TimeTracker(WebUser webUser, Date date, int calendarField, Session dataSession) {
    this.webUser = webUser;
    if (webUser.getParentWebUser() != null) {
      this.webUser = webUser.getParentWebUser();
    }
    Calendar t = Calendar.getInstance();
    t.setTime(date);
    removeTime(t);
    init(webUser, dataSession, t, calendarField);

  }

  public void init(WebUser webUser, Session dataSession) {
    Calendar t = createToday();
    init(webUser, dataSession, t);
  }

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
      t.set(Calendar.MONTH, Calendar.JANUARY);
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
    if (billEntry.getClientCode() != null) {
      Integer mins = totalMinsForClientMap.get(billEntry.getClientCode());
      int m = 0;
      if (mins != null) {
        m = mins;
      }
      m += billEntry.getBillMins();
      totalMinsForClientMap.put(billEntry.getClientCode(), m);
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

  public synchronized void update(Project project, Session dataSession) {
    if (runningClock) {
      if (billEntry != null) {
        saveTime(dataSession);
        if (billEntry.getProjectId() != project.getProjectId()) {
          startTime(project, dataSession);
        }
      } else {
        startTime(project, dataSession);
      }
    }
  }

  public synchronized void startClock(Project project, Session dataSession) {
    if (runningClock) {
      saveTime(dataSession);
      if (billEntry.getProjectId() != project.getProjectId()) {
        startTime(project, dataSession);
      }
    } else {
      startTime(project, dataSession);
      runningClock = true;
    }

  }

  public synchronized void stopClock(Session dataSession) {
    if (runningClock) {
      if (billEntry != null) {
        saveTime(dataSession);
        addToTotals(billEntry);
        billEntry = null;
      }
      runningClock = false;
    }
  }

  private void startTime(Project project, Session dataSession) {
    if (billEntry != null) {
      addToTotals(billEntry);
    }
    billEntry = null;

    if (project.getBillCode() != null && !project.getBillCode().equals("")) {
      BillCode billCode = (BillCode) dataSession.get(BillCode.class, project.getBillCode());
      if (billCode != null) {
        billEntry = new BillEntry();
        billEntry.setProjectId(project.getProjectId());
        billEntry.setClientCode(project.getClientCode());
        billEntry.setUsername(webUser.getUsername());
        billEntry.setStartTime(new Date());
        billEntry.setEndTime(new Date());
        billEntry.setBillMins(0);
        billEntry.setBillable(billCode.getBillable());
        billEntry.setBillCode(billCode.getBillCode());
        billEntry.setProviderId(webUser.getProviderId());
        Transaction trans = dataSession.beginTransaction();
        try {
          dataSession.save(billEntry);
        } finally {
          trans.commit();
        }
      }
    }
  }

  private void saveTime(Session dataSession) {
    billEntry.setEndTime(new Date());
    billEntry.setBillMins(calculateMins(billEntry));
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

  public static Calendar createToday() {
    Calendar calendar = Calendar.getInstance();
    removeTime(calendar);
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

  public static String formatTime(int min) {
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
}
