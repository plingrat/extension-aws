/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
 */

package org.entermedia.email;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openedit.ModuleManager;
import com.openedit.OpenEditRuntimeException;
import com.openedit.page.manage.PageManager;

public class PostMail 
{
	private static final Log log = LogFactory.getLog(PostMail.class);
	protected String fieldSmtpUsername;
	protected String fieldSmtpPassword;
	protected String fieldSmtpServer = "localhost";
	protected Integer fieldPort;
	protected boolean fieldSmtpSecured = false;
	protected PageManager fieldPageManager;
	protected boolean fieldSslEnabled = false;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public String getSmtpPassword() {
		return fieldSmtpPassword;
	}

	public void setSmtpPassword(String inSmtpPassword) {
		this.fieldSmtpPassword = inSmtpPassword;
	}

	public boolean isSmtpSecured() {
		return fieldSmtpSecured;
	}

	public void setSmtpSecured(boolean inSmtpSecured) {
		this.fieldSmtpSecured = inSmtpSecured;
	}

	public String getSmtpUsername() {
		return fieldSmtpUsername;
	}

	public void setSmtpUsername(String inSmtpUsername) {
		this.fieldSmtpUsername = inSmtpUsername;
	}

	public void postMail(String recipient, String subject, String message,
			String from) throws MessagingException {
		postMail(new String[] { recipient }, subject, message, null, from);
	}

	// returns a new template web email instance preconfigured with spring
	// settings.
	public TemplateWebEmail getTemplateWebEmail() {
		TemplateWebEmail email = null;
		if (getModuleManager()!=null)
		{
			email = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring
		}
		if (email == null)
		{
			email = new TemplateWebEmail();
		}
		email.setPostMail(this);
		email.setPageManager(getPageManager());
		return email;
	}

	public void postMail(String[] recipients, String subject, String inHtml,
			String inText, String from) throws MessagingException {
		postMail(recipients, subject, inHtml, inText, from, null, null);
	}
	
	public void postMail(String[] recipients, String subject, String inHtml,
			String inText, String from, List inAttachments, Map inProperties)
			throws MessagingException {
		postMail(recipients,new String[]{},subject,inHtml,inText,from,inAttachments,inProperties);
	}
	
	public void postMail(List<Recipient> recipients, List<Recipient> blindrecipients, String subject, 
			String inHtml, String inText, String from, List inAttachments, Map inProperties)
			throws MessagingException {
		ArrayList<String> list = new ArrayList<String>();
		if (recipients!=null){
			for (Recipient recipient:recipients){
				if (recipient.getEmailAddress()!=null && !list.contains(recipient.getEmailAddress())){
					list.add(recipient.getEmailAddress());
				}
			}
		}
		String [] ccarr = list.toArray(new String[list.size()]);//array of recipients
		list.clear();
		if (blindrecipients!=null){
			for (Recipient recipient:blindrecipients){
				if (recipient.getEmailAddress()!=null && !list.contains(recipient.getEmailAddress())){
					list.add(recipient.getEmailAddress());
				}
			}
		}
		String [] bccarr = list.toArray(new String[list.size()]);
		postMail(ccarr, bccarr, subject, inHtml, inText, from, inAttachments, inProperties);
	}
	
	public void postMail(String [] recipients, List<Recipient> blindrecipients, String subject,
			String inHtml, String inText, String from, List inAttachments, Map inProperties)
			throws MessagingException {
		ArrayList<String> list = new ArrayList<String>();
		if (recipients!=null){
			for (String recipient:recipients){
				if (!list.contains(recipient))
					list.add(recipient);
			}
		}
		String [] ccarr = list.toArray(new String[list.size()]);//array of recipients
		list.clear();
		if (blindrecipients!=null){
			for (Recipient recipient:blindrecipients){
				if (recipient.getEmailAddress()!=null && !list.contains(recipient.getEmailAddress()))
					list.add(recipient.getEmailAddress());
			}
		}
		String [] bccarr = list.toArray(new String[list.size()]);
		postMail(ccarr, bccarr, subject, inHtml, inText, from, inAttachments, inProperties);
	}

