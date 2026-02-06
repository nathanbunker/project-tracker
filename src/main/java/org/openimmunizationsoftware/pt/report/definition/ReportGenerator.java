package org.openimmunizationsoftware.pt.report.definition;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;

public class ReportGenerator {
  public static StringBuffer generateReport(PrintWriter out, StringBuffer sbuf,
      Map<String, String> parameterValues, String reportText, Session dataSession) {
    final String cleaned = removeReportTag(reportText);
    dataSession.doWork(conn -> {
      generateReport(out, sbuf, parameterValues, cleaned, 0, conn);
    });

    return sbuf;
  }

  private static int generateReport(PrintWriter out, StringBuffer sbuf,
      Map<String, String> parameterValues, String reportText, int curPos, Connection conn) {
    int nextStartBracket = 0;
    while (curPos < reportText.length()) {
      nextStartBracket = reportText.indexOf("<%", curPos);
      if (nextStartBracket != -1) {
        int nextEndBracket = reportText.indexOf("%>", nextStartBracket);
        if (nextEndBracket != -1) {
          if (parameterValues != null) {
            print(out, sbuf, parameterValues, reportText, curPos, nextStartBracket);
          }
          String sql = reportText.substring(nextStartBracket + 2, nextEndBracket).trim();
          curPos = nextEndBracket + 2;
          if (sql.equalsIgnoreCase("end-loop")) {
            return curPos;
          }
          curPos = query(out, sbuf, parameterValues, conn, reportText, sql, curPos);
        } else {
          curPos = print(out, sbuf, parameterValues, reportText, curPos, reportText.length());
        }
      } else {
        curPos = print(out, sbuf, parameterValues, reportText, curPos, reportText.length());
      }
    }
    return curPos;
  }

  public static final String COMMAND_LOOP = "LOOP";
  public static final String COMMAND_SET = "SET";
  public static final String COMMAND_TABLE = "TABLE";
  public static final String COMMAND_GET = "GET";

