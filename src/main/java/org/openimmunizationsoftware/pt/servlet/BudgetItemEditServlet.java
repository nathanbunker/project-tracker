/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.axis2.databinding.types.Month;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.MoneyUtil;
import org.openimmunizationsoftware.pt.manager.MonthUtil;
import org.openimmunizationsoftware.pt.model.BudgetAccount;
import org.openimmunizationsoftware.pt.model.BudgetItem;
import org.openimmunizationsoftware.pt.model.BudgetMonth;
import org.openimmunizationsoftware.pt.model.BudgetTrans;
import org.openimmunizationsoftware.pt.model.ProjectClient;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BudgetItemEditServlet extends ClientServlet
{

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
  protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    response.setContentType("text/html;charset=UTF-8");
    HttpSession session = request.getSession(true);
    WebUser webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);
    if (webUser == null)
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try
    {
      Session dataSession = getDataSession(session);

      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

      BudgetAccount budgetAccount = (BudgetAccount) dataSession.get(BudgetAccount.class, Integer.parseInt(request.getParameter("accountId")));
      BudgetItem budgetItem = null;
      BudgetTrans budgetTrans = null;
      if (request.getParameter("transId") != null && !request.getParameter("transId").equals("0") && !request.getParameter("transId").equals(""))
      {
        budgetTrans = (BudgetTrans) dataSession.get(BudgetTrans.class, Integer.parseInt(request.getParameter("transId")));
        budgetItem = budgetTrans.getBudgetItem();
      }
      if (budgetTrans == null)
      {
        budgetTrans = new BudgetTrans();
        budgetTrans.setTransDate(new Date());
        if (request.getParameter("monthDate") != null)
        {
          try
          {
            BudgetMonth budgetMonth = BudgetServlet.getBudgetMonth(dataSession, budgetAccount, sdf.parse(request.getParameter("monthDate")));
            budgetTrans.setBudgetMonth(budgetMonth);
          } catch (ParseException pe)
          {
            throw new ServletException("Unexpected parse exception", pe);
          }
        }
      }
      if (budgetItem == null && request.getParameter("itemId") != null && !request.getParameter("itemId").equals("0")
          && !request.getParameter("itemId").equals(""))
      {
        budgetItem = (BudgetItem) dataSession.get(BudgetItem.class, Integer.parseInt(request.getParameter("itemId")));
      }
      if (budgetItem == null)
      {
        budgetItem = new BudgetItem();
        budgetItem.setBudgetAccount(budgetAccount);
      }

      String action = request.getParameter("action");
      if (action != null)
      {
        if (action.equals("Save"))
        {
          String message = null;
          String itemLabel = request.getParameter("itemLabel");
          if (budgetItem.getItemId() == 0)
          {
            Query query = dataSession.createQuery("from BudgetItem where itemLabel = ? and budgetAccount = ?");
            query.setParameter(0, itemLabel);
            query.setParameter(1, budgetAccount);
            List<BudgetItem> budgetItemAreadyExistsList = query.list();
            if (budgetItemAreadyExistsList.size() > 0)
            {
              message = "Unable to save item label is not unique";
            }
          } else if (!budgetItem.getItemLabel().equals(itemLabel))
          {
            Query query = dataSession.createQuery("from BudgetItem where itemLabel = ? and budgetAccount = ?");
            query.setParameter(0, itemLabel);
            query.setParameter(1, budgetAccount);
            List<BudgetItem> budgetItemAreadyExistsList = query.list();
            if (budgetItemAreadyExistsList.size() > 0 && budgetItemAreadyExistsList.get(0).getItemId() != budgetItem.getItemId())
            {
              message = "Unable to save item label is not unique";
            }
          }
          if (message != null)
          {
            request.setAttribute(REQUEST_VAR_MESSAGE, message);
          } else
          {
            budgetItem.setItemLabel(request.getParameter("itemLabel"));
            budgetItem.setItemStatus(request.getParameter("itemStatus"));
            budgetItem.setPriorityCode(request.getParameter("priorityCode"));
            try
            {
              budgetItem.setLastDate(sdf.parse(request.getParameter("transDate")));
            } catch (ParseException pe)
            {
              message = "Unable to parse transaction date: " + pe.getMessage();
            }
            budgetItem.setLastAmount(MoneyUtil.parse(request.getParameter("transAmount")));
            String relatedItemIdString = request.getParameter("relatedItemId");
            if (relatedItemIdString != null && !relatedItemIdString.equals("0") && !relatedItemIdString.equals("")
                && !relatedItemIdString.startsWith("-"))
            {
              budgetItem.setRelatedBudgetItem((BudgetItem) dataSession.get(BudgetItem.class, Integer.parseInt(relatedItemIdString)));
            }
            if (message != null)
            {
              request.setAttribute(REQUEST_VAR_MESSAGE, message);
            } else
            {
              Transaction trans = dataSession.beginTransaction();
              try
              {
                dataSession.saveOrUpdate(budgetItem);

                if (budgetTrans.getTransStatus() != BudgetTrans.TRANS_STATUS_PAID)
                {
                  if (budgetTrans.getBudgetMonth() == null)
                  {
                    budgetTrans.setBudgetMonth((BudgetMonth) dataSession.get(BudgetMonth.class, Integer.parseInt(request.getParameter("monthId"))));
                  }
                  try
                  {
                    budgetTrans.setTransDate(sdf.parse(request.getParameter("transDate")));
                  } catch (ParseException pe)
                  {
                    message = "Unable to parse transaction date: " + pe.getMessage();
                  }
                  budgetTrans.setTransAmount(MoneyUtil.parse(request.getParameter("transAmount")));
                  budgetTrans.setTransStatus(request.getParameter("transStatus"));
                  budgetTrans.setBudgetItem(budgetItem);
                  if (message != null)
                  {
                    request.setAttribute(REQUEST_VAR_MESSAGE, message);
                  } else
                  {
                    if (budgetTrans.getTransId() == 0)
                    {
                      dataSession.save(budgetTrans);
                    } else
                    {
                      dataSession.update(budgetTrans);
                    }
                    if (relatedItemIdString != null && relatedItemIdString.startsWith("-"))
                    {
                      BudgetItem relatedBudgetItem = new BudgetItem();
                      BudgetAccount relatedBudgetAccount = (BudgetAccount) dataSession.get(BudgetAccount.class,
                          -Integer.parseInt(relatedItemIdString));
                      relatedBudgetItem.setBudgetAccount(relatedBudgetAccount);
                      relatedBudgetItem.setItemLabel(budgetItem.getItemLabel());
                      relatedBudgetItem.setItemStatus(budgetItem.getItemStatus());
                      relatedBudgetItem.setLastAmount(-budgetItem.getLastAmount());
                      relatedBudgetItem.setLastDate(budgetItem.getLastDate());
                      relatedBudgetItem.setPriorityCode(budgetItem.getPriorityCode());
                      relatedBudgetItem.setRelatedBudgetItem(budgetItem);
                      dataSession.save(relatedBudgetItem);
                      budgetItem.setRelatedBudgetItem(relatedBudgetItem);
                      dataSession.update(budgetItem);
                    }
                    if (budgetItem.getRelatedBudgetItem() != null)
                    {
                      BudgetTrans relatedBudgetTrans = budgetTrans.getRelatedBudgetTrans();
                      if (relatedBudgetTrans == null)
                      {
                        Query query = dataSession.createQuery("from BudgetTrans where budgetItem = ? and budgetMonth.monthDate = ?");
                        query.setParameter(0, budgetItem.getRelatedBudgetItem());
                        query.setParameter(1, budgetTrans.getBudgetMonth().getMonthDate());
                        List<BudgetTrans> relatedBudgetTransList = query.list();
                        if (relatedBudgetTransList.size() > 0)
                        {
                          relatedBudgetTrans = relatedBudgetTransList.get(0);
                        }
                      }
                      BudgetMonth relatedBudgetMonth = BudgetServlet.getOrCreateBudgetMonth(dataSession, budgetItem.getRelatedBudgetItem()
                          .getBudgetAccount(), budgetTrans.getBudgetMonth().getMonthDate());
                      if (relatedBudgetTrans == null)
                      {
                        relatedBudgetTrans = new BudgetTrans();
                        relatedBudgetTrans.setBudgetItem(budgetItem.getRelatedBudgetItem());
                        relatedBudgetTrans.setBudgetMonth(relatedBudgetMonth);
                        relatedBudgetTrans.setRelatedBudgetTrans(budgetTrans);
                        relatedBudgetTrans.setTransAmount(-budgetTrans.getTransAmount());
                        relatedBudgetTrans.setTransDate(budgetTrans.getTransDate());
                        relatedBudgetTrans.setTransStatus(budgetTrans.getTransStatus());
                        dataSession.save(relatedBudgetTrans);
                      } else
                      {
                        if (!relatedBudgetTrans.getTransStatus().equals(BudgetTrans.TRANS_STATUS_PAID))
                        {
                          relatedBudgetTrans.setBudgetMonth(relatedBudgetMonth);
                          relatedBudgetTrans.setRelatedBudgetTrans(budgetTrans);
                          relatedBudgetTrans.setTransAmount(-budgetTrans.getTransAmount());
                          relatedBudgetTrans.setTransDate(budgetTrans.getTransDate());
                          dataSession.update(relatedBudgetTrans);
                        }
                      }
                    }

                    response.sendRedirect("BudgetServlet?accountId=" + budgetAccount.getAccountId() + "&monthDate="
                        + sdf.format(budgetTrans.getBudgetMonth().getMonthDate()));
                    return;
                  }
                }
              } finally
              {
                trans.commit();
              }
            }
          }
        }
      }

      printHtmlHead(out, "Budget", request);
      out.println("<div class=\"main\">");
      out.println("<form action=\"BudgetItemEditServlet\" method=\"POST\">");
      out.println("<input type=\"hidden\" name=\"accountId\" value=\"" + budgetAccount.getAccountId() + "\">");
      out.println("<input type=\"hidden\" name=\"transId\" value=\"" + budgetTrans.getTransId() + "\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"2\">Edit Budget Item</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Account</th>");
      out.println("    <td class=\"boxed\">" + budgetAccount.getAccountLabel() + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Budget Item</th>");
      out.println("    <td class=\"boxed\"><select name=\"itemId\">");
      Query query = dataSession.createQuery("from BudgetItem where budgetAccount = ? order by itemLabel");
      query.setParameter(0, budgetAccount);
      List<BudgetItem> selectBudgetItemList = query.list();
      out.println("      <option value=\"0\">-- New --</option>");
      for (BudgetItem selectBudgetItem : selectBudgetItemList)
      {
        if (selectBudgetItem.getItemId() == budgetItem.getItemId())
        {
          out.println("      <option value=\"" + selectBudgetItem.getItemId() + "\" selected>" + selectBudgetItem.getItemLabel() + "</option>");
        } else
        {
          out.println("      <option value=\"" + selectBudgetItem.getItemId() + "\">" + selectBudgetItem.getItemLabel() + "</option>");
        }
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Item Label</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"itemLabel\" value=\"" + budgetItem.getItemLabel() + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Item Status</th>");
      out.println("    <td class=\"boxed\"><select name=\"itemStatus\">");
      for (String[] selection : BudgetItem.ITEM_STATUS)
      {
        if (selection[0].equals(budgetItem.getItemStatus()))
        {
          out.println("      <option value=\"" + selection[0] + "\" selected>" + selection[1] + "</option>");
        } else
        {
          out.println("      <option value=\"" + selection[0] + "\">" + selection[1] + "</option>");
        }
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Item Priority</th>");
      out.println("    <td class=\"boxed\"><select name=\"priorityCode\">");
      for (String[] selection : BudgetItem.PRIORITY_CODE)
      {
        if (selection[0].equals(budgetItem.getPriorityCode()))
        {
          out.println("      <option value=\"" + selection[0] + "\" selected>" + selection[1] + "</option>");
        } else
        {
          out.println("      <option value=\"" + selection[0] + "\">" + selection[1] + "</option>");
        }
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");

      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Transfer To</th>");
      out.println("    <td class=\"boxed\"><select name=\"relatedItemId\">");
      query = dataSession
          .createQuery("from BudgetItem where budgetAccount.providerId = ? and budgetAccount <> ? order by budgetAccount.accountLabel, itemLabel");
      query.setParameter(0, webUser.getProviderId());
      query.setParameter(1, budgetAccount);
      List<BudgetItem> relatedBudgetItemList = query.list();
      out.println("      <option value=\"\"></option>");

      for (BudgetItem relatedBudgetItem : relatedBudgetItemList)
      {
        if (budgetItem.getRelatedBudgetItem() != null && relatedBudgetItem.getItemId() == budgetItem.getRelatedBudgetItem().getItemId())
        {
          out.println("      <option value=\"" + relatedBudgetItem.getItemId() + "\" selected>"
              + relatedBudgetItem.getBudgetAccount().getAccountLabel() + ": " + relatedBudgetItem.getItemLabel() + "</option>");
        } else
        {
          out.println("      <option value=\"" + relatedBudgetItem.getItemId() + "\">" + relatedBudgetItem.getBudgetAccount().getAccountLabel()
              + ": " + relatedBudgetItem.getItemLabel() + "</option>");
        }
      }

      query = dataSession.createQuery("from BudgetAccount where providerId = ? order by accountLabel");
      query.setParameter(0, webUser.getProviderId());
      List<BudgetAccount> relatedBudgetAccountList = query.list();
      for (BudgetAccount lastBudgetAccount : relatedBudgetAccountList)
      {
        if (lastBudgetAccount.getAccountId() != budgetAccount.getAccountId())
        {
          out.println("      <option value=\"-" + lastBudgetAccount.getAccountId() + "\">" + lastBudgetAccount.getAccountLabel()
              + ": (new) </option>");
        }
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");

      if (budgetItem.getLastAmount() > 0)
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Last Amount</th>");
        out.println("    <td class=\"boxed\">" + MoneyUtil.format(budgetItem.getLastAmount()) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Last Trans Date</th>");
        out.println("    <td class=\"boxed\">" + sdf.format(budgetItem.getLastDate()) + "</td>");
        out.println("  </tr>");
      }
      if (budgetTrans.getTransStatus() != BudgetTrans.TRANS_STATUS_PAID)
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">Edit Transaction</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Budget Month</th>");
        out.println("    <td class=\"boxed\"><select name=\"monthId\">");
        query = dataSession.createQuery("from BudgetMonth where budgetAccount = ? order by monthDate desc");
        query.setParameter(0, budgetAccount);
        List<BudgetMonth> budgetMonthList = query.list();
        SimpleDateFormat monthSdf = new SimpleDateFormat("MMM yyyy");
        for (BudgetMonth budgetMonth : budgetMonthList)
        {
          if (budgetTrans.getBudgetMonth() != null && budgetMonth.getMonthId() == budgetTrans.getBudgetMonth().getMonthId())
          {
            out.println("      <option value=\"" + budgetMonth.getMonthId() + "\" selected>" + monthSdf.format(budgetMonth.getMonthDate())
                + "</option>");
          } else
          {
            out.println("      <option value=\"" + budgetMonth.getMonthId() + "\">" + monthSdf.format(budgetMonth.getMonthDate()) + "</option>");
          }
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Date</th>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"transDate\" value=\"" + sdf.format(budgetTrans.getTransDate())
            + "\" size=\"10\"></td>");
        out.println("  </tr>");

        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Amount</th>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"transAmount\" value=\"" + MoneyUtil.format(budgetTrans.getTransAmount())
            + "\" size=\"10\"></td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Trans Status</th>");
        out.println("    <td class=\"boxed\"><select name=\"transStatus\">");
        for (String[] selection : BudgetTrans.TRANS_STATUS)
        {
          if (selection[0].equals(BudgetTrans.TRANS_STATUS_PAID))
          {
            continue;
          }
          if (selection[0].equals(budgetTrans.getTransStatus()))
          {
            out.println("      <option value=\"" + selection[0] + "\" selected>" + selection[1] + "</option>");
          } else
          {
            out.println("      <option value=\"" + selection[0] + "\">" + selection[1] + "</option>");
          }
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("  </tr>");
      } else
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">Paid Transaction</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Budget Month</th>");
        SimpleDateFormat monthSdf = new SimpleDateFormat("MMM yyyy");
        out.println("    <td class=\"boxed\">" + monthSdf.format(budgetTrans.getBudgetMonth().getMonthDate()) + "</td>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Date</th>");
        out.println("    <td class=\"boxed\">" + sdf.format(budgetTrans.getTransDate()) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Amount</th>");
        out.println("    <td class=\"boxed\">" + MoneyUtil.format(budgetTrans.getTransAmount()) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Trans Status</th>");
        out.println("    <td class=\"boxed\">Paid</select>");
        out.println("    </td>");
        out.println("  </tr>");

      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("<br/>");
      if (budgetItem.getItemId() > 0)
      {
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"3\">Transaction History</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Date</th>");
        out.println("    <th class=\"boxed\">Status</th>");
        out.println("    <th class=\"boxed\">Amount</th>");
        out.println("  </tr>");
        query = dataSession.createQuery("from BudgetTrans where budgetItem = ? order by budgetMonth.monthDate desc");
        query.setParameter(0, budgetItem);
        List<BudgetTrans> budgetTransList = query.list();
        for (BudgetTrans otherBudgetTrans : budgetTransList)
        {
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\"><a href=\"BudgetItemEditServlet?accountId=" + budgetAccount.getAccountId() + "&monthDate="
              + sdf.format(MonthUtil.getMonthDate(otherBudgetTrans.getTransDate())) + "&transId=" + otherBudgetTrans.getTransId()
              + "\" class=\"button\">" + sdf.format(otherBudgetTrans.getTransDate()) + "</a></td>");
          out.println("    <td class=\"boxed\">" + otherBudgetTrans.getTransStatusLabel() + "</td>");
          out.println("    <td class=\"boxed\">" + MoneyUtil.format(otherBudgetTrans.getTransAmount()) + "</td>");
          out.println("  </tr>");

        }
        out.println("</table>");
      }
      out.println("</div>");
      printHtmlFoot(out);

    } finally
    {
      out.close();
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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo()
  {
    return "DQA Tester Home Page";
  }// </editor-fold>

}
