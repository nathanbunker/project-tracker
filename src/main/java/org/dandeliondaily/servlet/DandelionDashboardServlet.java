package org.dandeliondaily.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.dashboard.render.DashboardPageRenderer;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class DandelionDashboardServlet extends ClientServlet {

    private static final long serialVersionUID = 6049052526445852256L;

    private final DashboardPageRenderer dashboardPageRenderer = new DashboardPageRenderer();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            appReq.setTitle("Dandelion Dashboard");
            printHtmlHead(appReq);
            dashboardPageRenderer.render(appReq);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
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