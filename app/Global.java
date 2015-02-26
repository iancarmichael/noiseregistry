import models.ActivityApplication;
import models.NoiseProducer;
import models.Regulator;

import com.typesafe.config.ConfigFactory;

import akka.actor.Cancellable;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.libs.Akka;
import play.libs.Time.CronExpression;
import play.twirl.api.Html;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import utils.AppConfigSettings;
import utils.MailSettings;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.hibernate.jpa.HibernatePersistenceProvider;

public class Global extends GlobalSettings {

	private Cancellable scheduler;
	
	
	/* Fix for an issue with the Hibernate Persistence Provider, as detailed here:
	 * https://hibernate.atlassian.net/browse/HHH-9141
	 * 
	 * @see play.GlobalSettings#beforeStart(play.Application)
	 */
	@Override
	public void beforeStart(Application arg0) {
		PersistenceProviderResolverHolder.setPersistenceProviderResolver(new PersistenceProviderResolver() {
	        private final List<PersistenceProvider> providers_ = Arrays.asList((PersistenceProvider) new HibernatePersistenceProvider());

	        @Override
	        public void clearCachedProviders() {
	            //
	        }

	        @Override
	        public List<PersistenceProvider> getPersistenceProviders() { 
	            return providers_; 
	        }
	    });
		super.beforeStart(arg0);
	}

	@Override
	public void onStart(Application application) {
	    super.onStart(application); 
	    
	    schedule(); 
	}

	@Override
	public void onStop(Application application) {
	    //Stop the scheduler
	    if (scheduler != null) {
	        scheduler.cancel();
	    }
	}

	private void schedule() {
	    try {
	    	FiniteDuration d;
	    	Date nextValidTimeAfter = new Date();
	    	String cronExpr = ConfigFactory.load().getString("email.weekly.cron");
	    	if (!cronExpr.equals("")) {
		    	String sDebugCron = ConfigFactory.load().getString("email.weekly.crondebug");
			    if (sDebugCron.compareTo("")!=0)
			    {
			    	Logger.debug("Debug schedule!!!");
			    	Date now = new Date();
			    	nextValidTimeAfter.setTime(now.getTime() + 2 * 60 * 1000); 
			    }
			    else
			    {
			    	CronExpression e = new CronExpression(cronExpr);
			    	nextValidTimeAfter = e.getNextValidTimeAfter(new Date());
			    }
		    	d = Duration.create(
		    			nextValidTimeAfter.getTime() - System.currentTimeMillis(), 
		    			TimeUnit.MILLISECONDS);
		        
		        Logger.debug("Scheduling to run at "+nextValidTimeAfter);
		        Logger.debug("Application hostname for external links: " + AppConfigSettings.getConfigString("externalHostname", "application.hostname"));
			    Logger.debug("Email override for regulator emails: " + AppConfigSettings.getConfigString("regulatorOverrideAddress", "email.regulator_override_address"));
			    Logger.debug("Email override for noiseproducer emails: " + AppConfigSettings.getConfigString("regulatorOverrideAddress", "email.noiseproducer_override_address"));
			    
		        scheduler = Akka.system().scheduler().scheduleOnce(d, new Runnable() {
	
			        @Override
			        public void run() {
			            Logger.debug("Runing scheduler");
			            //Do your tasks here
			            notifyLateToRegulatorsWrapper();
			            notifyLateToNoiseProducersWrapper();
			            
			            schedule(); //Schedule for next time
		
			        }
		        }, Akka.system().dispatcher());
	    	}
	    } catch (Exception e) {
	        Logger.error("", e);
	    }
	}
	
