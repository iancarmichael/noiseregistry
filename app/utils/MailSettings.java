package utils;

import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.typesafe.config.ConfigFactory;

public class MailSettings {

	/**
	 * Helper to get the mail session object from JNDI if available (Tomcat instance deployments)
	 * or from the configuration (for Play standalone running)
	 * 
	 * @return javax.mail.Session
	 */
	public static Session getSession() {
		Session session = null;
		
		//Try to get context values (for tomcat deployments):
		Context initCtx;
		try {
			initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
			session = (Session) envCtx.lookup("mail/Session");
			return session;
		} catch (NamingException e) {
			//Logger.error("Unable to get mail session from JNDI");
			//Logger.error(e.getMessage());
		}
		//no session from java naming context, so try using application config		
        Properties props = new Properties();
        props.put("mail.smtp.host", ConfigFactory.load().getString("mail.smtp.host")); 
        props.put("mail.smtp.port", ConfigFactory.load().getString("mail.smtp.port"));
        props.put("mail.smtp.user", ConfigFactory.load().getString("mail.smtp.user"));
        props.put("mail.smtp.auth", ConfigFactory.load().getString("mail.smtp.auth"));
        props.put("mail.smtp.starttls.enable", ConfigFactory.load().getString("mail.smtp.starttls.enable"));
        props.put("mail.smtp.starttls.required", ConfigFactory.load().getString("mail.smtp.starttls.required"));
        
        //props.put("mail.debug", "true");
        if (ConfigFactory.load().getString("mail.smtp.auth").equals("true")) {
	        session = Session.getInstance(props, new javax.mail.Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(
	                		ConfigFactory.load().getString("mail.smtp.user"), ConfigFactory.load().getString("mail.smtp.password"));
	             }
	          });
        } else {
        	session = Session.getInstance(props);
        }
		
		return session;
	}

}
