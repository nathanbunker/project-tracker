package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

/**
 * Base servlet for mobile views with shared header/footer rendering.
 * 
 * @author nathan
 */
public abstract class MobileBaseServlet extends ClientServlet {

    protected void printHtmlHead(AppReq appReq) {
        printHtmlHead(appReq, "Mobile");
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
        out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"../mobile.css\" />");
        out.println("  </head>");
        out.println("  <body>");
        out.println("    <div class=\"container\">");
    }

    protected void printHtmlFoot(AppReq appReq) {
        PrintWriter out = appReq.getOut();
        out.println("      <p>Open Immunization Software - Project Tracker</p>");
        out.println("    </div>");
        out.println("  </body>");
        out.println("</html>");
    }
}
