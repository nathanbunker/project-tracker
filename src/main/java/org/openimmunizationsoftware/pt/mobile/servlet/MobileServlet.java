/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openimmunizationsoftware.pt.AppReq;

/**
 * 
 * @author nathan
 */
public class MobileServlet extends MobileBaseServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     * 
     * @param request
     *                 servlet request
     * @param response
     *                 servlet response
     * @throws ServletException
     *                          if a servlet-specific error occurs
     * @throws IOException
     *                          if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            appReq.setTitle("Mobile");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            out.println("<h1>Welcome to mobile</h1>");
            out.println("<p><a href=\"../HomeServlet\">Back to Home</a></p>");
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * 
     * @param request
     *                 servlet request
     * @param response
     *                 servlet response
     * @throws ServletException
     *                          if a servlet-specific error occurs
     * @throws IOException
     *                          if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * 
     * @param request
     *                 servlet request
     * @param response
     *                 servlet response
     * @throws ServletException
     *                          if a servlet-specific error occurs
     * @throws IOException
     *                          if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}
