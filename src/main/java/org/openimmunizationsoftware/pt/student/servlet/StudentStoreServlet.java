package org.openimmunizationsoftware.pt.student.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import org.openimmunizationsoftware.pt.doa.WeUserDependencyDao;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.GamePointLedger;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.StudentOffer;
import org.openimmunizationsoftware.pt.model.WeUserDependency;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentStoreServlet extends StudentBaseServlet {

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_STUDENT_OFFER_ID = "studentOfferId";

    private static final String ACTION_BUY = "buy";
    private static final String ACTION_RETURN = "return";

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_BOUGHT = "BOUGHT";
    private static final String STATUS_FULFILLING = "FULFILLING";
    private static final String STATUS_DELIVERED = "DELIVERED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                response.sendRedirect("../LoginServlet");
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();

            String action = request.getParameter(PARAM_ACTION);
            Integer studentOfferId = parseInteger(request.getParameter(PARAM_STUDENT_OFFER_ID));
            if (action != null && studentOfferId != null) {
                if (ACTION_BUY.equals(action)) {
                    handleBuy(request, webUser, studentOfferId.intValue(), dataSession);
                } else if (ACTION_RETURN.equals(action)) {
                    handleReturn(webUser, studentOfferId.intValue(), dataSession);
                }
                response.sendRedirect("store");
                return;
            }

            int availablePoints = getAvailablePoints(webUser, dataSession);

            List<StudentOffer> availableOffers = listOffersByStatus(webUser, dataSession, STATUS_AVAILABLE);
            List<StudentOffer> boughtOffers = listOffersByStatus(webUser, dataSession, STATUS_BOUGHT);
            List<StudentOffer> comingSoonOffers = listOffersByStatus(webUser, dataSession, STATUS_FULFILLING);
            List<StudentOffer> deliveredOffers = listOffersByStatus(webUser, dataSession, STATUS_DELIVERED);

            appReq.setTitle("Student Store");
            printHtmlHead(appReq, "Store");
            PrintWriter out = appReq.getOut();

            out.println("<h1>Store</h1>");
            out.println("<table class=\"boxed-mobile\" style=\"width:100%; margin-bottom:10px;\">");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Spendable Points</th>");
            out.println("    <td class=\"boxed\" style=\"text-align:right; font-size:18px; font-weight:bold;\">"
                    + availablePoints + "</td></tr>");
            out.println("</table>");

            printOfferSection(out, "Available", availableOffers, webUser, availablePoints, true, false);
            printOfferSection(out, "Bought", boughtOffers, webUser, availablePoints, false, true);
            printOfferSection(out, "Coming Soon", comingSoonOffers, webUser, availablePoints, false, false);
            printOfferSection(out, "Delivered", deliveredOffers, webUser, availablePoints, false, false);

            if (availableOffers.isEmpty() && boughtOffers.isEmpty() && comingSoonOffers.isEmpty()
                    && deliveredOffers.isEmpty()) {
                out.println("<table class=\"boxed-mobile\" style=\"width:100%;\">");
                out.println("  <tr class=\"boxed\"><td class=\"boxed\">No store offers yet.</td></tr>");
                out.println("</table>");
            }

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            if (!response.isCommitted()) {
                response.sendRedirect("../HomeServlet");
            }
        } finally {
            appReq.close();
        }
    }

    private void printOfferSection(PrintWriter out, String sectionTitle, List<StudentOffer> offers,
            WebUser webUser, int availablePoints, boolean allowBuy, boolean allowReturn) {
        out.println("<h2>" + escapeHtml(sectionTitle) + "</h2>");
        if (offers.isEmpty()) {
            out.println("<table class=\"boxed-mobile\" style=\"width:100%; margin-bottom:10px;\">");
            out.println("  <tr class=\"boxed\"><td class=\"boxed\">No offers in this section.</td></tr>");
            out.println("</table>");
            return;
        }

        for (StudentOffer offer : offers) {
            int pricePoints = intValue(offer.getPricePoints());
            boolean canAfford = availablePoints >= pricePoints;
            int pointsNeeded = Math.max(0, pricePoints - availablePoints);

            out.println("<table class=\"boxed-mobile\" style=\"width:100%; margin-bottom:10px;\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\" style=\"width:72px;\"><img src=\"" + buildOfferImageUrl(offer)
                    + "\" alt=\"Offer image\" width=\"64\" height=\"64\" style=\"object-fit:cover;border:1px solid #ccc;\"></td>");
            out.println("    <td class=\"boxed\">");
            out.println("      <div><strong>" + escapeHtml(n(offer.getTitle())) + "</strong></div>");
            out.println("      <div class=\"small\">" + escapeHtml(trimForDisplay(n(offer.getDescription()), 120))
                    + "</div>");

            if (allowBuy) {
                out.println("      <div style=\"margin-top:4px;\">Price: <strong>" + pricePoints
                        + " points</strong></div>");
            } else {
                out.println("      <div style=\"margin-top:4px;\">Status: <strong>"
                        + escapeHtml(getStudentStatusLabel(offer.getStatus()))
                        + "</strong></div>");
            }

            if (allowBuy) {
                out.println("      <form action=\"store\" method=\"POST\" style=\"margin-top:6px;\">");
                out.println("        <input type=\"hidden\" name=\"" + PARAM_STUDENT_OFFER_ID + "\" value=\""
                        + offer.getStudentOfferId() + "\">");
                out.println("        <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_BUY + "\""
                        + (canAfford ? "" : " disabled") + ">Buy</button>");
                if (!canAfford) {
                    out.println("        <span class=\"small\" style=\"margin-left:6px;\">Need " + pointsNeeded
                            + " more points</span>");
                }
                out.println("      </form>");
            } else if (allowReturn) {
                out.println("      <form action=\"store\" method=\"POST\" style=\"margin-top:6px;\">");
                out.println("        <input type=\"hidden\" name=\"" + PARAM_STUDENT_OFFER_ID + "\" value=\""
                        + offer.getStudentOfferId() + "\">");
                out.println("        <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_RETURN
                        + "\">Return</button>");
                out.println("      </form>");
            }

            out.println("    </td>");
            out.println("  </tr>");
            out.println("</table>");
        }
    }

    private List<StudentOffer> listOffersByStatus(WebUser webUser, Session dataSession, String status) {
        Query query = dataSession.createQuery(
                "from StudentOffer so where so.contact.contactId = :contactId and so.status = :status order by coalesce(so.pricePoints, 0) asc, so.updatedDate desc, so.studentOfferId desc");
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("status", status);
        @SuppressWarnings("unchecked")
        List<StudentOffer> list = query.list();
        return list;
    }

    private void handleBuy(HttpServletRequest request, WebUser webUser, int studentOfferId, Session dataSession) {
        StudentOffer offer = (StudentOffer) dataSession.get(StudentOffer.class, studentOfferId);
        if (offer == null || offer.getContact() == null
                || offer.getContact().getContactId() != webUser.getContactId()) {
            return;
        }
        if (!STATUS_AVAILABLE.equals(offer.getStatus()) || STATUS_WITHDRAWN.equals(offer.getStatus())) {
            return;
        }

        int price = Math.max(0, intValue(offer.getPricePoints()));
        int availablePoints = getAvailablePoints(webUser, dataSession);
        if (availablePoints < price) {
            return;
        }

        ProjectContact contact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());
        Transaction trans = dataSession.beginTransaction();
        try {
            GamePointLedger ledger = new GamePointLedger();
            ledger.setContact(contact);
            ledger.setStudentOffer(offer);
            ledger.setPointChange(Integer.valueOf(-price));
            ledger.setAccountBucket(GamePointLedger.ACCOUNT_SPENDABLE);
            ledger.setEntryType("PURCHASE");
            ledger.setEntryNote("Bought offer: " + n(offer.getTitle()));
            ledger.setCreatedDate(new Date());
            ledger.setCreatedBy(webUser.getUsername());
            dataSession.save(ledger);

            offer.setStatus(STATUS_BOUGHT);
            offer.setBoughtDate(new Date());
            offer.setUpdatedDate(new Date());
            dataSession.update(offer);
            trans.commit();

            sendGuardianPurchaseEmail(request, webUser, offer, dataSession);
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private void sendGuardianPurchaseEmail(HttpServletRequest request, WebUser studentUser,
            StudentOffer offer, Session dataSession) {
        WeUserDependencyDao dependencyDao = new WeUserDependencyDao(dataSession);
        List<WeUserDependency> dependencyList = dependencyDao
                .listByDependentWebUserId(studentUser.getWebUserId());
        if (dependencyList == null || dependencyList.isEmpty()) {
            return;
        }

        String studentName = (n(studentUser.getFirstName()) + " " + n(studentUser.getLastName())).trim();
        if (studentName.equals("")) {
            studentName = n(studentUser.getEmailAddress());
        }

        String offerTitle = n(offer.getTitle());
        int price = Math.max(0, intValue(offer.getPricePoints()));
        String subject = "Dandelion: Student Store Purchase";

        MailManager mailManager = new MailManager(dataSession);
        Set<String> emailedAddresses = new HashSet<String>();

        for (WeUserDependency dependency : dependencyList) {
            if (dependency == null || !"active".equalsIgnoreCase(n(dependency.getDependencyStatus()))) {
                continue;
            }
            WebUser guardianUser = dependency.getGuardianWebUser();
            if (guardianUser == null) {
                continue;
            }
            String toEmail = n(guardianUser.getEmailAddress()).trim();
            if (toEmail.equals("")) {
                continue;
            }
            String addressKey = toEmail.toLowerCase();
            if (emailedAddresses.contains(addressKey)) {
                continue;
            }

            String storeUrl = buildGuardianStoreUrl(request, dataSession, dependency.getDependencyId());
            String body = "<p>" + escapeHtml(studentName)
                    + " just bought an item in the Student Store.</p>"
                    + "<p>Item: <strong>" + escapeHtml(offerTitle) + "</strong><br/>"
                    + "Price: <strong>" + price + " points</strong></p>"
                    + "<p><a href=\"" + storeUrl
                    + "\">Review this dependent account's Student Store</a></p>";

            try {
                mailManager.sendEmail(subject, body, toEmail);
                emailedAddresses.add(addressKey);
            } catch (Exception e) {
                // Purchase should not fail if email cannot be sent.
                e.printStackTrace();
            }
        }
    }

    private String buildGuardianStoreUrl(HttpServletRequest request, Session dataSession, int dependencyId) {
        String externalUrl = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, "", dataSession);
        String base;
        if (externalUrl == null || externalUrl.trim().isEmpty()) {
            String requestUrl = request.getRequestURL().toString();
            String servletPath = request.getServletPath();
            int idx = requestUrl.indexOf(servletPath);
            String rootUrl = idx > 0 ? requestUrl.substring(0, idx + 1) : requestUrl;
            base = rootUrl + "StudentStoreServlet";
        } else {
            String norm = externalUrl.trim();
            if (!norm.endsWith("/")) {
                norm += "/";
            }
            base = norm + "StudentStoreServlet";
        }
        try {
            return base + "?dependencyId=" + URLEncoder.encode(String.valueOf(dependencyId), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return base + "?dependencyId=" + dependencyId;
        }
    }

    private void handleReturn(WebUser webUser, int studentOfferId, Session dataSession) {
        StudentOffer offer = (StudentOffer) dataSession.get(StudentOffer.class, studentOfferId);
        if (offer == null || offer.getContact() == null
                || offer.getContact().getContactId() != webUser.getContactId()) {
            return;
        }
        if (!STATUS_BOUGHT.equals(offer.getStatus())) {
            return;
        }

        int price = Math.max(0, intValue(offer.getPricePoints()));
        ProjectContact contact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());

        Transaction trans = dataSession.beginTransaction();
        try {
            GamePointLedger ledger = new GamePointLedger();
            ledger.setContact(contact);
            ledger.setStudentOffer(offer);
            ledger.setPointChange(Integer.valueOf(price));
            ledger.setAccountBucket(GamePointLedger.ACCOUNT_SPENDABLE);
            ledger.setEntryType("REFUND");
            ledger.setEntryNote("Returned offer: " + n(offer.getTitle()));
            ledger.setCreatedDate(new Date());
            ledger.setCreatedBy(webUser.getUsername());
            dataSession.save(ledger);

            offer.setStatus(STATUS_AVAILABLE);
            offer.setBoughtDate(null);
            offer.setUpdatedDate(new Date());
            dataSession.update(offer);
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private String buildOfferImageUrl(StudentOffer offer) {
        StringBuilder url = new StringBuilder();
        url.append("../StudentOfferImageServlet?mode=view&studentOfferId=")
                .append(offer.getStudentOfferId())
                .append("&size=thumb");
        if (offer.getUpdatedDate() != null) {
            url.append("&v=").append(offer.getUpdatedDate().getTime());
        }
        return url.toString();
    }

    private String getStudentStatusLabel(String status) {
        if (STATUS_BOUGHT.equals(status)) {
            return "Bought";
        }
        if (STATUS_FULFILLING.equals(status)) {
            return "Coming Soon";
        }
        if (STATUS_DELIVERED.equals(status)) {
            return "Delivered";
        }
        return "";
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int getAvailablePoints(WebUser webUser, Session dataSession) {
        return getAccountPoints(webUser, dataSession, GamePointLedger.ACCOUNT_SPENDABLE);
    }

    private int getAccountPoints(WebUser webUser, Session dataSession, String accountBucket) {
        Query query = dataSession.createQuery(
                "select sum(gpl.pointChange) from GamePointLedger gpl where gpl.contact.contactId = :contactId and (gpl.accountBucket = :accountBucket or (:includeLegacy = true and gpl.accountBucket is null))");
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("accountBucket", accountBucket);
        query.setParameter("includeLegacy", Boolean.valueOf(GamePointLedger.ACCOUNT_SPENDABLE.equals(accountBucket)));
        return intValue((Number) query.uniqueResult());
    }
}
