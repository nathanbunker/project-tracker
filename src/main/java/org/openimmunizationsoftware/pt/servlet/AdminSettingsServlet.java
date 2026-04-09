package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.pt.AppReq;

public class AdminSettingsServlet extends ClientServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }
            appReq.setTitle("Admin Settings");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            out.println("<div class=\"main\">");
            out.println("<h1>Admin Settings</h1>");
            out.println("<p>Legacy category administration has been removed for hard cutover.</p>");
            out.println("<p>Use tag-based project controls from dashboard and project health pages.</p>");
            out.println("<p><a href=\"DandelionDashboardServlet\">Open Dashboard</a></p>");
            out.println("</div>");
            printHtmlFoot(appReq);
        } finally {
            appReq.close();
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
