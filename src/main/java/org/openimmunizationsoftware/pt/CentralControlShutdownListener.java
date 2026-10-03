package org.openimmunizationsoftware.pt;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

/**
 * Releases database resources when the webapp stops. Registered first in web.xml so that it is
 * destroyed last, after listeners that may still use the SessionFactory (such as the template
 * scheduler) have shut down.
 */
public class CentralControlShutdownListener implements ServletContextListener {

  private static final Logger LOGGER = Logger.getLogger(CentralControlShutdownListener.class.getName());

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // nothing to do; the SessionFactory is built lazily
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    try {
      CentralControl.shutdown();
    } catch (RuntimeException e) {
      LOGGER.log(Level.WARNING, "Unable to close Hibernate SessionFactory", e);
    }
    ClassLoader webappClassLoader = Thread.currentThread().getContextClassLoader();
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      if (driver.getClass().getClassLoader() == webappClassLoader) {
        try {
          DriverManager.deregisterDriver(driver);
        } catch (SQLException e) {
          LOGGER.log(Level.WARNING, "Unable to deregister JDBC driver " + driver, e);
        }
      }
    }
    AbandonedConnectionCleanupThread.checkedShutdown();
  }
}