	public void postMail(String[] recipients, String [] blindrecipients, String subject, String inHtml,
			String inText, String from, List inAttachments, Map inProperties)
			throws MessagingException {
		// Set the host smtp address
		Properties props = new Properties();
		// create some properties and get the default Session
		props.put("mail.smtp.host", fieldSmtpServer);
		props.put("mail.smtp.port", String.valueOf(getPort()));
		props.put("mail.smtp.auth", new Boolean(fieldSmtpSecured).toString());
		if (isSslEnabled()) {
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
		}
		
		Session session;
		// If we need to authenticate, create the authenticator
		if (fieldSmtpSecured)
		{
			SmtpAuthenticator auth = new SmtpAuthenticator();
			session = Session.getInstance(props, auth);
		} else {
			session = Session.getInstance(props);
		}
		// session.setDebug(debug);

		// create a message
		Message msg = new MimeMessage(session);
		MimeMultipart mp = null;
		// msg.setDataHandler(new DataHandler(new ByteArrayDataSource(message,
		// "text/html")));
		
		if(inAttachments != null && inAttachments.size() == 0)
		{
			inAttachments = null;
		}
		
		if (inText != null && inHtml != null  || inAttachments != null) {
			// Create an "Alternative" Multipart message
			mp = new MimeMultipart("mixed");
			
			if(inText != null){
				BodyPart messageBodyPart = new MimeBodyPart();
		
				messageBodyPart.setContent(inText, "text/plain");
				mp.addBodyPart(messageBodyPart);
			}
			if(inHtml != null){
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setContent(inHtml, "text/html");
				mp.addBodyPart(messageBodyPart);
			}
			if (inAttachments != null) {
				for (Iterator iterator = inAttachments.iterator(); iterator
						.hasNext();) {
					String filename = (String) iterator.next();
	
					File file = new File(filename);
	
					if (file.exists() && !file.isDirectory()) {
						// create the second message part
						MimeBodyPart mbp = new MimeBodyPart();
						
						FileDataSource fds = new FileDataSource(file);
						mbp.setDataHandler(new DataHandler(fds));
						
						mbp.setFileName(fds.getName());
						
	
						mp.addBodyPart(mbp);
					}
				}
			}
			
			msg.setContent(mp);
			
		}
		else if (inHtml != null)
		{
			msg.setContent(inHtml, "text/html");
		}
		else
		{
			msg.setContent(inText, "text/plain");
		}
		// set the from and to address
		InternetAddress addressFrom = new InternetAddress(from);
		msg.setFrom(addressFrom);
		//msg.setRecipient(RecipientType.BCC, addressFrom);
		msg.setSentDate(new Date());
		if (recipients == null || recipients.length == 0) {
			throw new MessagingException("No recipients specified");
		}
		InternetAddress[] addressTo = new InternetAddress[recipients.length];

		for (int i = 0; i < recipients.length; i++) {
			String rec = recipients[i];
			if(rec == null || rec.length() <= 0)
			{
				continue;
			}
			addressTo[i] = new InternetAddress(rec);
			String personal = addressTo[i].getPersonal();
			if (personal != null && personal.indexOf("\"") == -1) {
				// check for commas or . and quote it if found
				if (personal.indexOf(",") > -1 || personal.indexOf(".") > -1) {
					personal = "\"" + personal + "\"";
					try {
						addressTo[i].setPersonal(personal);
					} catch (UnsupportedEncodingException ex) {
						throw new OpenEditRuntimeException(ex);
					}
				}
			}
		}

		msg.setRecipients(Message.RecipientType.TO, addressTo);
		
		//add bcc
		if (blindrecipients!=null && blindrecipients.length > 0)
		{
			InternetAddress[] addressBcc = new InternetAddress[blindrecipients.length];
			for (int i = 0; i < blindrecipients.length; i++) {
				String rec = blindrecipients[i];
				if(rec == null || rec.length() <= 0)
				{
					continue;
				}
				addressBcc[i] = new InternetAddress(rec);
				String personal = addressBcc[i].getPersonal();
				if (personal != null && personal.indexOf("\"") == -1) {
					// check for commas or . and quote it if found
					if (personal.indexOf(",") > -1 || personal.indexOf(".") > -1) {
						personal = "\"" + personal + "\"";
						try {
							addressBcc[i].setPersonal(personal);
						} catch (UnsupportedEncodingException ex) {
							throw new OpenEditRuntimeException(ex);
						}
					}
				}
			}
			msg.setRecipients(Message.RecipientType.BCC, addressBcc);
		}
		

		// Optional : You can also set your custom headers in the Email if you
		// Want
		// msg.addHeader("MyHeaderName", "myHeaderValue");
		// Setting the Subject and Content Type
		msg.setSubject(subject);

		// Transport tr = session.getTransport("smtp");
		// tr.connect(serverandport[0], null, null);
		// msg.saveChanges(); // don't forget this
		// tr.sendMessage(msg, msg.getAllRecipients());
		// tr.close();
		// msg.setContent(msg, "text/plain");
		
		Transport.send(msg);
		log.info("sent email " + subject);
	}

	public int getPort() 
	{
		if( fieldPort == null)
		{
			fieldPort = Integer.getInteger("mail.smtp.port");
			if(fieldPort == null)
			{
				fieldPort = new Integer(25);
			}
		}
		return fieldPort;
	}

	public void setPort(int inPort) {
		this.fieldPort = new Integer(inPort);
	}
	public void setPort(Integer inPort) {
		this.fieldPort = inPort;
	}

	public String getSmtpServer() {
		return fieldSmtpServer;
	}

	public void setSmtpServer(String inSmtpServer) {
		this.fieldSmtpServer = inSmtpServer;
	}

	public class SmtpAuthenticator extends javax.mail.Authenticator {
		public javax.mail.PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(fieldSmtpUsername,
					fieldSmtpPassword);
		}
	}

	public boolean isSslEnabled() {
		return fieldSslEnabled;
	}

	public void setSslEnabled(boolean inSslEnabled) {
		fieldSslEnabled = inSslEnabled;
	}

}