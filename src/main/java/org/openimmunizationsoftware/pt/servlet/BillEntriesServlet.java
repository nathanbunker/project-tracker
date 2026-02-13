/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillEntriesServlet extends ClientServlet {

  private static final Logger LOGGER = Logger.getLogger(BillEntriesServlet.class.getName());
  private static final long SAFETY_MILLIS = 2L * 60L * 1000L;

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AppReq appReq = new AppReq(request, response);
    try {
      WebUser webUser = appReq.getWebUser();
      if (appReq.isLoggedOut() || appReq.isDependentWebUser()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getDateFormat();

      String billDateString = request.getParameter("billDate");
      Date billDate = null;
      if ((billDateString != null && billDateString.length() > 0)) {
        try {
          billDate = sdf.parse(billDateString);
        } catch (ParseException pe) {
          appReq.setMessageProblem("Unable to parse date: " + pe.getMessage());
        }
      } else {
        billDateString = sdf.format(new Date());
      }

      appReq.setTitle("Time");
      printHtmlHead(appReq);

      Date previousBillDate = getPreviousBillDate(webUser, dataSession, billDate);

      out.println("<form action=\"BillEntriesServlet\" method=\"GET\">");
      out.println("Date");
      out.println(
          "<input type=\"text\" name=\"billDate\" value=\"" + billDateString + "\" size=\"10\">");
      out.println("<input type=\"submit\" name=\"action\" value=\"Refresh\">");
      if (previousBillDate != null) {
        out.println("<a class=\"button\" href=\"BillEntriesServlet?billDate="
            + sdf.format(previousBillDate) + "\">Previous Day</a>");
      }
      out.println("</form>");

      Query query;

      Calendar t = TimeTracker.createToday(webUser);
      if (billDate != null) {
        t.setTime(billDate);
      }
      Date today = t.getTime();
      t.add(Calendar.DAY_OF_MONTH, 1);
      Date tomorrow = t.getTime();
      query = dataSession.createQuery(
          "from BillEntry where username = ? and startTime >= ? and startTime < ? order by startTime");
      query.setParameter(0, webUser.getUsername());
      query.setParameter(1, today);
      query.setParameter(2, tomorrow);
      @SuppressWarnings("unchecked")
      List<BillEntry> billEntryList = query.list();
      TimeTracker timeTracker = appReq.getTimeTracker();
      Integer lockedBillEntryId = timeTracker == null ? null : timeTracker.getBillEntryId();

      SimpleDateFormat timeFormat = webUser.getDateFormat("h:mm aaa");
      if (billEntryList.isEmpty()) {
        out.println("<p>No bill entries.</p>");
      } else {
        normalizeBillEntriesForDay(webUser, dataSession, billEntryList, today, tomorrow);
        out.println("<h2>" + webUser.getDateFormat("EEEE MM-dd-yyyy").format(today) + "</h2>");
        printBillEntriesSection(out, dataSession, billEntryList, lockedBillEntryId, billDateString,
            timeFormat);
      }
      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private Date getPreviousBillDate(WebUser webUser, Session dataSession, Date billDate) {
    Calendar calendar = TimeTracker.createToday(webUser);
    if (billDate != null) {
      calendar.setTime(billDate);
    }
    calendar.add(Calendar.DAY_OF_YEAR, -1);
    Date cutoff = new Date(946684800000L);
    while (calendar.getTime().after(cutoff)) {
      Date start = calendar.getTime();
      Calendar nextDay = (Calendar) calendar.clone();
      nextDay.add(Calendar.DAY_OF_YEAR, 1);
      Date end = nextDay.getTime();
      Query query = dataSession.createQuery(
          "select 1 from BillEntry where username = :username and startTime >= :start and startTime < :end "
              + "and billMins > 0");
      query.setParameter("username", webUser.getUsername());
      query.setParameter("start", start);
      query.setParameter("end", end);
      query.setMaxResults(1);
      if (!query.list().isEmpty()) {
        return start;
      }
      calendar.add(Calendar.DAY_OF_YEAR, -1);
    }
    return null;
  }

  private static void normalizeBillEntriesForDay(
      WebUser webUser,
      Session dataSession,
      List<BillEntry> billEntryList,
      Date dayStart,
      Date dayEnd) {
    if (billEntryList == null || billEntryList.isEmpty()) {
      return;
    }

    Date now = new Date();
    boolean isToday = now.after(dayStart) && now.before(dayEnd);
    BillEntry lastEntry = billEntryList.get(billEntryList.size() - 1);
    boolean preserveLastEnd = isToday && within10Minutes(lastEntry.getEndTime(), now);

    List<Chain> chains = buildChains(billEntryList);
    List<BillEntry> changedEntries = new ArrayList<BillEntry>();

    for (BillEntry entry : billEntryList) {
      boolean preserveLastEntry = isToday && entry == lastEntry;
      if (preserveLastEntry) {
        continue;
      }
      updateStartTime(entry, truncateToMinute(webUser, entry.getStartTime()), dayStart, dayEnd,
          changedEntries);
      updateEndTime(entry, truncateToMinute(webUser, entry.getEndTime()), dayStart, dayEnd, isToday,
          now, false, changedEntries);
    }

    for (Chain chain : chains) {
      BillEntry firstEntry = billEntryList.get(chain.startIndex);
      BillEntry lastEntryInChain = billEntryList.get(chain.endIndex);

      Date roundedStart = roundDown10(webUser, firstEntry.getStartTime());
      updateStartTime(firstEntry, roundedStart, dayStart, dayEnd, changedEntries);

      boolean allowFutureEnd = preserveLastEnd && lastEntryInChain == lastEntry;
      if (!allowFutureEnd) {
        Date roundedEnd = roundUp10(webUser, lastEntryInChain.getEndTime());
        updateEndTime(lastEntryInChain, roundedEnd, dayStart, dayEnd, isToday, now, allowFutureEnd,
            changedEntries);
      }
    }

    for (int i = 0; i < chains.size() - 1; i++) {
      BillEntry aLast = billEntryList.get(chains.get(i).endIndex);
      BillEntry bFirst = billEntryList.get(chains.get(i + 1).startIndex);
      Date aEnd = aLast.getEndTime();
      Date bStart = bFirst.getStartTime();

      if (bStart == null || aEnd == null) {
        continue;
      }

      if (!bStart.after(aEnd)) {
        if (bStart.getTime() != aEnd.getTime()) {
          updateStartTime(bFirst, aEnd, dayStart, dayEnd, changedEntries);
        }
      } else {
        if (sameTenMinuteBucket(webUser, aEnd, bStart)) {
          updateStartTime(bFirst, aEnd, dayStart, dayEnd, changedEntries);
        } else {
          Date roundedAEnd = roundUp10(webUser, aEnd);
          if (roundedAEnd.after(bStart)) {
            roundedAEnd = bStart;
          }
          updateEndTime(aLast, roundedAEnd, dayStart, dayEnd, isToday, now, false, changedEntries);
          Date newAEnd = aLast.getEndTime();
          long gapMillis = bStart.getTime() - newAEnd.getTime();
          if (gapMillis < 10L * 60L * 1000L
              && (bFirst.getStartTime().after(newAEnd) || bFirst.getStartTime().before(newAEnd))) {
            updateStartTime(bFirst, newAEnd, dayStart, dayEnd, changedEntries);
          }
        }
      }
    }

    BillEntry previous = null;
    for (BillEntry entry : billEntryList) {
      Date startTime = clampToDay(entry.getStartTime(), dayStart, dayEnd);
      Date endTime = clampToDay(entry.getEndTime(), dayStart, dayEnd);

      if (previous != null && startTime.before(previous.getEndTime())) {
        startTime = previous.getEndTime();
      }
      if (startTime.after(endTime)) {
        endTime = startTime;
      }

      updateStartTime(entry, startTime, dayStart, dayEnd, changedEntries);
      boolean allowFutureEnd = preserveLastEnd && entry == lastEntry;
      updateEndTime(entry, endTime, dayStart, dayEnd, isToday, now, allowFutureEnd, changedEntries);
      previous = entry;
    }

    if (changedEntries.isEmpty()) {
      return;
    }

    for (BillEntry entry : billEntryList) {
      entry.setBillMins(TimeTracker.calculateMins(entry));
    }

    Transaction trans = dataSession.beginTransaction();
    try {
      for (BillEntry entry : billEntryList) {
        dataSession.update(entry);
      }
    } finally {
      trans.commit();
    }

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(Level.FINE,
          "Normalized bill entries for user={0}, dayStart={1}, dayEnd={2}, chains={3}, updates={4}",
          new Object[] { webUser.getUsername(), dayStart, dayEnd, chains.size(), changedEntries.size() });
    }
  }

  private static List<Chain> buildChains(List<BillEntry> billEntryList) {
    List<Chain> chains = new ArrayList<Chain>();
    int chainStart = 0;
    for (int i = 1; i < billEntryList.size(); i++) {
      BillEntry previous = billEntryList.get(i - 1);
      BillEntry current = billEntryList.get(i);
      if (!isSameMinute(previous.getEndTime(), current.getStartTime())) {
        chains.add(new Chain(chainStart, i - 1));
        chainStart = i;
      }
    }
    chains.add(new Chain(chainStart, billEntryList.size() - 1));
    return chains;
  }

  private static void updateStartTime(BillEntry entry, Date newStart, Date dayStart, Date dayEnd,
      List<BillEntry> changedEntries) {
    Date clamped = clampToDay(newStart, dayStart, dayEnd);
    if (entry.getStartTime() == null || entry.getStartTime().getTime() != clamped.getTime()) {
      entry.setStartTime(clamped);
      if (!changedEntries.contains(entry)) {
        changedEntries.add(entry);
      }
    }
  }

  private static void updateEndTime(BillEntry entry, Date newEnd, Date dayStart, Date dayEnd,
      boolean isToday, Date now, boolean allowFutureEnd, List<BillEntry> changedEntries) {
    Date adjusted = clampToDay(newEnd, dayStart, dayEnd);
    if (isToday && !allowFutureEnd) {
      Date cap = new Date(now.getTime() + SAFETY_MILLIS);
      if (adjusted.after(cap)) {
        adjusted = now;
      }
    }
    if (entry.getEndTime() == null || entry.getEndTime().getTime() != adjusted.getTime()) {
      entry.setEndTime(adjusted);
      if (!changedEntries.contains(entry)) {
        changedEntries.add(entry);
      }
    }
  }

  private static Date clampToDay(Date value, Date dayStart, Date dayEnd) {
    if (value == null) {
      return dayStart;
    }
    if (value.before(dayStart)) {
      return dayStart;
    }
    if (value.after(dayEnd)) {
      return dayEnd;
    }
    return value;
  }

  private static Date truncateToMinute(WebUser webUser, Date time) {
    if (time == null) {
      return null;
    }
    Calendar calendar = webUser.getCalendar();
    calendar.setTime(time);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  private static Date roundDown10(WebUser webUser, Date time) {
    Calendar calendar = webUser.getCalendar();
    calendar.setTime(time);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    int minute = calendar.get(Calendar.MINUTE);
    int roundedMinute = (minute / 10) * 10;
    calendar.set(Calendar.MINUTE, roundedMinute);
    return calendar.getTime();
  }

  private static Date roundUp10(WebUser webUser, Date time) {
    Calendar calendar = webUser.getCalendar();
    calendar.setTime(time);
    int minute = calendar.get(Calendar.MINUTE);
    int mod = minute % 10;
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    if (mod == 0) {
      return calendar.getTime();
    }
    calendar.add(Calendar.MINUTE, 10 - mod);
    return calendar.getTime();
  }

  private static boolean sameTenMinuteBucket(WebUser webUser, Date first, Date second) {
    Calendar calendar = webUser.getCalendar();
    calendar.setTime(first);
    int firstBucket = calendar.get(Calendar.HOUR_OF_DAY) * 6 + calendar.get(Calendar.MINUTE) / 10;
    calendar.setTime(second);
    int secondBucket = calendar.get(Calendar.HOUR_OF_DAY) * 6 + calendar.get(Calendar.MINUTE) / 10;
    return firstBucket == secondBucket;
  }

  private static boolean within10Minutes(Date first, Date second) {
    if (first == null || second == null) {
      return false;
    }
    long diff = Math.abs(first.getTime() - second.getTime());
    return diff <= 10L * 60L * 1000L;
  }

  private static int calculateMins(Date startTime, Date endTime) {
    if (startTime == null || endTime == null) {
      return 0;
    }
    long elapsedTime = endTime.getTime() - startTime.getTime();
    if (elapsedTime <= 0) {
      return 0;
    }
    return (int) (elapsedTime / 60000.0 + 0.5);
  }

  private static final class Chain {
    private final int startIndex;
    private final int endIndex;

    private Chain(int startIndex, int endIndex) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }
  }

  private static void printBillEntriesSection(
      PrintWriter out,
      Session dataSession,
      List<BillEntry> billEntryList,
      Integer lockedBillEntryId,
      String billDateString,
      SimpleDateFormat timeFormat) {
    BillEntry previousEntry = null;
    Date lastEndTime = null;
    int segmentIndex = 0;
    int segmentMinutes = 0;
    boolean tableOpen = false;

    for (BillEntry billEntry : billEntryList) {
      if (previousEntry != null && !isSameMinute(previousEntry.getEndTime(), billEntry.getStartTime())) {
        out.println("</table>");
        out.println("<p>Time worked: " + TimeTracker.formatTime(segmentMinutes) + "</p>");
        int breakMinutes = calculateMins(previousEntry.getEndTime(), billEntry.getStartTime());
        if (breakMinutes > 0) {
          out.println("<p>Break time: " + TimeTracker.formatTime(breakMinutes) + "</p>");
        }
        out.println("<br/>");
        tableOpen = false;
      }

      if (!tableOpen) {
        segmentIndex++;
        String headerLabel = segmentIndex == 1 ? "Start Working" : "Continue Working";
        out.println("<h3>" + timeFormat.format(billEntry.getStartTime()) + " " + headerLabel
            + "</h3>");
        printBillEntriesTableStart(out);
        segmentMinutes = 0;
        tableOpen = true;
      }

      String categoryCode = billEntry.getCategoryCode();
      ProjectCategory projectCategory = TrackServlet.getClient(dataSession, categoryCode, billEntry.getProvider());
      Project project = (Project) dataSession.get(Project.class, billEntry.getProjectId());
      ProjectActionNext projectAction = billEntry.getAction();
      if (projectAction != null) {
        projectAction
            .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
        projectAction.setContact(
            (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId()));
      }

      BillCode billCode = null;
      if (billEntry.getBillCode() != null) {
        billCode = (BillCode) dataSession.get(BillCode.class, billEntry.getBillCode());
      }

      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">"
          + (projectCategory != null ? projectCategory.getClientName() : "") + "</td>");
      if (project != null) {
        out.println(
            "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
      } else {
        out.println("    <td class=\"boxed\"></td>");
      }
      if (projectAction != null) {
        out.println(
            "    <td class=\"boxed\"><a href=\"ProjectActionServlet?actionId=" + projectAction.getActionNextId()
                + "\" class=\"button\">" + projectAction.getNextDescriptionForDisplay(null) + "</a></td>");
      } else {
        out.println("    <td class=\"boxed\"></td>");
      }
      out.println("    <td class=\"boxed\">" + (billCode != null ? billCode.getBillLabel() : "")
          + "</td>");
      out.println(
          "    <td class=\"boxed\">" + timeFormat.format(billEntry.getStartTime()) + "</td>");
      out.println(
          "    <td class=\"boxed\">" + timeFormat.format(billEntry.getEndTime()) + "</td>");
      if (lockedBillEntryId != null && lockedBillEntryId.intValue() == billEntry.getBillId()) {
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billEntry.getBillMins())
            + "</td>");
      } else {
        out.println("    <td class=\"boxed\"><a href=\"BillEntryEditServlet?billId="
            + billEntry.getBillId() + "&billDate=" + billDateString + "\" class=\"button\">"
            + TimeTracker.formatTime(billEntry.getBillMins()) + "</a></td>");
      }
      out.println("    <td class=\"boxed\">" + billEntry.getBillable() + "</td>");
      out.println("  </tr>");

      segmentMinutes += billEntry.getBillMins();
      lastEndTime = billEntry.getEndTime();
      previousEntry = billEntry;
    }

    if (tableOpen) {
      out.println("</table>");
      out.println("<p>Time worked: " + TimeTracker.formatTime(segmentMinutes) + "</p>");
      out.println("<br/>");
    }

    if (lastEndTime != null) {
      out.println("<h3>" + timeFormat.format(lastEndTime) + " Stop Working</h3>");
    }
  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the
  // code.">

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  private static void printBillEntriesTableStart(PrintWriter out) {
    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"8\">Bill Entries</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Category</th>");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Action</th>");
    out.println("    <th class=\"boxed\">Bill Code</th>");
    out.println("    <th class=\"boxed\">Start</th>");
    out.println("    <th class=\"boxed\">End</th>");
    out.println("    <th class=\"boxed\">Time</th>");
    out.println("    <th class=\"boxed\">Bill</th>");
    out.println("  </tr>");
  }

  private static boolean isSameMinute(Date first, Date second) {
    if (first == null || second == null) {
      return false;
    }
    long firstMinute = first.getTime() / 60000L;
    long secondMinute = second.getTime() / 60000L;
    return firstMinute == secondMinute;
  }

}
