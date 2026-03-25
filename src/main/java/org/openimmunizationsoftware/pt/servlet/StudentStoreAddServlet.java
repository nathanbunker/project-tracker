package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.StudentOfferDao;
import org.openimmunizationsoftware.pt.doa.StudentOfferTemplateDao;
import org.openimmunizationsoftware.pt.doa.WeUserDependencyDao;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.StudentOffer;
import org.openimmunizationsoftware.pt.model.StudentOfferTemplate;
import org.openimmunizationsoftware.pt.model.WeUserDependency;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentStoreAddServlet extends ClientServlet {

    private static final String PARAM_DEPENDENCY_ID = "dependencyId";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_STUDENT_OFFER_TEMPLATE_ID = "studentOfferTemplateId";
    private static final String PARAM_PRICE_POINTS = "pricePoints";

    private static final String ACTION_ADD_TO_STORE = "AddToStore";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_AVAILABLE = "AVAILABLE";

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
                appReq.setTitle("Add Store Offers");
                printHtmlHead(appReq);
                printDandelionLocation(out, "Setup / Dependent Accounts / Add Store Offers");
                appReq.setMessageProblem("Dependent account was not found or is not active for this parent account.");
                out.println("<p><a href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");
                printHtmlFoot(appReq);
                return;
            }

            WebUser dependentUser = dependency.getDependentWebUser();
            ProjectContact parentContact = (ProjectContact) dataSession.get(ProjectContact.class,
                    parentUser.getContactId());
            ProjectContact dependentContact = (ProjectContact) dataSession.get(ProjectContact.class,
                    dependentUser.getContactId());

            String action = n(request.getParameter(PARAM_ACTION));
            if (ACTION_ADD_TO_STORE.equals(action)) {
                handleAddToStore(request, dataSession, parentContact, dependentContact, dependency.getDependencyId());
                response.sendRedirect(
                        "StudentStoreServlet?" + PARAM_DEPENDENCY_ID + "=" + dependency.getDependencyId());
                return;
            }

            StudentOfferTemplateDao templateDao = new StudentOfferTemplateDao(dataSession);
            List<StudentOfferTemplate> templateList = templateDao
                    .listActiveByContactId(parentUser.getContactId());

            appReq.setTitle("Add Store Offers");
            printHtmlHead(appReq);
            printDandelionLocation(out, "Setup / Dependent Accounts / Student Store / Add Offers");

            printContext(out, dependentUser, dependentContact, dependency.getDependencyId());

            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Image</th>");
            out.println("    <th class=\"boxed\">Title</th>");
            out.println("    <th class=\"boxed\">Default Price</th>");
            out.println("    <th class=\"boxed\">Status</th>");
            out.println("    <th class=\"boxed\">Add With Price</th>");
            out.println("  </tr>");

            if (templateList.isEmpty()) {
                out.println(
                        "  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"5\">No active offer templates available.</td></tr>");
            }

            for (StudentOfferTemplate template : templateList) {
                String imageUrl = buildTemplateImageUrl(template, "thumb");

                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\"><img src=\"" + imageUrl
                        + "\" alt=\"Offer image\" width=\"64\" height=\"64\" style=\"object-fit:cover;border:1px solid #ccc;\"></td>");
                out.println("    <td class=\"boxed\">" + h(n(template.getTitle())) + "<br/><span class=\"small\">"
                        + h(trimForDisplay(n(template.getDescription()), 120)) + "</span></td>");
                out.println("    <td class=\"boxed\">" + intValue(template.getDefaultPricePoints()) + "</td>");
                out.println("    <td class=\"boxed\">" + h(n(template.getStatus())) + "</td>");
                out.println("    <td class=\"boxed\">");
                out.println("      <form action=\"StudentStoreAddServlet\" method=\"POST\"> ");
                out.println("      <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\""
                        + dependency.getDependencyId() + "\"> ");
                out.println("      <input type=\"hidden\" name=\"" + PARAM_STUDENT_OFFER_TEMPLATE_ID + "\" value=\""
                        + template.getStudentOfferTemplateId() + "\"> ");
                out.println("      <input type=\"text\" name=\"" + PARAM_PRICE_POINTS + "\" size=\"5\" value=\""
                        + intValue(template.getDefaultPricePoints()) + "\"> ");
                out.println("      <input type=\"submit\" class=\"button\" name=\"" + PARAM_ACTION + "\" value=\""
                        + ACTION_ADD_TO_STORE + "\"> ");
                out.println("      </form>");
                out.println("    </td>");
                out.println("  </tr>");
            }

            out.println("</table>");

            out.println("<br/>");
            out.println("<p><a class=\"button\" href=\"StudentStoreServlet?" + PARAM_DEPENDENCY_ID + "="
                    + dependency.getDependencyId() + "\">Back to Student Store</a> "
                    + "<a class=\"button\" href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void handleAddToStore(HttpServletRequest request, Session dataSession,
            ProjectContact parentContact, ProjectContact dependentContact, int dependencyId) {
        if (parentContact == null || dependentContact == null) {
            return;
        }

        Integer templateId = parseInteger(request.getParameter(PARAM_STUDENT_OFFER_TEMPLATE_ID));
        if (templateId == null) {
            return;
        }

        StudentOfferTemplateDao templateDao = new StudentOfferTemplateDao(dataSession);
        StudentOfferTemplate template = templateDao.getById(templateId.intValue());
        if (template == null || template.getContact() == null
                || template.getContact().getContactId() != parentContact.getContactId()
                || !STATUS_ACTIVE.equalsIgnoreCase(n(template.getStatus()))) {
            return;
        }

        int pricePoints = parseNonNegativeInt(request.getParameter(PARAM_PRICE_POINTS),
                intValue(template.getDefaultPricePoints()));
        StudentOfferDao studentOfferDao = new StudentOfferDao(dataSession);

        if (studentOfferDao.hasAvailableOfferForTemplate(dependentContact.getContactId(),
                template.getStudentOfferTemplateId())) {
            return;
        }

        Date now = new Date();
        StudentOffer studentOffer = new StudentOffer();
        studentOffer.setContact(dependentContact);
        studentOffer.setStudentOfferTemplate(template);
        studentOffer.setTitle(template.getTitle());
        studentOffer.setDescription(template.getDescription());
        studentOffer.setImagePath(template.getImagePath());
        studentOffer.setPricePoints(Integer.valueOf(pricePoints));
        studentOffer.setStatus(STATUS_AVAILABLE);
        studentOffer
                .setDisplayOrder(Integer.valueOf(studentOfferDao.nextDisplayOrder(dependentContact.getContactId())));
        studentOffer.setCreatedDate(now);
        studentOffer.setUpdatedDate(now);

        Transaction trans = dataSession.beginTransaction();
        try {
            studentOfferDao.save(studentOffer);
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private String buildTemplateImageUrl(StudentOfferTemplate template, String size) {
        StringBuilder url = new StringBuilder();
        url.append("StudentOfferImageServlet?mode=view&studentOfferTemplateId=")
                .append(template.getStudentOfferTemplateId())
                .append("&size=")
                .append(size);
        if (template.getUpdatedDate() != null) {
            url.append("&v=").append(template.getUpdatedDate().getTime());
        }
        return url.toString();
    }

    private void printContext(PrintWriter out, WebUser dependentUser, ProjectContact dependentContact,
            int dependencyId) {
        String dependentName = "";
        if (dependentContact != null) {
            dependentName = (safe(dependentContact.getNameFirst()) + " " + safe(dependentContact.getNameLast())).trim();
        }
        if (dependentName.equals("")) {
            dependentName = (safe(dependentUser.getFirstName()) + " " + safe(dependentUser.getLastName())).trim();
        }

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Add Offers To Student Store</th></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Dependent</th><td class=\"boxed\">" + h(dependentName)
                + "</td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Username</th><td class=\"boxed\">"
                + h(safe(dependentUser.getUsername())) + "</td></tr>");
        out.println(
                "  <tr class=\"boxed\"><th class=\"boxed\">Current Store</th><td class=\"boxed\"><a class=\"button\" href=\"StudentStoreServlet?"
                        + PARAM_DEPENDENCY_ID + "=" + dependencyId + "\">Open Student Store</a></td></tr>");
        out.println("</table>");
        out.println("<br/>");
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
