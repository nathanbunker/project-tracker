# Deployment Notes for Project Tracker

## Target Stack

* **Domain:** Namecheap or Cloudflare Registrar (~$12/year)
* **Hosting:** Hetzner VPS (~$5–$8/month)
* **Email:** Zoho or Migadu (~$1–$3/month)
* **Application:** Java Servlet app on Tomcat
* **Database:** MySQL
* **Reverse Proxy / TLS:** Nginx + Let's Encrypt

## Goal

Deploy the application to a small VPS with:

* a real domain name
* HTTPS
* Tomcat running the Java web application
* MySQL running locally on the server
* SMTP email sending through a provider on the custom domain
* basic backup and security setup

---

# Phase 1 - Buy and Prepare Services

## 1. Buy a domain

Checklist:

* [ ] Choose a domain name
* [ ] Register the domain through Namecheap or Cloudflare Registrar
* [ ] Enable domain management access
* [ ] Decide what hostname will run the app:

  * [ ] `example.com`
  * [ ] `app.example.com`
* [ ] Decide what sender email address to use:

  * [ ] `noreply@example.com`
  * [ ] `me@example.com`

Notes:

* Using `app.example.com` for the servlet app is often simpler.
* Keep the root domain available for a future landing page if needed.

## 2. Buy a VPS

Checklist:

* [ ] Create an account with Hetzner
* [ ] Choose a small VPS plan in the ~$5–$8/month range
* [ ] Select **Ubuntu LTS** as the operating system
* [ ] Add your SSH key during setup if possible
* [ ] Note the server IP address
* [ ] Confirm you can SSH into the server

Suggested minimum starting point:

* 2 vCPU
* 4 GB RAM preferred if affordable
* 40+ GB disk

A smaller server may still work, but Java + Tomcat + MySQL are more comfortable with some headroom.

## 3. Set up email service

Checklist:

* [ ] Create an account with Zoho or Migadu
* [ ] Add your domain to the mail provider
* [ ] Follow their DNS verification steps
* [ ] Create a mailbox or sender identity
* [ ] Create SMTP credentials if separate from mailbox password
* [ ] Record SMTP settings:

  * [ ] SMTP host
  * [ ] Port
  * [ ] Username
  * [ ] Password
  * [ ] Whether to use STARTTLS on port 587

Notes:

* Prefer a dedicated sender such as `noreply@yourdomain.com`.
* Avoid using a personal mailbox for application infrastructure.

---

# Phase 2 - DNS Setup

## 4. Point domain to the VPS

Checklist:

* [ ] Open DNS management for the domain
* [ ] Create an `A` record for the app hostname pointing to the VPS IP
* [ ] If using `app.example.com`, create:

  * [ ] `A  app  -> <server IP>`
* [ ] If using the root domain, create:

  * [ ] `A  @  -> <server IP>`
* [ ] Wait for DNS propagation
* [ ] Verify with `ping`, `nslookup`, or `dig`

## 5. Add mail DNS records

Checklist:

* [ ] Add MX records required by Zoho or Migadu
* [ ] Add SPF record
* [ ] Add DKIM record
* [ ] Add DMARC record
* [ ] Verify domain mail setup in the provider dashboard

Notes:

* This matters for deliverability.
* Do not skip SPF/DKIM/DMARC if the application will send real emails.

---

# Phase 3 - Base VPS Setup

## 6. Log into the server and update packages

Checklist:

* [ ] SSH into the VPS
* [ ] Update package lists
* [ ] Upgrade installed packages
* [ ] Reboot if needed

Typical commands:

```bash
sudo apt update
sudo apt upgrade -y
sudo reboot
```

## 7. Create a non-root deployment user

Checklist:

* [ ] Create a normal user account
* [ ] Add the user to sudo
* [ ] Confirm SSH login works for that user
* [ ] Disable password login later if using SSH keys

Typical commands:

