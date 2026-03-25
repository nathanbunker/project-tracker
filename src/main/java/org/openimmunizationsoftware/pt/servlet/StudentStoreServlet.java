package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.StudentOfferDao;
import org.openimmunizationsoftware.pt.doa.WeUserDependencyDao;
import org.openimmunizationsoftware.pt.model.GamePointLedger;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.StudentOffer;
import org.openimmunizationsoftware.pt.model.WeUserDependency;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentStoreServlet extends ClientServlet {

    private static final String PARAM_DEPENDENCY_ID = "dependencyId";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_STUDENT_OFFER_ID = "studentOfferId";

    private static final String ACTION_WITHDRAW = "Withdraw";
    private static final String ACTION_REFUND = "Refund";
    private static final String ACTION_MARK_FULFILLING = "MarkFulfilling";
    private static final String ACTION_MARK_DELIVERED = "MarkDelivered";

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_BOUGHT = "BOUGHT";
    private static final String STATUS_FULFILLING = "FULFILLING";
    private static final String STATUS_DELIVERED = "DELIVERED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser parentUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            PrintWriter out = appReq.getOut();

            Integer dependencyId = parseInteger(request.getParameter(PARAM_DEPENDENCY_ID));
            WeUserDependency dependency = loadValidatedDependency(parentUser, dependencyId, dataSession);
            if (dependency == null) {
                appReq.setTitle("Student Store");
                printHtmlHead(appReq);
                printDandelionLocation(out, "Setup / Dependent Accounts / Student Store");
                appReq.setMessageProblem("Dependent account was not found or is not active for this parent account.");
                out.println("<p><a href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");
                printHtmlFoot(appReq);
                return;
            }

            WebUser dependentUser = dependency.getDependentWebUser();
            ProjectContact dependentContact = (ProjectContact) dataSession.get(ProjectContact.class,
                    dependentUser.getContactId());

            String action = n(request.getParameter(PARAM_ACTION));
            if (!action.equals("")) {
                handleAction(request, dataSession, parentUser, dependentUser, dependentContact, action);
                response.sendRedirect(buildSelfUrl(dependency.getDependencyId()));
                return;
            }

            List<StudentOffer> offerList = loadStoreOffers(dataSession, dependentUser);
            int availablePoints = loadAvailablePoints(dataSession, dependentUser.getContactId());
            Map<String, Integer> counts = buildStatusCounts(offerList);

            appReq.setTitle("Student Store");
            printHtmlHead(appReq);
            printDandelionLocation(out, "Setup / Dependent Accounts / Student Store");

            printDependentContext(out, dependency, dependentUser, dependentContact, availablePoints, counts);

            out.println("<p><a class=\"button\" href=\"StudentStoreAddServlet?" + PARAM_DEPENDENCY_ID + "="
                    + dependency.getDependencyId() + "\">Add Offers</a> "
                    + "<a class=\"button\" href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");

            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Image</th>");
            out.println("    <th class=\"boxed\">Title</th>");
            out.println("    <th class=\"boxed\">Price</th>");
            out.println("    <th class=\"boxed\">Status</th>");
            out.println("    <th class=\"boxed\">Bought</th>");
            out.println("    <th class=\"boxed\">Delivered</th>");
            out.println("    <th class=\"boxed\">Actions</th>");
            out.println("  </tr>");

            if (offerList.isEmpty()) {
                out.println(
                        "  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"7\">No store offers assigned yet.</td></tr>");
            }

            for (StudentOffer offer : offerList) {
                String imageUrl = buildImageUrl(offer);
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\"><img src=\"" + imageUrl
                        + "\" alt=\"Offer image\" width=\"64\" height=\"64\" style=\"object-fit:cover;border:1px solid #ccc;\"></td>");
                out.println("    <td class=\"boxed\">" + h(n(offer.getTitle())) + "</td>");
                out.println("    <td class=\"boxed\">" + intValue(offer.getPricePoints()) + "</td>");
                out.println("    <td class=\"boxed\">" + h(n(offer.getStatus())) + "</td>");
                out.println("    <td class=\"boxed\">" + formatDateTime(parentUser, offer.getBoughtDate()) + "</td>");
                out.println(
                        "    <td class=\"boxed\">" + formatDateTime(parentUser, offer.getDeliveredDate()) + "</td>");
                out.println("    <td class=\"boxed\">" + renderActions(offer, dependency.getDependencyId()) + "</td>");
                out.println("  </tr>");
            }

            out.println("</table>");

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void handleAction(HttpServletRequest request, Session dataSession,
            WebUser parentUser, WebUser dependentUser, ProjectContact dependentContact, String action) {
        Integer studentOfferId = parseInteger(request.getParameter(PARAM_STUDENT_OFFER_ID));
        if (studentOfferId == null) {
            return;
        }

        StudentOfferDao offerDao = new StudentOfferDao(dataSession);
        StudentOffer offer = offerDao.getById(studentOfferId.intValue());
        if (offer == null || offer.getContact() == null
                || offer.getContact().getContactId() != dependentUser.getContactId()) {
            return;
        }

        String status = n(offer.getStatus());
        Date now = new Date();

        Transaction trans = dataSession.beginTransaction();
        try {
            if (ACTION_WITHDRAW.equals(action) && STATUS_AVAILABLE.equals(status)) {
                offer.setStatus(STATUS_WITHDRAWN);
                offer.setUpdatedDate(now);
                offerDao.update(offer);
            } else if (ACTION_MARK_FULFILLING.equals(action) && STATUS_BOUGHT.equals(status)) {
                offer.setStatus(STATUS_FULFILLING);
                offer.setFulfillingDate(now);
                offer.setUpdatedDate(now);
                offerDao.update(offer);
            } else if (ACTION_MARK_DELIVERED.equals(action) && STATUS_FULFILLING.equals(status)) {
                offer.setStatus(STATUS_DELIVERED);
                offer.setDeliveredDate(now);
                offer.setUpdatedDate(now);
                offerDao.update(offer);
            } else if (ACTION_REFUND.equals(action) && STATUS_BOUGHT.equals(status)) {
                GamePointLedger refund = new GamePointLedger();
                refund.setContact(dependentContact != null ? dependentContact : offer.getContact());
                refund.setStudentOffer(offer);
                refund.setPointChange(Integer.valueOf(Math.max(0, intValue(offer.getPricePoints()))));
                refund.setEntryType("OFFER_REFUND");
                refund.setEntryNote("Refunded offer: " + n(offer.getTitle()));
                refund.setCreatedDate(now);
                refund.setCreatedBy(parentUser.getUsername());
                dataSession.save(refund);

                offer.setStatus(STATUS_WITHDRAWN);
                offer.setUpdatedDate(now);
                offerDao.update(offer);
            }
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private List<StudentOffer> loadStoreOffers(Session dataSession, WebUser dependentUser) {
        StudentOfferDao offerDao = new StudentOfferDao(dataSession);
        List<StudentOffer> list = offerDao.listByContactId(dependentUser.getContactId());
        Collections.sort(list, new Comparator<StudentOffer>() {
            @Override
            public int compare(StudentOffer left, StudentOffer right) {
                int leftRank = statusRank(left.getStatus());
                int rightRank = statusRank(right.getStatus());
                if (leftRank != rightRank) {
                    return leftRank - rightRank;
                }
                int leftOrder = intValue(left.getDisplayOrder());
                int rightOrder = intValue(right.getDisplayOrder());
                if (leftOrder != rightOrder) {
                    return leftOrder - rightOrder;
                }
                int titleCmp = n(left.getTitle()).compareToIgnoreCase(n(right.getTitle()));
                if (titleCmp != 0) {
                    return titleCmp;
                }
                return intValue(left.getStudentOfferId()) - intValue(right.getStudentOfferId());
            }
        });
        return list;
    }

    private int loadAvailablePoints(Session dataSession, int contactId) {
        Query query = dataSession.createQuery(
                "select sum(gpl.pointChange) from GamePointLedger gpl where gpl.contact.contactId = :contactId");
        query.setParameter("contactId", contactId);
        Number total = (Number) query.uniqueResult();
        return intValue(total);
    }

    private Map<String, Integer> buildStatusCounts(List<StudentOffer> offerList) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        counts.put(STATUS_AVAILABLE, Integer.valueOf(0));
        counts.put(STATUS_BOUGHT, Integer.valueOf(0));
        counts.put(STATUS_FULFILLING, Integer.valueOf(0));
        for (StudentOffer offer : offerList) {
            String status = n(offer.getStatus());
            Integer current = counts.get(status);
            if (current != null) {
                counts.put(status, Integer.valueOf(current.intValue() + 1));
            }
        }
        return counts;
    }

    private void printDependentContext(PrintWriter out, WeUserDependency dependency, WebUser dependentUser,
            ProjectContact dependentContact, int availablePoints, Map<String, Integer> counts) {
        String dependentName = "";
        if (dependentContact != null) {
            dependentName = (safe(dependentContact.getNameFirst()) + " " + safe(dependentContact.getNameLast())).trim();
        }
        if (dependentName.equals("")) {
            dependentName = (safe(dependentUser.getFirstName()) + " " + safe(dependentUser.getLastName())).trim();
        }

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Student Store Context</th></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Dependent</th><td class=\"boxed\">"
                + h(dependentName) + "</td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Username</th><td class=\"boxed\">"
                + h(safe(dependentUser.getUsername())) + "</td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Available Points</th><td class=\"boxed\"><strong>"
                + availablePoints + "</strong></td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Offer Counts</th><td class=\"boxed\">"
                + "Available: " + intValue(counts.get(STATUS_AVAILABLE))
                + " | Bought: " + intValue(counts.get(STATUS_BOUGHT))
                + " | Fulfilling: " + intValue(counts.get(STATUS_FULFILLING))
                + "</td></tr>");
        out.println("</table>");
        out.println("<br/>");
    }

    private String renderActions(StudentOffer offer, int dependencyId) {
        String status = n(offer.getStatus());
        List<String> actions = new ArrayList<String>();
        if (STATUS_AVAILABLE.equals(status)) {
            actions.add(actionForm(offer.getStudentOfferId(), dependencyId, ACTION_WITHDRAW, "Withdraw"));
        } else if (STATUS_BOUGHT.equals(status)) {
            actions.add(actionForm(offer.getStudentOfferId(), dependencyId, ACTION_REFUND, "Refund"));
            actions.add(actionForm(offer.getStudentOfferId(), dependencyId, ACTION_MARK_FULFILLING, "Mark Fulfilling"));
        } else if (STATUS_FULFILLING.equals(status)) {
            actions.add(actionForm(offer.getStudentOfferId(), dependencyId, ACTION_MARK_DELIVERED, "Mark Delivered"));
        }
        if (actions.isEmpty()) {
            return "&nbsp;";
        }
        StringBuilder sb = new StringBuilder();
        for (String actionHtml : actions) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(actionHtml);
        }
        return sb.toString();
    }

    private String actionForm(Integer studentOfferId, int dependencyId, String action, String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("<form action=\"StudentStoreServlet\" method=\"POST\" style=\"display:inline\"> ");
        sb.append("<input type=\"hidden\" name=\"").append(PARAM_DEPENDENCY_ID).append("\" value=\"")
                .append(dependencyId).append("\"> ");
        sb.append("<input type=\"hidden\" name=\"").append(PARAM_STUDENT_OFFER_ID).append("\" value=\"")
                .append(studentOfferId).append("\"> ");
        sb.append("<input type=\"submit\" name=\"").append(PARAM_ACTION).append("\" value=\"")
                .append(action).append("\" class=\"button\" title=\"").append(h(label)).append("\"> ");
        sb.append("</form>");
        return sb.toString();
    }

    private String buildImageUrl(StudentOffer offer) {
        if (offer.getStudentOfferTemplate() == null
                || offer.getStudentOfferTemplate().getStudentOfferTemplateId() == null) {
            return "StudentOfferImageServlet?mode=view&size=thumb";
        }
        return "StudentOfferImageServlet?mode=view&studentOfferTemplateId="
                + offer.getStudentOfferTemplate().getStudentOfferTemplateId() + "&size=thumb";
    }

    private String formatDateTime(WebUser parentUser, Date date) {
        if (date == null) {
            return "";
        }
        return parentUser.getTimeFormat().format(date);
    }

    private WeUserDependency loadValidatedDependency(WebUser parentUser, Integer dependencyId, Session dataSession) {
        if (dependencyId == null) {
            return null;
        }
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        WeUserDependency dependency = dao.getById(dependencyId.intValue());
        if (dependency == null) {
            return null;
        }
        if (dependency.getGuardianWebUser() == null
                || dependency.getGuardianWebUser().getWebUserId() != parentUser.getWebUserId()) {
            return null;
        }
        if (!"active".equals(dependency.getDependencyStatus())) {
            return null;
        }
        return dependency;
    }

    private int statusRank(String status) {
        if (STATUS_AVAILABLE.equals(status)) {
            return 1;
        }
        if (STATUS_BOUGHT.equals(status)) {
            return 2;
        }
        if (STATUS_FULFILLING.equals(status)) {
            return 3;
        }
        if (STATUS_DELIVERED.equals(status)) {
            return 4;
        }
        if (STATUS_WITHDRAWN.equals(status)) {
            return 5;
        }
        return 9;
    }

    private String buildSelfUrl(int dependencyId) {
        return "StudentStoreServlet?" + PARAM_DEPENDENCY_ID + "=" + dependencyId;
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int intValue(Number number) {
        return number == null ? 0 : number.intValue();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String h(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
