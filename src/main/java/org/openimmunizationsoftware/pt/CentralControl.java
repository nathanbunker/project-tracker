package org.openimmunizationsoftware.pt;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

public class CentralControl {

  private static SessionFactory factory;

  public static synchronized SessionFactory getSessionFactory() {
    if (factory == null) {
      factory = new AnnotationConfiguration().configure().buildSessionFactory();
    }
    return factory;
  }

  /**
   * Closes the SessionFactory and its c3p0 pool. Called on webapp shutdown so a redeploy does not
   * leave the old pool's MySQL connections open.
   */
  public static synchronized void shutdown() {
    if (factory != null) {
      factory.close();
      factory = null;
    }
  }
}
