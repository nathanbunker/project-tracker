/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class BillEntryEditServlet extends ClientServlet {

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
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getTimeFormat();

      int billId = Integer.parseInt(request.getParameter("billId"));
      BillEntry billEntry = (BillEntry) dataSession.get(BillEntry.class, billId);
      TimeTracker timeTracker = appReq.getTimeTracker();
      String billDate = request.getParameter("billDate");

      ProjectActionNext projectAction = billEntry.getAction();
      if (projectAction != null) {
        projectAction
            .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
        projectAction.setContact(
            (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId()));
      }

      boolean adjustOthersChecked = action == null || request.getParameter("adjustOthers") != null;

      if (action != null) {
        String message = null;
        if (action.equals("Save")) {
          Date originalStart = billEntry.getStartTime();
          Date originalEnd = billEntry.getEndTime();
          Date newStart = null;
          Date newEnd = null;

          try {
            newStart = sdf.parse(request.getParameter("startTime"));
          } catch (ParseException pe) {
            message = "Unable to parse start time: " + pe.getMessage();
          }
          try {
            newEnd = sdf.parse(request.getParameter("endTime"));
          } catch (ParseException pe) {
            message = "Unable to parse end time: " + pe.getMessage();
          }

          if (message == null) {
            if (newStart.after(newEnd)) {
              message = "Start time must be before end time.";
            } else if (newEnd.getTime() - newStart.getTime() > (12L * 60L * 60L * 1000L)) {
              message = "Duration cannot exceed 12 hours.";
            }
          }

          if (message == null && adjustOthersChecked) {
            boolean sameDay = isSameDate(newStart, newEnd);
            boolean sameAsOriginalStart = isSameDate(newStart, originalStart);
            boolean sameAsOriginalEnd = isSameDate(newEnd, originalStart);
            if (!sameDay || !sameAsOriginalStart || !sameAsOriginalEnd) {
              message = "Automatic adjustment only works within the same day. Uncheck 'Adjust other entries' to move entries across days.";
            }
          }

          if (message != null) {
            appReq.setMessageProblem(message);
          } else {
            int projectId = Integer.parseInt(request.getParameter("projectId"));
            if (billEntry.getProjectId() != projectId) {
              billEntry.setAction(null);
            }
            billEntry.setProjectId(projectId);
            billEntry.setCategoryCode(request.getParameter("categoryCode"));
            billEntry.setBillCode(request.getParameter("billCode"));
            billEntry.setBillable(request.getParameter("billable") != null ? "Y" : "N");
            billEntry.setBillCode(request.getParameter("billCode"));

            if (adjustOthersChecked) {
              try {
                List<BillEntry> dayEntries = listEntriesForDay(webUser, dataSession, originalStart);
                applyAdjustedTimes(dayEntries, billEntry, newStart, newEnd, originalStart, originalEnd);

                Transaction trans = dataSession.beginTransaction();
                try {
                  for (BillEntry entry : dayEntries) {
                    dataSession.update(entry);
                  }
                } finally {
                  trans.commit();
                }
              } catch (RuntimeException ex) {
                appReq.setMessageProblem("Unable to adjust entries. Please try again.");
                adjustOthersChecked = true;
                action = null;
              }
            } else {
              billEntry.setStartTime(newStart);
              billEntry.setEndTime(newEnd);
              billEntry.setBillMins(TimeTracker.calculateMins(billEntry));
              Transaction trans = dataSession.beginTransaction();
              try {
                dataSession.update(billEntry);
              } finally {
                trans.commit();
              }
            }

            if (appReq.getMessageProblem() == null) {
              timeTracker.init(webUser, dataSession);
              response.sendRedirect("BillEntriesServlet?billDate=" + billDate);
              return;
            }
          }
        }
      }

      appReq.setTitle("Track");
      printHtmlHead(appReq);

      out.println("<form action=\"BillEntryEditServlet\" method=\"POST\">");
      out.println(
          " <input type=\"hidden\" name=\"billId\" value=\"" + billEntry.getBillId() + "\">");
      out.println(" <input type=\"hidden\" name=\"billDate\" value=\"" + billDate + "\">");

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"6\">Edit Bill Entries</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <td class=\"boxed\"><select name=\"categoryCode\">");
      Query query = dataSession.createQuery("from ProjectCategory where provider = :provider");
      query.setParameter("provider", webUser.getProvider());
      @SuppressWarnings("unchecked")
      List<ProjectCategory> projectCategoryList = query.list();
      for (ProjectCategory projectCategory : projectCategoryList) {
        if (projectCategory.getCategoryCode().equals(billEntry.getCategoryCode())) {
          out.println("      <option value=\"" + projectCategory.getCategoryCode() + "\" selected>"
              + projectCategory.getClientName() + "</option>");
        } else {
          out.println("      <option value=\"" + projectCategory.getCategoryCode() + "\">"
              + projectCategory.getClientName() + "</option>");
        }
      }
      out.println("    </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <td class=\"boxed\"><select name=\"projectId\">");
      query = dataSession.createQuery(
          "from Project where provider = :provider order by categoryCode, projectName");
      query.setParameter("provider", webUser.getProvider());
      @SuppressWarnings("unchecked")
      List<Project> projectList = query.list();
      for (Project project : projectList) {
        if (project.getProjectId() == billEntry.getProjectId()) {
          out.println("      <option value=\"" + project.getProjectId() + "\" selected>"
              + project.getCategoryCode() + " " + project.getProjectName() + "</option>");
        } else {
          out.println("      <option value=\"" + project.getProjectId() + "\">"
              + project.getCategoryCode() + " " + project.getProjectName() + "</option>");
        }
      }
      out.println("    </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Provider</th>");
      out.println("    <td class=\"boxed\"><select name=\"billCode\">");
      query = dataSession.createQuery(
          "from BillCode where provider = :provider and visible = 'Y' order by billLabel");
      query.setParameter("provider", webUser.getProvider());
      @SuppressWarnings("unchecked")
      List<BillCode> billCodeList = query.list();
      for (BillCode billCode : billCodeList) {
        if (billCode.getBillCode().equals(billEntry.getBillCode())) {
          out.println("      <option value=\"" + billCode.getBillCode() + "\" selected>"
              + billCode.getBillLabel() + "</option>");
        } else {
          out.println("      <option value=\"" + billCode.getBillCode() + "\">"
              + billCode.getBillLabel() + "</option>");
        }
      }
      out.println("    </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("    <td class=\"boxed\">"
          + (billEntry.getAction() == null ? "" : billEntry.getAction().getNextDescriptionForDisplay(null)) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Start Time</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"startTime\" value=\""
          + sdf.format(billEntry.getStartTime()) + "\" size=\"20\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">End Time</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"endTime\" value=\""
          + sdf.format(billEntry.getEndTime()) + "\" size=\"20\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Time</th>");
      out.println(
          "    <td class=\"boxed\">" + TimeTracker.formatTime(billEntry.getBillMins()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Billable</th>");
      out.println("    <td class=\"boxed\"><input type=\"checkbox\" name=\"billable\" value=\"Y\""
          + (billEntry.getBillable().equals("Y") ? " checked" : "") + "></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\">"
              + "<input type=\"submit\" name=\"action\" value=\"Save\"> "
              + "<label><input type=\"checkbox\" name=\"adjustOthers\" value=\"Y\""
              + (adjustOthersChecked ? " checked" : "")
              + "> Adjust other entries</label></td>");
      out.println("  </tr>");
      out.println("</table> ");
      out.println("</form>");
      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
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

  private static List<BillEntry> listEntriesForDay(WebUser webUser, Session dataSession, Date originalStart) {
    Calendar calendar = TimeTracker.createToday(webUser);
    calendar.setTime(originalStart);
    Date dayStart = calendar.getTime();
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    Date nextDay = calendar.getTime();
    Query query = dataSession.createQuery(
        "from BillEntry where username = ? and startTime >= ? and startTime < ? order by startTime");
    query.setParameter(0, webUser.getUsername());
    query.setParameter(1, dayStart);
    query.setParameter(2, nextDay);
    @SuppressWarnings("unchecked")
    List<BillEntry> dayEntries = query.list();
    return dayEntries;
  }

  private static void applyAdjustedTimes(List<BillEntry> dayEntries, BillEntry editedEntry, Date newStart,
      Date newEnd, Date originalStart, Date originalEnd) {
    if (dayEntries == null || dayEntries.isEmpty()) {
      return;
    }

    int editedIndex = -1;
    BillEntry entryFromList = null;
    for (int i = 0; i < dayEntries.size(); i++) {
      BillEntry entry = dayEntries.get(i);
      if (entry.getBillId() == editedEntry.getBillId()) {
        editedIndex = i;
        entryFromList = entry;
        break;
      }
    }
    if (editedIndex == -1 || entryFromList == null) {
      return;
    }

    BillEntry previous = editedIndex > 0 ? dayEntries.get(editedIndex - 1) : null;
    BillEntry next = editedIndex < dayEntries.size() - 1 ? dayEntries.get(editedIndex + 1) : null;
    boolean previousContiguous = previous != null && isSameMinute(previous.getEndTime(), originalStart);
    boolean nextContiguous = next != null && isSameMinute(next.getStartTime(), originalEnd);

    entryFromList.setStartTime(newStart);
    entryFromList.setEndTime(newEnd);

    Set<BillEntry> changed = new HashSet<BillEntry>();
    changed.add(entryFromList);

    if (previous != null && newStart.after(originalStart) && previousContiguous) {
      previous.setEndTime(newStart);
      if (previous.getStartTime().after(previous.getEndTime())) {
        previous.setStartTime(newStart);
        previous.setEndTime(newStart);
      }
      changed.add(previous);
    }

    if (next != null && newEnd.before(originalEnd) && nextContiguous) {
      next.setStartTime(newEnd);
      if (next.getStartTime().after(next.getEndTime())) {
        next.setStartTime(newEnd);
        next.setEndTime(newEnd);
      }
      changed.add(next);
    }

    Date boundaryStart = entryFromList.getStartTime();
    for (int i = editedIndex - 1; i >= 0; i--) {
      BillEntry entry = dayEntries.get(i);
      if (entry.getEndTime().after(boundaryStart)) {
        entry.setEndTime(boundaryStart);
        if (entry.getStartTime().after(entry.getEndTime())) {
          entry.setStartTime(boundaryStart);
          entry.setEndTime(boundaryStart);
        }
        changed.add(entry);
      }
      boundaryStart = entry.getStartTime();
    }

    Date boundaryEnd = entryFromList.getEndTime();
    for (int i = editedIndex + 1; i < dayEntries.size(); i++) {
      BillEntry entry = dayEntries.get(i);
      if (entry.getStartTime().before(boundaryEnd)) {
        entry.setStartTime(boundaryEnd);
        if (entry.getStartTime().after(entry.getEndTime())) {
          entry.setStartTime(boundaryEnd);
          entry.setEndTime(boundaryEnd);
        }
        changed.add(entry);
      }
      boundaryEnd = entry.getEndTime();
    }

    for (BillEntry entry : changed) {
      entry.setBillMins(TimeTracker.calculateMins(entry));
    }
  }

  private static boolean isSameDate(Date first, Date second) {
    if (first == null || second == null) {
      return false;
    }
    Calendar firstCal = Calendar.getInstance();
    firstCal.setTime(first);
    Calendar secondCal = Calendar.getInstance();
    secondCal.setTime(second);
    return firstCal.get(Calendar.YEAR) == secondCal.get(Calendar.YEAR)
        && firstCal.get(Calendar.DAY_OF_YEAR) == secondCal.get(Calendar.DAY_OF_YEAR);
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
