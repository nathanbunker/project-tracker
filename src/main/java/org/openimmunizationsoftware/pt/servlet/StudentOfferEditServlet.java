package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.StudentOfferTemplateDao;
import org.openimmunizationsoftware.pt.model.StudentOfferTemplate;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentOfferEditServlet extends ClientServlet {

    private static final String PARAM_STUDENT_OFFER_TEMPLATE_ID = "studentOfferTemplateId";
    private static final String PARAM_RETURN_ANCHOR = "returnAnchor";

    private static final String PARAM_TITLE = "title";
    private static final String PARAM_DESCRIPTION = "description";
    private static final String PARAM_DEFAULT_PRICE = "defaultPricePoints";
    private static final String PARAM_STATUS = "status";

    private static final String ACTION_SAVE = "Save";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

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
                response.sendRedirect("HomeServlet");
                return;
            }

            Session dataSession = appReq.getDataSession();
            StudentOfferTemplateDao dao = new StudentOfferTemplateDao(dataSession);
            Integer offerTemplateId = parseInteger(request.getParameter(PARAM_STUDENT_OFFER_TEMPLATE_ID));
            if (offerTemplateId == null) {
                response.sendRedirect("StudentOfferSetupServlet");
                return;
            }

            StudentOfferTemplate offerTemplate = dao.getById(offerTemplateId.intValue());
            if (!canEdit(offerTemplate, webUser)) {
                response.sendRedirect("StudentOfferSetupServlet");
                return;
            }

            String returnAnchor = n(request.getParameter(PARAM_RETURN_ANCHOR));
            if (returnAnchor.equals("")) {
                returnAnchor = "offer-" + offerTemplate.getStudentOfferTemplateId();
            }

            String action = n(request.getParameter("action"));
            if (ACTION_SAVE.equals(action)) {
                String title = trim(request.getParameter(PARAM_TITLE), 200);
                String description = trim(request.getParameter(PARAM_DESCRIPTION), 12000);
                int defaultPricePoints = parseNonNegativeInt(request.getParameter(PARAM_DEFAULT_PRICE), 0);
                String status = normalizeStatus(request.getParameter(PARAM_STATUS));

                if (title.equals("")) {
                    appReq.setMessageProblem("Title is required.");
                } else {
                    Transaction trans = dataSession.beginTransaction();
                    try {
                        offerTemplate.setTitle(title);
                        offerTemplate.setDescription(description);
                        offerTemplate.setDefaultPricePoints(Integer.valueOf(defaultPricePoints));
                        offerTemplate.setStatus(status);
                        offerTemplate.setUpdatedDate(new Date());
                        dao.update(offerTemplate);
                        trans.commit();
                    } catch (RuntimeException e) {
                        trans.rollback();
                        throw e;
                    }
                    response.sendRedirect(buildSetupUrl(returnAnchor));
                    return;
                }
            }

            appReq.setTitle("Edit Offer Template");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Setup / Student Reward Store / Edit Offer Template");

            String imageUrl = buildTemplateImageUrl(offerTemplate, "thumb");
            String uploadUrl = "StudentOfferImageServlet?studentOfferTemplateId="
                    + offerTemplate.getStudentOfferTemplateId()
                    + "&returnAnchor=" + urlEncode(returnAnchor);

            out.println("<form action=\"StudentOfferEditServlet\" method=\"POST\">");
            out.println("<input type=\"hidden\" name=\"" + PARAM_STUDENT_OFFER_TEMPLATE_ID + "\" value=\""
                    + offerTemplate.getStudentOfferTemplateId() + "\">");
            out.println("<input type=\"hidden\" name=\"" + PARAM_RETURN_ANCHOR + "\" value=\"" + h(returnAnchor)
                    + "\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Edit Offer Template</th></tr>");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Image</th><td class=\"boxed\"><img src=\"" + imageUrl
                    + "\" alt=\"Offer image\" width=\"64\" height=\"64\" style=\"object-fit:cover;border:1px solid #ccc;\"> "
                    + "<a class=\"button\" href=\"" + uploadUrl + "\">Upload/Replace</a></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Title</th><td class=\"boxed\"><input type=\"text\" name=\""
                            + PARAM_TITLE + "\" size=\"40\" value=\"" + h(n(offerTemplate.getTitle()))
                            + "\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Description</th><td class=\"boxed\"><textarea name=\""
                            + PARAM_DESCRIPTION + "\" rows=\"4\" cols=\"60\">" + h(n(offerTemplate.getDescription()))
                            + "</textarea></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Default Price Points</th><td class=\"boxed\"><input type=\"text\" name=\""
                            + PARAM_DEFAULT_PRICE + "\" size=\"6\" value=\""
                            + intValue(offerTemplate.getDefaultPricePoints())
                            + "\"></td></tr>");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Status</th><td class=\"boxed\"><select name=\""
                    + PARAM_STATUS + "\">");
            printStatusOption(out, STATUS_ACTIVE, offerTemplate.getStatus());
            printStatusOption(out, STATUS_ARCHIVED, offerTemplate.getStatus());
            out.println("</select></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\""
                            + ACTION_SAVE + "\"> <a class=\"button\" href=\"" + buildSetupUrl(returnAnchor)
                            + "\">Cancel</a></td></tr>");
            out.println("</table>");
            out.println("</form>");

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private boolean canEdit(StudentOfferTemplate offerTemplate, WebUser webUser) {
        return offerTemplate != null
                && offerTemplate.getContact() != null
                && offerTemplate.getContact().getContactId() == webUser.getContactId();
    }

    private void printStatusOption(PrintWriter out, String value, String selectedValue) {
        out.println("<option value=\"" + value + "\"" + (value.equalsIgnoreCase(n(selectedValue)) ? " selected" : "")
                + ">" + value + "</option>");
    }

    private String normalizeStatus(String status) {
        return STATUS_ARCHIVED.equalsIgnoreCase(n(status)) ? STATUS_ARCHIVED : STATUS_ACTIVE;
    }

    private int parseNonNegativeInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(n(value).trim());
            return parsed < 0 ? 0 : parsed;
        } catch (Exception e) {
            return fallback;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(Integer.parseInt(n(value).trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private String buildSetupUrl(String returnAnchor) {
        if (returnAnchor == null || returnAnchor.trim().equals("")) {
            return "StudentOfferSetupServlet";
        }
        return "StudentOfferSetupServlet#" + returnAnchor;
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

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(n(value), "UTF-8");
        } catch (Exception e) {
            return n(value);
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
