package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.StudentOfferTemplateDao;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.StudentOfferTemplate;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentOfferSetupServlet extends ClientServlet {

    private static final String ACTION_ADD_OFFER = "Add Offer";
    private static final String ACTION_BULK_ADD = "Bulk Add Offers";

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_DESCRIPTION = "description";
    private static final String PARAM_DEFAULT_PRICE = "defaultPricePoints";
    private static final String PARAM_BULK_LINES = "bulkLines";
    private static final String PARAM_BULK_DEFAULT_PRICE = "bulkDefaultPricePoints";

    private static final String STATUS_ACTIVE = "ACTIVE";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            if ("STUDENT".equalsIgnoreCase(n(webUser.getWorkflowType()))) {
                appReq.setTitle("Offer Templates");
                printHtmlHead(appReq);
                PrintWriter out = appReq.getOut();
                out.println("<table class=\"boxed\">");
                out.println("  <tr class=\"boxed\"><th class=\"title\">Offer Templates</th></tr>");
                out.println(
                        "  <tr class=\"boxed\"><td class=\"boxed\">This page is available for parent/admin accounts only.</td></tr>");
                out.println(
                        "  <tr class=\"boxed\"><td class=\"boxed\"><a href=\"HomeServlet\">Back to Home</a></td></tr>");
                out.println("</table>");
                printHtmlFoot(appReq);
                return;
            }

            Session dataSession = appReq.getDataSession();
            String action = request.getParameter(PARAM_ACTION);

            if (ACTION_ADD_OFFER.equals(action)) {
                Integer newId = handleQuickAdd(request, dataSession, webUser);
                if (newId != null) {
                    response.sendRedirect(buildSetupUrl("offer-" + newId));
                } else {
                    response.sendRedirect("StudentOfferSetupServlet");
                }
                return;
            }

            if (ACTION_BULK_ADD.equals(action)) {
                handleBulkAdd(request, dataSession, webUser);
                response.sendRedirect("StudentOfferSetupServlet");
                return;
            }

            appReq.setTitle("Student Offer Setup");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Setup / Student Reward Store / Offer Templates");

            ProjectContact contact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());
            List<StudentOfferTemplate> offerList = loadOfferTemplates(dataSession, contact);

            out.println("<h2>Quick Add Offer</h2>");
            out.println("<form action=\"StudentOfferSetupServlet\" method=\"POST\">");
            out.println("<table class=\"boxed\">");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Title</th><td class=\"boxed\"><input type=\"text\" name=\""
                            + PARAM_TITLE + "\" size=\"40\" value=\"\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Description</th><td class=\"boxed\"><textarea name=\""
                            + PARAM_DESCRIPTION + "\" rows=\"3\" cols=\"50\"></textarea></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Default Price Points</th><td class=\"boxed\"><input type=\"text\" name=\""
                            + PARAM_DEFAULT_PRICE + "\" size=\"6\" value=\"0\"></td></tr>");
            out.println("  <tr class=\"boxed\"><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
                    + PARAM_ACTION + "\" value=\"" + ACTION_ADD_OFFER + "\"></td></tr>");
            out.println("</table>");
            out.println("</form>");

            out.println("<br/>");
            out.println("<h2>Offer Templates</h2>");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Image</th>");
            out.println("    <th class=\"boxed\">Title</th>");
            out.println("    <th class=\"boxed\">Description</th>");
            out.println("    <th class=\"boxed\">Default Price</th>");
            out.println("    <th class=\"boxed\">Status</th>");
            out.println("    <th class=\"boxed\">Actions</th>");
            out.println("  </tr>");

            for (StudentOfferTemplate offerTemplate : offerList) {
                String rowAnchor = "offer-" + offerTemplate.getStudentOfferTemplateId();
                String returnAnchor = urlEncode(rowAnchor);
                String imageUrl = buildTemplateImageUrl(offerTemplate, "thumb");
                String editUrl = "StudentOfferEditServlet?studentOfferTemplateId="
                        + offerTemplate.getStudentOfferTemplateId() + "&returnAnchor=" + returnAnchor;
                String uploadUrl = "StudentOfferImageServlet?studentOfferTemplateId="
                        + offerTemplate.getStudentOfferTemplateId() + "&returnAnchor=" + returnAnchor;

                out.println("  <tr class=\"boxed\" id=\"" + rowAnchor + "\">");
                out.println("    <td class=\"boxed\"><a href=\"" + uploadUrl + "\"><img src=\"" + imageUrl
                        + "\" alt=\"Offer image\" width=\"64\" height=\"64\" style=\"object-fit:cover;border:1px solid #ccc;\"></a></td>");
                out.println("    <td class=\"boxed\">" + h(offerTemplate.getTitle()) + "</td>");
                out.println("    <td class=\"boxed\">" + h(trimForDisplay(n(offerTemplate.getDescription()), 120))
                        + "</td>");
                out.println("    <td class=\"boxed\">" + intValue(offerTemplate.getDefaultPricePoints()) + "</td>");
                out.println("    <td class=\"boxed\">" + h(n(offerTemplate.getStatus())) + "</td>");
                out.println("    <td class=\"boxed\"><a class=\"button\" href=\"" + editUrl + "\">Edit</a> "
                        + "<a class=\"button\" href=\"" + uploadUrl + "\">Upload Image</a></td>");
                out.println("  </tr>");
            }

            if (offerList.isEmpty()) {
                out.println(
                        "  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"6\">No offer templates yet.</td></tr>");
            }
            out.println("</table>");

            out.println("<br/>");
            out.println("<h2>Bulk Create</h2>");
            out.println("<form action=\"StudentOfferSetupServlet\" method=\"POST\">");
            out.println("<table class=\"boxed\">");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Default Price Points</th><td class=\"boxed\"><input type=\"text\" name=\""
                            + PARAM_BULK_DEFAULT_PRICE
                            + "\" size=\"6\" value=\"0\"> <span class=\"small\">Applied to all lines</span></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">One Per Line</th><td class=\"boxed\"><textarea name=\""
                            + PARAM_BULK_LINES
                            + "\" rows=\"10\" cols=\"80\" placeholder=\"Science Kit: A fun science project kit&#10;Gum: Pack of gum&#10;Dairy Queen with Dad: Go get a treat together\"></textarea></td></tr>");
            out.println("  <tr class=\"boxed\"><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
                    + PARAM_ACTION + "\" value=\"" + ACTION_BULK_ADD + "\"></td></tr>");
            out.println("</table>");
            out.println("</form>");

            out.println("<br/>");
            out.println("<p><a href=\"SettingsServlet\">Back to Settings</a></p>");

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private Integer handleQuickAdd(HttpServletRequest request, Session dataSession, WebUser webUser) {
        String title = trim(request.getParameter(PARAM_TITLE), 200);
        if (title.equals("")) {
            return null;
        }

        String description = trim(request.getParameter(PARAM_DESCRIPTION), 12000);
        int defaultPricePoints = parseNonNegativeInt(request.getParameter(PARAM_DEFAULT_PRICE), 0);

        ProjectContact contact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());
        if (contact == null) {
            return null;
        }

        StudentOfferTemplate template = new StudentOfferTemplate();
        template.setContact(contact);
        template.setTitle(title);
        template.setDescription(description);
        template.setDefaultPricePoints(Integer.valueOf(defaultPricePoints));
        template.setImagePath(null);
        template.setStatus(STATUS_ACTIVE);
        template.setDisplayOrder(Integer.valueOf(nextDisplayOrder(dataSession, contact)));
        template.setCreatedDate(new Date());
        template.setUpdatedDate(new Date());

        Transaction trans = dataSession.beginTransaction();
        try {
            dataSession.save(template);
            trans.commit();
            return template.getStudentOfferTemplateId();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private void handleBulkAdd(HttpServletRequest request, Session dataSession, WebUser webUser) {
        String bulkLines = n(request.getParameter(PARAM_BULK_LINES));
        if (bulkLines.trim().equals("")) {
            return;
        }

        int defaultPricePoints = parseNonNegativeInt(request.getParameter(PARAM_BULK_DEFAULT_PRICE), 0);
        ProjectContact contact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());
        if (contact == null) {
            return;
        }

        int displayOrder = nextDisplayOrder(dataSession, contact);
        Date now = new Date();

        Transaction trans = dataSession.beginTransaction();
        try {
            String[] lines = bulkLines.split("\\r?\\n");
            for (String line : lines) {
                String raw = n(line).trim();
                if (raw.equals("")) {
                    continue;
                }

                String title;
                String description;
                int firstColon = raw.indexOf(':');
                if (firstColon < 0) {
                    title = trim(raw, 200);
                    description = "";
                } else {
                    title = trim(raw.substring(0, firstColon), 200);
                    description = trim(raw.substring(firstColon + 1), 12000);
                }

                if (title.equals("")) {
                    continue;
                }

                StudentOfferTemplate template = new StudentOfferTemplate();
                template.setContact(contact);
                template.setTitle(title);
                template.setDescription(description);
                template.setDefaultPricePoints(Integer.valueOf(defaultPricePoints));
                template.setImagePath(null);
                template.setStatus(STATUS_ACTIVE);
                template.setDisplayOrder(Integer.valueOf(displayOrder));
                template.setCreatedDate(now);
                template.setUpdatedDate(now);
                dataSession.save(template);
                displayOrder += 10;
            }
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private List<StudentOfferTemplate> loadOfferTemplates(Session dataSession, ProjectContact contact) {
        if (contact == null) {
            return new ArrayList<StudentOfferTemplate>();
        }
        StudentOfferTemplateDao dao = new StudentOfferTemplateDao(dataSession);
        List<StudentOfferTemplate> list = dao.listByContactId(contact.getContactId());
        Collections.sort(list, new Comparator<StudentOfferTemplate>() {
            @Override
            public int compare(StudentOfferTemplate left, StudentOfferTemplate right) {
                int leftStatus = STATUS_ACTIVE.equalsIgnoreCase(n(left.getStatus())) ? 0 : 1;
                int rightStatus = STATUS_ACTIVE.equalsIgnoreCase(n(right.getStatus())) ? 0 : 1;
                if (leftStatus != rightStatus) {
                    return leftStatus - rightStatus;
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
                return intValue(left.getStudentOfferTemplateId()) - intValue(right.getStudentOfferTemplateId());
            }
        });
        return list;
    }

    private int nextDisplayOrder(Session dataSession, ProjectContact contact) {
        Query query = dataSession.createQuery(
                "select max(sot.displayOrder) from StudentOfferTemplate sot where sot.contact.contactId = :contactId");
        query.setInteger("contactId", contact.getContactId());
        Number max = (Number) query.uniqueResult();
        int current = max == null ? 0 : max.intValue();
        return current + 10;
    }

    private String buildTemplateImageUrl(StudentOfferTemplate offerTemplate, String size) {
        StringBuilder url = new StringBuilder();
        url.append("StudentOfferImageServlet?mode=view&studentOfferTemplateId=")
                .append(offerTemplate.getStudentOfferTemplateId())
                .append("&size=")
                .append(size);
        if (offerTemplate.getUpdatedDate() != null) {
            url.append("&v=").append(offerTemplate.getUpdatedDate().getTime());
        }
        return url.toString();
    }

    private static int parseNonNegativeInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(n(value).trim());
            return parsed < 0 ? 0 : parsed;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int intValue(Number number) {
        return number == null ? 0 : number.intValue();
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

    private String buildSetupUrl(String returnAnchor) {
        if (returnAnchor == null || returnAnchor.trim().equals("")) {
            return "StudentOfferSetupServlet";
        }
        return "StudentOfferSetupServlet#" + returnAnchor;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(n(value), "UTF-8");
        } catch (Exception e) {
            return n(value);
        }
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