```bash
sudo adduser deploy
sudo usermod -aG sudo deploy
```

## 8. Secure SSH and firewall

Checklist:

* [ ] Confirm SSH key login works
* [ ] Install and configure UFW firewall
* [ ] Allow OpenSSH
* [ ] Allow HTTP
* [ ] Allow HTTPS
* [ ] Enable firewall
* [ ] Disable root SSH login if practical
* [ ] Disable password authentication if practical

Typical commands:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

---

# Phase 4 - Install Runtime Stack

## 9. Install Java

Checklist:

* [ ] Install a supported JDK
* [ ] Verify Java version

Typical commands:

```bash
sudo apt install -y openjdk-21-jdk
java -version
```

Use the version your application and Tomcat support best. If the project is already tested on Java 17, stay with Java 17.

## 10. Install Tomcat

Checklist:

* [ ] Install Tomcat or deploy a standalone Tomcat manually
* [ ] Confirm the Tomcat service starts
* [ ] Confirm it listens on localhost or the local interface
* [ ] Decide where WAR deployments will go

Notes:

* Package-managed Tomcat is simple.
* Manual Tomcat install can give you more version control.
* For a single personal app, either is acceptable.

## 11. Install MySQL

Checklist:

* [ ] Install MySQL server
* [ ] Run initial secure setup
* [ ] Set root/admin password if needed
* [ ] Confirm MySQL starts automatically

Typical commands:

```bash
sudo apt install -y mysql-server
sudo systemctl enable mysql
sudo systemctl start mysql
sudo mysql_secure_installation
```

## 12. Lock MySQL down

Checklist:

* [ ] Confirm MySQL is **not** exposed publicly
* [ ] Bind MySQL to localhost if needed
* [ ] Do **not** open port 3306 in the firewall
* [ ] Create an application database
* [ ] Create an application database user
* [ ] Grant only needed privileges

Notes:

* The app and DB can live on the same VPS.
* That is fine for an initial deployment.

## 13. Install Nginx

Checklist:

* [ ] Install Nginx
* [ ] Start and enable Nginx
* [ ] Confirm default web page loads over HTTP

Typical commands:

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

---

# Phase 5 - Application Setup

## 14. Prepare database schema

Checklist:

* [ ] Copy `schema.sql` to the server
* [ ] Create the production database
* [ ] Run schema creation script
* [ ] Load any seed or settings data needed
* [ ] Test login from the application DB user

## 15. Configure application settings

Checklist:

* [ ] Set production database connection values
* [ ] Set mail SMTP host, port, username, password
* [ ] Set application base URL
* [ ] Set reply-to address
* [ ] Turn off debug settings not needed in production
* [ ] Confirm secrets are not hard-coded in source if avoidable

Notes:

* At minimum, keep production credentials out of Git.
* Environment variables, external config files, or protected settings tables are better than embedding secrets in code.

## 16. Build and deploy the WAR

Checklist:

* [ ] Build the WAR locally or from CI
* [ ] Copy WAR to server
* [ ] Deploy to Tomcat
* [ ] Restart Tomcat if needed
* [ ] Confirm app starts cleanly
* [ ] Check Tomcat logs for errors

Common deployment flow:

* Build locally with Maven or Gradle
* Copy WAR with `scp`
* Place it in Tomcat's webapps directory
* Restart Tomcat if auto-deploy is not enough

## 17. Test the app locally on the server

Checklist:

* [ ] Use `curl` against localhost to confirm app responses
* [ ] Confirm the app connects to MySQL
* [ ] Confirm no startup exceptions appear in logs

---

# Phase 6 - Reverse Proxy and HTTPS

## 18. Configure Nginx as reverse proxy to Tomcat

Checklist:

* [ ] Create an Nginx site config for the app hostname
* [ ] Proxy requests from Nginx to Tomcat
* [ ] Set standard proxy headers
* [ ] Test Nginx config
* [ ] Reload Nginx

