/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillCodesServlet extends ClientServlet {

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   * 
   * @param request
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AppReq appReq = new AppReq(request, response);
    try {
      WebUser webUser = appReq.getWebUser();
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getDateFormat();


      Query query;

      appReq.setTitle("Track");
      printHtmlHead(appReq);


      query = dataSession.createQuery(
          "from BillCode where provider = :provider and visible = 'Y' order by billCode");
      query.setParameter("provider", webUser.getProvider());
      List<BillCode> billCodeList = query.list();
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"7\">Bill Codes</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Bill Code</th>");
      out.println("    <th class=\"boxed\">Bill Label</th>");
      out.println("    <th class=\"boxed\">Billable</th>");
      out.println("    <th class=\"boxed\">Visible</th>");
      out.println("    <th class=\"boxed\">Estimate Min</th>");
      out.println("    <th class=\"boxed\">Rate</th>");
      out.println("    <th class=\"boxed\">Round</th>");
      out.println("  </tr>");

      for (BillCode billCode : billCodeList) {
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed\"><a href=\"BillCodeServlet?billCode=" + billCode.getBillCode()
                + "\" class=\"button\">" + billCode.getBillCode() + "</a></td>");
        out.println(
            "    <td class=\"boxed\"><a href=\"BillCodeServlet?billCode=" + billCode.getBillCode()
                + "\" class=\"button\">" + billCode.getBillLabel() + "</a></td>");
        out.println("    <td class=\"boxed\">" + billCode.getBillable() + "</td>");
        out.println("    <td class=\"boxed\">" + billCode.getVisible() + "</td>");
        out.println("    <td class=\"boxed\">" + billCode.getEstimateMin() + "</td>");
        out.println("    <td class=\"boxed\">" + billCode.getBillRate() + "</td>");
        out.println("    <td class=\"boxed\">" + billCode.getBillRound() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table> ");

      out.println("<h2>Create a New Bill Code</h2>");
      out.println(
          "<p>If you do not see your bill code in the list above you can <a href=\"BillCodeEditServlet\">create</a> one.</p>");
      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
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
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

}
