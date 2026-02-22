package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openimmunizationsoftware.pt.AppReq;

/**
 * Mobile fallback page for unexpected errors.
 */
public class OopsServlet extends MobileBaseServlet {

    private static final String PARAM_ACTION = "action";
    private static final String ACTION_LOGOUT = "logout";

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
            String action = request.getParameter(PARAM_ACTION);
            if (ACTION_LOGOUT.equals(action)) {
                if (appReq.isLoggedIn()) {
                    appReq.logout();
                }
                response.sendRedirect("../LoginServlet?uiMode=mobile");
                return;
            }

            appReq.setTitle("Oops");
            printHtmlHead(appReq, "Oops");
            PrintWriter out = appReq.getOut();

            out.println("<h1>Oops</h1>");
            out.println("<p>Something went wrong while loading this mobile page.</p>");
            out.println("<p>You can try again, or reset your session and log in fresh.</p>");
            out.println("<p><a href=\"todo\" class=\"button\">Try Todo Again</a></p>");

            if (appReq.isLoggedIn()) {
                out.println("<p><a href=\"oops?action=logout\" class=\"box\">Logout &amp; Reset Session</a></p>");
            } else {
                out.println("<p><a href=\"../LoginServlet?uiMode=mobile\" class=\"box\">Go to Login</a></p>");
            }

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            if (!response.isCommitted()) {
                response.sendRedirect("../LoginServlet?uiMode=mobile");
            }
        } finally {
            appReq.close();
        }
    }
}