  private static int query(PrintWriter out, StringBuffer sbuf, Map<String, String> parameterValues,
      Connection conn, String reportText, String sqlSource, int curPos) {
    String command = "";
    int pos = sqlSource.indexOf(" ");
    if (pos != -1) {
      command = sqlSource.substring(0, pos).trim().toUpperCase();
      sqlSource = sqlSource.substring(pos).trim();
    }
    if (parameterValues == null) {
      if (command.equals(COMMAND_LOOP)) {
        curPos = generateReport(null, null, null, reportText, curPos, null);
      }
      return curPos;
    }
    if (sqlSource.endsWith(";")) {
      sqlSource = sqlSource.substring(0, sqlSource.length() - 1).trim();
    }
    String caption = null;
    if (command.equals(COMMAND_TABLE)) {
      if (sqlSource.startsWith("\"")) {
        int quotePos = sqlSource.indexOf("\"", 1);
        if (quotePos != -1) {
          caption = substitute(sqlSource.substring(1, quotePos), parameterValues);
          quotePos++;
          sqlSource = sqlSource.substring(quotePos).trim();
        }
      }
    }
    int sqlListPos = 0;
    String[] sqlList = sqlSource.split("\\;");
    PreparedStatement pstmt = null;
    ResultSet rset = null;
    try {
      pstmt = prepareStatement(parameterValues, conn, sqlListPos, sqlList);
      rset = pstmt.executeQuery();
      ResultSetMetaData meta = rset.getMetaData();
      int columnCount = meta.getColumnCount();
      if (command.equals(COMMAND_TABLE)) {
        println("<table class=\"boxed\">", out, sbuf);
        if (caption != null) {
          println("<tr><th class=\"title\" colspan=\"" + columnCount + "\">", out, sbuf);
          print(caption, out, sbuf);
          println("</th></tr>", out, sbuf);
        }
        println("<tr class=\"boxed\">", out, sbuf);
        for (int i = 1; i <= columnCount; i++) {
          println("<th class=\"boxed\">", out, sbuf);
          print(meta.getColumnLabel(i), out, sbuf);
          println("</th>", out, sbuf);
        }
        print("</tr class=\"boxed\">", out, sbuf);
        while (true) {
          while (rset.next()) {
            println("<tr class=\"boxed\">", out, sbuf);
            for (int i = 1; i <= columnCount; i++) {
              String value = rset.getString(i);
              println("<td class=\"boxed\">", out, sbuf);
              if (!rset.wasNull()) {
                print(value, out, sbuf);
              }
              println("</td>", out, sbuf);
            }
          }
          sqlListPos++;
          if (sqlListPos >= sqlList.length) {
            break;
          }
          rset.close();
          pstmt.close();
          pstmt = prepareStatement(parameterValues, conn, sqlListPos, sqlList);
          rset = pstmt.executeQuery();
        }
        println("</tr></table><br/>", out, sbuf);
      } else if (command.equals(COMMAND_GET)) {
        performGET(out, sbuf, rset);
      } else {
        int startPos = curPos;
        boolean foundRow = false;
        while (true) {
          while (rset.next()) {
            foundRow = true;
            for (int i = 1; i <= columnCount; i++) {
              parameterValues.put(meta.getColumnLabel(i).toUpperCase(),
                  cleanNull(rset.getString(i)));
            }
            if (command.equals(COMMAND_LOOP)) {
              curPos = generateReport(out, sbuf, parameterValues, reportText, startPos, conn);
            } else {
              return curPos;
            }
          }
          sqlListPos++;
          if (sqlListPos >= sqlList.length) {
            break;
          }
          rset.close();
          pstmt.close();
          pstmt = prepareStatement(parameterValues, conn, sqlListPos, sqlList);
          rset = pstmt.executeQuery();
        }
        if (!foundRow && command.equals(COMMAND_LOOP)) {
          curPos = generateReport(null, null, null, reportText, startPos, null);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return curPos;
  }

  private static void performGET(PrintWriter out, StringBuffer sbuf, ResultSet rset)
      throws SQLException {
    if (rset.next()) {
      String value = rset.getString(1);
      if (!rset.wasNull()) {
        print(value, out, sbuf);
      }
    }
  }

  private static PreparedStatement prepareStatement(Map<String, String> parameterValues,
      Connection conn,
      int sqlListPos, String[] sqlList) throws SQLException {
    List<String> queryParams = new ArrayList<>();
    PreparedStatement pstmt;
    sqlList[sqlListPos] = substitute(sqlList[sqlListPos], null, queryParams);
    pstmt = conn.prepareStatement(sqlList[sqlListPos]);
    int i = 0;
    for (Iterator<String> it = queryParams.iterator(); it.hasNext();) {
      String parameter = it.next();
      String value = (String) parameterValues.get(parameter.toUpperCase());
      pstmt.setString(++i, value);
    }
    return pstmt;
  }

  private static void print(String s, PrintWriter out, StringBuffer sbuf) {
    if (out != null) {
      out.print(s);
    }
    if (sbuf != null) {
      sbuf.append(s);
    }
  }

  private static void println(String s, PrintWriter out, StringBuffer sbuf) {
    if (out != null) {
      out.println(s);
    }
    if (sbuf != null) {
      sbuf.append(s);
      sbuf.append("\n");
    }
  }

  private static int print(PrintWriter out, StringBuffer sbuf,
      Map<String, String> parameterValues, String reportText, int curPos, int endPos) {
    if (parameterValues != null) {
      String text = reportText.substring(curPos, endPos);
      text = substitute(text, parameterValues);
      if (out != null) {
        out.print(text);
      }
      if (sbuf != null) {
        sbuf.append(text);
      }
    }
    curPos = endPos;
    return curPos;
  }

  public static String substitute(String string, Map<String, String> parameterValues) {
    return substitute(string, parameterValues, null);
  }

  public static String substitute(String string, Map<String, String> parameterValues,
      List<String> queryParams) {
    StringBuffer sbuf = new StringBuffer(string.length());
    int curPos = 0;
    int nextStartBracket = 0;
    while (curPos < string.length()) {
      nextStartBracket = string.indexOf("${", curPos);
      if (nextStartBracket != -1) {
        int nextEndBracket = string.indexOf("}", nextStartBracket);
        if (nextEndBracket != -1) {
          sbuf.append(string.substring(curPos, nextStartBracket));
          String parameter = string.substring(nextStartBracket + 2, nextEndBracket).trim();
          if (queryParams != null) {
            queryParams.add(parameter);
            sbuf.append(" ? ");
          } else {
            String value = (String) parameterValues.get(parameter.toUpperCase());
            if (value == null) {
              value = "(!!! " + parameter + " NOT FOUND !!!)";
            }
            sbuf.append(value);
          }
          curPos = nextEndBracket + 1;
          continue;
        } else {
          sbuf.append(string.substring(curPos));
          break;
        }
      } else {
        sbuf.append(string.substring(curPos));
        break;
      }
    }
    return sbuf.toString();
  }

  public static String cleanNull(String s) {
    return s == null ? "" : s;
  }

  private static String removeReportTag(String reportText) {
    int posStart = reportText.indexOf("<report");
    if (posStart == -1) {
      throw new IllegalArgumentException("Could not find <report> tag in report defintion text");
    }
    posStart = reportText.indexOf(">", posStart);
    if (posStart == -1 || posStart == reportText.length()) {
      throw new IllegalArgumentException(
          "Could not find properly formatted <report> tag in report defintion text");
    }
    posStart++;
    int posEnd = reportText.indexOf("</report>");
    if (posEnd == -1) {
      throw new IllegalArgumentException("Could not find </report> tag in report definition text");
    }
    return reportText.substring(posStart, posEnd);
  }
}
