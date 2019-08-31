package org.openimmunizationsoftware.dqa;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

public class StartProjectTracker {

  private static final int PORT_NUMBER = 8262;
  private static boolean TRUST_ALL_CERTS = true;

  public static void main(String[] args) throws Exception {

    if (TRUST_ALL_CERTS) {
      TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {

        public X509Certificate[] getAcceptedIssuers() {
          // TODO Auto-generated method stub
          return null;
        }

        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
          // TODO Auto-generated method stub

        }

        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
          // TODO Auto-generated method stub

        }
      }};
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HostnameVerifier allHostsValid = new HostnameVerifier() {

        public boolean verify(String arg0, SSLSession arg1) {
          // TODO Auto-generated method stub
          return true;
        }
      };
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    int timeout = (int) Duration.ONE_HOUR.getMilliseconds();

    Server server = new Server();
    SocketConnector connector = new SocketConnector();

    // Set some timeout options to make debugging easier.
    connector.setMaxIdleTime(timeout);
    connector.setSoLingerTime(-1);
    connector.setPort(PORT_NUMBER);
    server.addConnector(connector);

    // check if a keystore for a SSL certificate is available, and
    // if so, start a SSL connector on port 8443. By default, the
    // quickstart comes with a Apache Wicket Quickstart Certificate
    // that expires about half way september 2021. Do not use this
    // certificate anywhere important as the passwords are available
    // in the source.

    Resource keystore = Resource.newClassPathResource("/keystore");
    if (keystore != null && keystore.exists()) {
      connector.setConfidentialPort(8442);

      SslContextFactory factory = new SslContextFactory();
      factory.setKeyStoreResource(keystore);
      factory.setKeyStorePassword("wicket");
      factory.setTrustStore(keystore);
      factory.setKeyManagerPassword("wicket");
      SslSocketConnector sslConnector = new SslSocketConnector(factory);
      sslConnector.setMaxIdleTime(timeout);
      sslConnector.setPort(8443);
      sslConnector.setAcceptors(4);
      server.addConnector(sslConnector);

      System.out.println("SSL access to the quickstart has been enabled on port 8443");
      System.out.println("You can access the application using SSL on https://localhost:8443");
      System.out.println();
    }

    WebAppContext bb = new WebAppContext();
    bb.setServer(server);
    bb.setContextPath("/");
    bb.setWar("src/main/webapp");

    // START JMX SERVER
    // MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    // MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
    // server.getContainer().addEventListener(mBeanContainer);
    // mBeanContainer.start();

    server.setHandler(bb);

    try {
      System.out.println(
          ">>> STARTING EMBEDDED JETTY SERVER ON PORT " + PORT_NUMBER + ", PRESS ANY KEY TO STOP");
      server.start();
      System.in.read();
      System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
      server.stop();
      server.join();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
