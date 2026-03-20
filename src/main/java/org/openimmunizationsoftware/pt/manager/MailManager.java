package org.openimmunizationsoftware.pt.manager;

import java.util.Date;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.hibernate.Session;

public class MailManager {
  private String reply = "";
  private String smtpsPassword = "";
  private int smtpsPort = 0;
  private String smtpsUsername = "";
  private boolean useSmtps = false;
  private String address = "";
  private boolean emailEnabled = false;
  private boolean emailDebug = false;

  public MailManager(Session dataSession) {
    reply = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY,
        dataSession);
    smtpsPassword = TrackerKeysManager
        .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, dataSession);
    smtpsPort = parsePort(TrackerKeysManager
      .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT, "0", dataSession));
    smtpsUsername = TrackerKeysManager
        .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME, dataSession);
    useSmtps = TrackerKeysManager
        .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS, dataSession)
        .equals("Y");
    address = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS,
        dataSession);
    emailEnabled = TrackerKeysManager.getApplicationKeyValueBoolean(
        TrackerKeysManager.KEY_SYSTEM_EMAIL_ENABLE, false, dataSession);
    emailDebug = TrackerKeysManager.getApplicationKeyValueBoolean(
      TrackerKeysManager.KEY_SYSTEM_EMAIL_DEBUG, false, dataSession);
  }

  public void sendEmail(String subject, String body, String to) throws Exception {
    sendEmail(subject, body, to, null);
  }

  public void sendEmail(String subject, String body, String to, String cc) throws Exception {
    if (!emailEnabled) {
      return;
    }

    String smtpHost = normalizeHost(address, smtpsUsername);
    int smtpPort = resolvePort(smtpsPort, useSmtps);
    String fromAddress = reply == null || reply.trim().equals("") ? smtpsUsername : reply;

    Properties props = new Properties();
    props.put("mail.transport.protocol", useSmtps ? "smtps" : "smtp");
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", String.valueOf(smtpPort));
    props.put("mail.smtp.connectiontimeout", "15000");
    props.put("mail.smtp.timeout", "15000");
    props.put("mail.smtp.writetimeout", "15000");
    props.put("mail.smtp.auth", "true");

    if (useSmtps) {
      props.put("mail.smtps.host", smtpHost);
      props.put("mail.smtps.port", String.valueOf(smtpPort));
      props.put("mail.smtps.auth", "true");
      props.put("mail.smtps.ssl.enable", "true");
      props.put("mail.smtps.ssl.protocols", "TLSv1.2");
      props.put("mail.smtps.quitwait", "false");
      if (isGmailHost(smtpHost)) {
        props.put("mail.smtps.ssl.trust", "smtp.gmail.com");
      }
    } else {
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
      props.put("mail.smtp.ssl.protocols", "TLSv1.2");
      if (isGmailHost(smtpHost)) {
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
      }
    }

    javax.mail.Session session = javax.mail.Session.getInstance(props, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(smtpsUsername, smtpsPassword);
      }
    });
    session.setDebug(emailDebug);

    MimeMessage msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(fromAddress));
    msg.setReplyTo(new InternetAddress[] { new InternetAddress(fromAddress) });
    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
    if (cc != null && !cc.trim().equals("")) {
      msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc, false));
    }
    msg.setSubject(subject, "UTF-8");
    msg.setSentDate(new Date());
    msg.setContent(body, "text/html; charset=UTF-8");
    msg.setHeader("X-Mailer", "Tracker");

    Transport transport = session.getTransport(useSmtps ? "smtps" : "smtp");
    try {
      transport.connect(smtpHost, smtpPort, smtpsUsername, smtpsPassword);
      transport.sendMessage(msg, msg.getAllRecipients());
    } finally {
      transport.close();
    }
  }

  private static int parsePort(String value) {
    try {
      return Integer.parseInt(value == null ? "0" : value.trim());
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }

  private static int resolvePort(int configuredPort, boolean useSmtps) {
    if (configuredPort > 0) {
      return configuredPort;
    }
    return useSmtps ? 465 : 587;
  }

  private static String normalizeHost(String configuredHost, String username) {
    if (configuredHost != null && !configuredHost.trim().equals("")) {
      return configuredHost.trim();
    }
    if (username != null && username.toLowerCase().endsWith("@gmail.com")) {
      return "smtp.gmail.com";
    }
    return "localhost";
  }

  private static boolean isGmailHost(String host) {
    if (host == null) {
      return false;
    }
    String h = host.toLowerCase();
    return h.equals("smtp.gmail.com") || h.endsWith(".gmail.com");
  }
}
