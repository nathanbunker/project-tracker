package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

/**
 * Base servlet for mobile views with shared header/footer rendering.
 * 
 * @author nathan
 */
public abstract class MobileBaseServlet extends ClientServlet {

    protected static final String PARAM_SHOW_WORK = "showWork";
    protected static final String PARAM_SHOW_PERSONAL = "showPersonal";
    protected static final String PARAM_FILTER_SUBMITTED = "filterSubmitted";
    protected static final String SESSION_SHOW_WORK = "projectAction.showWork";
    protected static final String SESSION_SHOW_PERSONAL = "projectAction.showPersonal";
    protected static final String REQUEST_SHOW_WORK = "projectAction.requestShowWork";
    protected static final String REQUEST_SHOW_PERSONAL = "projectAction.requestShowPersonal";

    protected void printHtmlHead(AppReq appReq) {
        printHtmlHead(appReq, "Project");
    }

    protected void printHtmlHead(AppReq appReq, String activeNavItem) {
        HttpServletResponse response = appReq.getResponse();
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = appReq.getOut();

        out.println("<html>");
        out.println("  <head>");
        out.println("    <meta charset=\"UTF-8\">");
        out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("    <title>PT Mobile</title>");
        String displayColor = appReq.getDisplayColor();
        String displaySize = appReq.getDisplaySize();
        try {
            out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"../CssServlet?displaySize="
                    + displaySize + "&displayColor=" + URLEncoder.encode(displayColor, "UTF-8") + "\" />");
        } catch (UnsupportedEncodingException uex) {
            uex.printStackTrace();
        }
        out.println("  </head>");
        out.println("  <body>");
        out.println(makeMobileMenu(activeNavItem));
        out.println("    <div class=\"mainMobile\">");
    }

    protected String makeMobileMenu(String activeNavItem) {
        List<String[]> menuList = new ArrayList<String[]>();
        menuList.add(new String[] { "project", "Project" });
        menuList.add(new String[] { "todo", "Todo" });
        menuList.add(new String[] { "action", "Action" });

        StringBuilder result = new StringBuilder();
        result.append("    <table class=\"menu\"><tr><td>");
        for (int i = 0; i < menuList.size(); i++) {
            String[] menu = menuList.get(i);
            if (i > 0) {
                result.append(" ");
            }
            String styleClass = "menuLink";
            if (menu[1].equals(activeNavItem)) {
                styleClass = "menuLinkSelected";
            }
            result.append("<a class=\"");
            result.append(styleClass);
            result.append("\" href=\"");
            result.append(menu[0]);
            result.append("\">");
            result.append(menu[1]);
            result.append("</a>");
        }
        result.append("</td></tr></table>");
        return result.toString();
    }

    protected void printHtmlFoot(AppReq appReq) {
        PrintWriter out = appReq.getOut();
        out.println("      <p>Open Immunization Software - Project Tracker</p>");
        out.println("      <p><a href=\"../HomeServlet\" class=\"box\">Desktop Mode</a></p>");
        out.println("    </div>");
        out.println("  </body>");
        out.println("</html>");
    }

    protected void resolveAndStoreShowPreferences(HttpServletRequest request) {
        if (request.getAttribute(REQUEST_SHOW_WORK) instanceof Boolean
                && request.getAttribute(REQUEST_SHOW_PERSONAL) instanceof Boolean) {
            return;
        }

        HttpSession session = request.getSession();
        boolean hasFilterSubmitted = request.getParameter(PARAM_FILTER_SUBMITTED) != null;
        boolean hasShowWorkParam = request.getParameter(PARAM_SHOW_WORK) != null;
        boolean hasShowPersonalParam = request.getParameter(PARAM_SHOW_PERSONAL) != null;

        boolean showWork;
        boolean showPersonal;
        if (hasFilterSubmitted) {
            showWork = hasShowWorkParam;
            showPersonal = hasShowPersonalParam;
        } else {
            Boolean sessionShowWork = (Boolean) session.getAttribute(SESSION_SHOW_WORK);
            Boolean sessionShowPersonal = (Boolean) session.getAttribute(SESSION_SHOW_PERSONAL);
            if (sessionShowWork == null && sessionShowPersonal == null) {
                showWork = true;
                showPersonal = true;
            } else {
                showWork = sessionShowWork != null ? sessionShowWork.booleanValue() : true;
                showPersonal = sessionShowPersonal != null ? sessionShowPersonal.booleanValue() : true;
            }
        }

        if (!showWork && !showPersonal) {
            showWork = true;
            showPersonal = true;
        }

        session.setAttribute(SESSION_SHOW_WORK, Boolean.valueOf(showWork));
        session.setAttribute(SESSION_SHOW_PERSONAL, Boolean.valueOf(showPersonal));
        request.setAttribute(REQUEST_SHOW_WORK, Boolean.valueOf(showWork));
        request.setAttribute(REQUEST_SHOW_PERSONAL, Boolean.valueOf(showPersonal));
    }

    protected boolean isShowWork(HttpServletRequest request) {
        resolveAndStoreShowPreferences(request);
        return ((Boolean) request.getAttribute(REQUEST_SHOW_WORK)).booleanValue();
    }

    protected boolean isShowPersonal(HttpServletRequest request) {
        resolveAndStoreShowPreferences(request);
        return ((Boolean) request.getAttribute(REQUEST_SHOW_PERSONAL)).booleanValue();
    }
}