Typical design:

* Public traffic comes to Nginx on ports 80/443
* Nginx forwards to Tomcat on localhost:8080
* Tomcat is not exposed directly to the internet

## 19. Add HTTPS with Let's Encrypt

Checklist:

* [ ] Install Certbot
* [ ] Request certificate for the hostname
* [ ] Configure automatic renewal
* [ ] Test HTTPS in browser
* [ ] Confirm HTTP redirects to HTTPS

Typical commands:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx
```

---

# Phase 7 - Email Setup in the Application

## 20. Configure SMTP in the app

Checklist:

* [ ] Enter SMTP host from Zoho or Migadu
* [ ] Use port 587 unless provider says otherwise
* [ ] Enable SMTP auth
* [ ] Enable STARTTLS
* [ ] Enter full sender email address as username
* [ ] Enter mailbox password or SMTP credential
* [ ] Save settings

## 21. Test outbound email

Checklist:

* [ ] Send a test email from the application
* [ ] Confirm it arrives externally
* [ ] Check spam/junk folder
* [ ] Review SMTP and application logs if it fails
* [ ] Confirm SPF/DKIM/DMARC are passing if provider offers diagnostics

---

# Phase 8 - Operations and Maintenance

## 22. Set up backups

Checklist:

* [ ] Create automated MySQL dumps
* [ ] Store backups off-server
* [ ] Back up application config
* [ ] Back up deployed WAR or release artifact
* [ ] Test restoring from backup at least once

Minimum backup targets:

* database dump
* application configuration
* uploaded files, if any
* key deployment notes

## 23. Basic monitoring

Checklist:

* [ ] Know where Nginx logs are
* [ ] Know where Tomcat logs are
* [ ] Know where MySQL logs are
* [ ] Check disk space regularly
* [ ] Check memory use occasionally
* [ ] Set up simple uptime monitoring later if desired

## 24. Updates and security maintenance

Checklist:

* [ ] Apply Ubuntu security updates regularly
* [ ] Keep Java updated
* [ ] Keep Tomcat updated
* [ ] Keep MySQL updated
* [ ] Renew certificates automatically and verify renewal works

---

# Suggested Initial Architecture

```text
Internet
   |
   v
Nginx (80/443, TLS)
   |
   v
Tomcat (localhost:8080)
   |
   +--> MySQL (localhost only)
   |
   +--> SMTP Provider (Zoho or Migadu)
```

---

# Initial Production Decisions

Use this as a quick planning checklist.

## Domain

* [ ] Final domain name selected
* [ ] App hostname selected
* [ ] Sender email address selected

## Hosting

* [ ] Hetzner VPS plan selected
* [ ] Ubuntu LTS selected
* [ ] SSH key ready

## Database

* [ ] Stay on MySQL
* [ ] Create production DB/user
* [ ] Plan automated dumps

## App Runtime

* [ ] Java version chosen
* [ ] Tomcat install approach chosen
* [ ] WAR deployment process chosen

## Email

* [ ] Zoho or Migadu selected
* [ ] SMTP credentials created
* [ ] DNS mail records configured

## Security

* [ ] Firewall enabled
* [ ] MySQL not internet-exposed
* [ ] HTTPS enabled
* [ ] Secrets kept out of source control

---

# Minimum First Milestone

A good first deployment target is just this:

* [ ] Domain resolves to server
* [ ] Nginx running
* [ ] HTTPS works
* [ ] Tomcat serves the app
* [ ] App connects to MySQL
* [ ] App sends a test email
* [ ] Database backups run nightly

Once that works, everything else is refinement.

---

# Notes to Myself

* Do not overbuild this.
* One VPS is enough to start.
* Keep MySQL local to the server.
* Use SMTP from a real domain mailbox, not personal Gmail.
* Backups matter more than clever architecture.
* Keep deployment steps documented as I go.
