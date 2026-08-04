package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dandeliondaily.nathan.NathanAccessService;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.CentralControl;
import org.openimmunizationsoftware.pt.model.NathanAccessEvent;
import org.openimmunizationsoftware.pt.model.WebUser;

public class NathanResumeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(NathanResumeServlet.class.getName());
    private static final String RESOURCE_PATH = "nathan/Nathan_Bunker_DSR_Resume_2026.pdf";
    private final NathanAccessService accessService = new NathanAccessService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession webSession = request.getSession(false);
        if (webSession == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        WebUser webUser = (WebUser) webSession.getAttribute("webUser");
        Object accessIdValue = webSession.getAttribute(NathanServlet.SESSION_ACCESS_ID);
        boolean admin = webUser != null && webUser.isUserTypeAdmin();
        Integer accessId = accessIdValue instanceof Integer ? (Integer) accessIdValue : null;

        Session dataSession = CentralControl.getSessionFactory().openSession();
        try {
            if (!admin && (accessId == null || !accessService.accessExists(dataSession, accessId.intValue()))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            InputStream input = NathanResumeServlet.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
            if (input == null) {
                LOGGER.warning("Nathan resume resource is unavailable: " + RESOURCE_PATH);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (accessId != null) {
                accessService.recordEvent(dataSession, accessId.intValue(),
                        NathanAccessEvent.TYPE_RESUME_DOWNLOAD, null);
            }
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"Nathan_Bunker_DSR_Resume_2026.pdf\"");
            response.setHeader("Cache-Control", "private, no-store");
            try (InputStream resource = input; OutputStream output = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = resource.read(buffer)) >= 0) {
                    output.write(buffer, 0, count);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to serve Nathan resume", e);
            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            dataSession.close();
        }
    }
}