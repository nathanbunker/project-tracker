package org.openimmunizationsoftware.pt.student.servlet;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public abstract class StudentBaseServlet extends ClientServlet {

    protected void printHtmlHead(AppReq appReq) {
        printHtmlHead(appReq, "School");
    }

    protected void printHtmlHead(AppReq appReq, String activeNavItem) {
        HttpServletResponse response = appReq.getResponse();
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = appReq.getOut();

        out.println("<html>");
        out.println("  <head>");
        out.println("    <meta charset=\"UTF-8\">");
        out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("    <title>Dandelion Student</title>");
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
        out.println(makeStudentMenu(activeNavItem));
        out.println("    <div class=\"mainMobile\" style=\"padding-bottom:80px;\">");
    }

    protected String makeStudentMenu(String activeNavItem) {
        List<String[]> menuList = new ArrayList<String[]>();
        menuList.add(new String[] { "school", "School" });
        menuList.add(new String[] { "store", "Store" });

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
        out.println("      <p>Open Immunization Software - Dandelion</p>");
        out.println("      <p><a href=\"../LoginServlet?action=Logout\" class=\"box\">Logout</a></p>");
        out.println("    </div>");
        out.println("  </body>");
        out.println("</html>");
    }

    protected Date getSelectedDate(HttpServletRequest request, WebUser webUser, String dateParamName) {
        String dateParam = request.getParameter(dateParamName);
        if (dateParam != null && !dateParam.isEmpty()) {
            try {
                return webUser.getDateFormatService().parseTransportDate(dateParam, webUser.getTimeZone());
            } catch (Exception e) {
                // Fall through to today.
            }
        }
        return webUser.getToday();
    }

    protected int getAvailablePoints(ProjectActionNext action) {
        if (action == null || action.getGamePoints() == null) {
            return 0;
        }
        return action.getGamePoints().intValue();
    }

    protected String toUserDateKey(Date date, WebUser webUser) {
        if (date == null) {
            return "";
        }
        return webUser.getDateFormatService().formatTransportDate(date, webUser.getTimeZone());
    }

    protected String toDatabaseDateKey(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    protected Date parseUTCDate(String dateString) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.parse(dateString);
    }

    protected int intValue(Number number) {
        return number == null ? 0 : number.intValue();
    }

    protected String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