	/**
	 * Simple transaction wrapper around the notification routine.
	 */
	private void notifyLateToRegulatorsWrapper() {
		JPA.withTransaction(new play.libs.F.Callback0() {
			 @Override
	         public void invoke() throws Throwable {
				 notifyLateToRegulators();
			 }
		});
	}
	/**
	 * Notification handler.  This must be called with a JPA transaction wrapper.
	 */
	private void notifyLateToRegulators() {
		
		String statusKey = "latecloseout";
		if (Messages.get("regulator.activity." + statusKey + ".mail.subject").equals("regulator.activity." + statusKey + ".mail.subject")) {
			//no message translation - don't try to send mail out in this case...
			Logger.error("No regulator late close out mail configuration settings found.  No notifications will be sent to regulators.");
			return;
		}
		List<Regulator> regs = Regulator.findAll();
		Iterator<Regulator> it = regs.iterator();
		while (it.hasNext()) {
			Regulator reg = it.next();
			if (reg.getAccepts_email().booleanValue()) {
				List<ActivityApplication> apps = ActivityApplication.findLateByRegulator(reg);
				if (!apps.isEmpty()) {
					try {
						Session session = MailSettings.getSession();
						String regulatorEmail = reg.getOrganisation().getContact_email();
						String regulatorContactname = reg.getOrganisation().getContact_name();
						
						String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");
						String overrideAddress = AppConfigSettings.getConfigString("regulatorOverrideAddress", "email.regulator_override_address");
						
						String hostname = AppConfigSettings.getConfigString("externalHostname", "application.hostname");
						
						InternetAddress[] addresses = new InternetAddress[1];
						
						if (overrideAddress.equals("")) {
							addresses[0] = new InternetAddress(regulatorEmail);
						} else {
							Logger.error("Regulator email override is in effect - email will not be sent to the regulator address");
							addresses[0] = new InternetAddress(overrideAddress);
						}
						Html mailBody = views.html.email.regulatorlatecloseout.render(apps, hostname, statusKey, regulatorContactname, regulatorEmail, overrideAddress);
						
				        MimeMessage message = new MimeMessage(session);
				        message.setSubject(Messages.get("regulator.activity." + statusKey + ".mail.subject"));
				        message.setContent(mailBody.body(), "text/html");
				        message.setRecipients(Message.RecipientType.TO, addresses);
				        message.setFrom(new InternetAddress(mailFrom));
				        // set the message content here
				        Transport t = session.getTransport("smtp");
				        t.connect();
				        t.sendMessage(message, message.getAllRecipients());
				        t.close();
					 } catch (MessagingException me) {
						 me.printStackTrace();
					 }	
				} else {
					Logger.error("No late applications found for regulator: " + reg.getOrganisation().getOrganisation_name());
				}
			} else {
				Logger.error("Regulator " + reg.getOrganisation().getOrganisation_name() + " is not set to receive email - not sending activity late close out mail.");
			}
		}
	}
	/**
	 * Simple transaction wrapper around the notification routine.
	 */
	private void notifyLateToNoiseProducersWrapper() {
		JPA.withTransaction(new play.libs.F.Callback0() {
			 @Override
	         public void invoke() throws Throwable {
				 notifyLateToNoiseProducers();
			 }
		});
	}	
	/**
	 * Notification handler.  This must be called with a JPA transaction wrapper.
	 */
	private void notifyLateToNoiseProducers() {
		
		String statusKey = "latecloseout";
		if (Messages.get("noiseproducer.activity." + statusKey + ".mail.subject").equals("noiseproducer.activity." + statusKey + ".mail.subject")) {
			//no message translation - don't try to send mail out in this case...
			Logger.error("No noise producer late close out mail configuration settings found.  No notifications will be sent to noise producers.");
			return;
		}
		List<NoiseProducer> nps = NoiseProducer.findAll();
		Iterator<NoiseProducer> it = nps.iterator();
		while (it.hasNext()) {
			NoiseProducer np = it.next();
			List<ActivityApplication> apps = ActivityApplication.findLateByNoiseProducer(np);
			if (!apps.isEmpty()) 
			{
				try 
				{
					Session session = MailSettings.getSession();
					String noiseproducerEmail = np.getOrganisation().getContact_email();
					String noiseproducerContactname = np.getOrganisation().getContact_name();
						
					String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");
					String overrideAddress = AppConfigSettings.getConfigString("noiseProducerOverrideAddress", "email.noiseproducer_override_address");
						
					String hostname = AppConfigSettings.getConfigString("externalHostname", "application.hostname");
						
					InternetAddress[] addresses = new InternetAddress[1];
						
					if (overrideAddress.equals("")) {
						addresses[0] = new InternetAddress(noiseproducerEmail);
					} else {
						Logger.error("Noise producer email override is in effect - email will not be sent to the noise producer address");
						addresses[0] = new InternetAddress(overrideAddress);
					}
					Html mailBody = views.html.email.noiseproducerlatecloseout.render(apps, hostname, statusKey, noiseproducerContactname, noiseproducerEmail, overrideAddress);
					
				    MimeMessage message = new MimeMessage(session);
				    message.setSubject(Messages.get("noiseproducer.activity." + statusKey + ".mail.subject"));
				    message.setContent(mailBody.body(), "text/html");
				    message.setRecipients(Message.RecipientType.TO, addresses);
				    message.setFrom(new InternetAddress(mailFrom));
				    // set the message content here
				    Transport t = session.getTransport("smtp");
				    t.connect();
				    t.sendMessage(message, message.getAllRecipients());
				    t.close();
				} catch (MessagingException me) {
					me.printStackTrace();
				}	
			} else {
				Logger.error("No late applications found for noise producer: " + np.getOrganisation().getOrganisation_name());
			}
		}
	}
}