package org.openimmunizationsoftware.pt.manager;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.eclipse.jetty.server.Authentication.SendSuccess;
import org.hibernate.Session;

public class MailManager
{
  private String reply = "";
  private String smtpsPassword = "";
  private int smtpsPort = 0;
  private String smtpsUsername = "";
  private boolean useSmtps = false;
  private String address = "";

  public MailManager(Session dataSession) {
    reply = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY, dataSession);
    smtpsPassword = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, dataSession);
    smtpsPort = Integer.parseInt(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT, "0", dataSession));
    smtpsUsername = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME, dataSession);
    useSmtps = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS, dataSession).equals("Y");
    address = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS, dataSession);
  }

  public void sendEmail(String subject, String body, String to) throws Exception
  {
    if (useSmtps)
    {
      // java.security.Security.addProvider(new
      // com.sun.net.ssl.internal.ssl.Provider());
      String smptsHost = address;
      Properties props = System.getProperties();
      props.put("mail.transport.protocol", "smtps");
      props.put("mail.smtp.host", smptsHost);
      props.put("mail.smtps.auth", "true");
      props.put("mail.smtps.quitwait", "false");
      javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, null);
      MimeMessage msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(reply));
      String[] t = to.split("\\,");
      InternetAddress[] addresses = new InternetAddress[t.length];
      for (int i = 0; i < t.length; i++)
      {
        addresses[i] = new InternetAddress(t[i].trim());
      }
      msg.setRecipients(Message.RecipientType.TO, addresses);
      msg.setSubject(subject);
      msg.setSentDate(new Date());
      msg.setContent(body, "text/html; charset=UTF-8");
      msg.setHeader("X-Mailer", "Tracker");
      Transport transport = session.getTransport();
      transport.connect(smptsHost, smtpsPort, smtpsUsername, smtpsPassword);
      msg.saveChanges();
      transport.sendMessage(msg, msg.getRecipients(Message.RecipientType.TO));
      transport.close();
    } else
    {
      Properties props = new Properties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "false");
      javax.mail.Session session = javax.mail.Session.getInstance(props, null);
      MimeMessage msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(reply));
      String[] t = to.split("\\,");
      InternetAddress[] addresses = new InternetAddress[t.length];
      for (int i = 0; i < t.length; i++)
      {
        addresses[i] = new InternetAddress(t[i].trim());
      }
      msg.setRecipients(Message.RecipientType.TO, addresses);
      msg.setSubject(subject);
      msg.setSentDate(new Date());
      msg.setContent(body, "text/html; charset=UTF-8");
      msg.setHeader("X-Mailer", "Tracker");
      Transport tr = session.getTransport("smtp");
      tr.connect(address, smtpsUsername, smtpsPassword);
      msg.saveChanges();
      tr.sendMessage(msg, addresses);
      tr.close();
    }
  }
}
