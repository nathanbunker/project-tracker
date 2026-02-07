package org.openimmunizationsoftware.pt.api.common;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.CentralControl;

public class HibernateSessionFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = CentralControl.getSessionFactory().openSession();
            HibernateRequestContext.set(session);
            transaction = session.beginTransaction();
            chain.doFilter(request, response);
            if (transaction != null && transaction.isActive()) {
                transaction.commit();
            }
        } catch (Exception ex) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rollbackEx) {
                    // Ignore rollback failures to preserve original exception.
                }
            }
            if (ex instanceof ServletException) {
                throw (ServletException) ex;
            }
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new ServletException(ex);
        } finally {
            HibernateRequestContext.clear();
            ApiRequestContext.clear();
            if (session != null) {
                try {
                    session.close();
                } catch (Exception closeEx) {
                    // Ignore close failures.
                }
            }
        }
    }

    @Override
    public void destroy() {
    }
}
