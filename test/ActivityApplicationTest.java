import static org.junit.Assert.*;

import java.util.Calendar;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import models.ActivityApplication;
import models.ActivityPiling;
import models.ActivityExplosives;
import models.NoiseProducer;
import models.Organisation;
import models.Regulator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.test.FakeApplication;
import play.test.Helpers;
import scala.Option;


/**
 * Unit tests for the ActivityApplication
 * 
 * @note persistence is tested in the ModelTest class so is not re-tested here.
 * 
 * @author david.simpson
 *
 */
public class ActivityApplicationTest {
	
	@PersistenceContext
	private static EntityManager em;
	
	@BeforeClass
	public static void setUp() {
		final FakeApplication app = Helpers.fakeApplication();
		Helpers.start(app);
		Option<JPAPlugin> jpaPlugin = app.getWrappedApplication().plugin(JPAPlugin.class);
		em = jpaPlugin.get().em("default");
		JPA.bindForCurrentThread(em);
		em.getTransaction().begin();
	}
	
	@AfterClass
	public static void tearDown()
	{
		em.getTransaction().rollback();
		JPA.bindForCurrentThread(null);
		if (em!=null)
			em.close();
	}	
	
	@Test
	public void test_setProposedActivityApplicationOrganisation() {
		try {
			ActivityApplication aa = new ActivityApplication();
			NoiseProducer np = new NoiseProducer();
			Organisation org = new Organisation();
			org.setOrganisation_name("TestOrg");
			org.setId(0L);

			np.setOrganisation(org);
			
			aa.setNoiseproducer(np);
			
			assertEquals(np, aa.getNoiseproducer());
			
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}	
	@Test
	public void test_setProposedActivityApplicationRegulator() {
		try {
			ActivityApplication aa = new ActivityApplication();
			Regulator reg = new Regulator();
			Organisation org = new Organisation();
			org.setOrganisation_name("TestReg");
			org.setId(0L);
			reg.setOrganisation(org);
			aa.setRegulator(reg);
			
			assertEquals(reg, aa.getRegulator());
			
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	/*private NoiseProducer createNP()
	{
		NoiseProducer np = new NoiseProducer();
		Organisation org = new Organisation();
		org.setOrganisation_name("test org");
		org.setContact_name("name");
		org.setContact_phone("1234");
		org.setContact_email("name@org.com");
		np.setOrganisation(org);
		np.save();
		return np;
	}*/
	/*private Regulator createReg()
	{
		Regulator reg = new Regulator();
		Organisation orgreg = new Organisation();
		orgreg.setOrganisation_name("test reg");
		orgreg.setContact_name("name");
		orgreg.setContact_phone("1234");
		orgreg.setContact_email("name@org.com");
		reg.setOrganisation(orgreg);			
		reg.save();
		return reg;		
	}*/
	/*private ActivityApplication createApplicationActivity()
	{
		ActivityApplication aa = new ActivityApplication();
		Calendar cal = Calendar.getInstance();

		//set date values as they would be entered...
		aa.setDate_start_year(2014);
		aa.setDate_start_month(12);
		aa.setDate_start_day(1);
		
		//Remember that calendar takes months in the range 0-11
		cal.set(2014, 11, 02);
		aa.setDate_end(cal.getTime());
		
		cal.set(2014, 11, 05);
		aa.setDate_due(cal.getTime());
		
		ActivityPiling pil = new ActivityPiling();
		pil.setMax_hammer_energy(100);
		aa.setActivityPiling(pil);
		
		//aa.setActivity_type_id(3L);				
		
		aa.setDuration(5);
							
		aa.setDuration(2);

		return aa;
	}*/
	@Test
	public void test_setListApplication() 
	{
		try 
		{
			int iCount = 5;
			
			NoiseProducer np = new NoiseProducer();
			Organisation org = new Organisation();
			org.setOrganisation_name("test org");
			org.setContact_name("name");
			org.setContact_phone("1234");
			org.setContact_email("name@org.com");
			np.setOrganisation(org);
			np.saveNoAdmin();
	
			Regulator reg = new Regulator();
			Organisation orgreg = new Organisation();
			orgreg.setOrganisation_name("test reg");
			orgreg.setContact_name("name");
			orgreg.setContact_phone("1234");
			orgreg.setContact_email("name@org.com");
			reg.setOrganisation(orgreg);			
			reg.save();			
			
			ActivityApplication aa;
			for (int i = 0; i<iCount ; i++)
			{
				aa = getAA();
				aa.setNoiseproducer(np);			
				aa.setRegulator(reg);		
	
				aa.save();
			}
			//liaa=ActivityApplication.findAll();
		}
		catch(Exception e)
		{
			fail(e.toString());
		}
		//assertEquals(iSize+iCount,liaa.size());
	}
	public static ActivityApplication getAA()
	{
		ActivityApplication aa = new ActivityApplication();
		Calendar cal = Calendar.getInstance();

		//set date values as they would be entered...
		aa.setDate_start_year(2014);
		aa.setDate_start_month(12);
		aa.setDate_start_day(1);
		
		//Remember that calendar takes months in the range 0-11
		cal.set(2014, 11, 02);
		aa.setDate_end(cal.getTime());
		
		cal.set(2014, 11, 05);
		aa.setDate_due(cal.getTime());
		
		aa.setDuration(5);
		
		ActivityPiling pil = new ActivityPiling();
		pil.setMax_hammer_energy(100);
		aa.setActivityPiling(pil);
				
		aa.setActivitytype_id(3L);
		
		aa.setNon_licensable(false);
		
		//aa.setActivity_type_id(3L);
		aa.setDuration(2);
		
		return aa;
	}
	@Test
	public void test_setCancelApplication() {
		try {
			NoiseProducer np = new NoiseProducer();
			Organisation org = new Organisation();
			org.setOrganisation_name("test org");
			org.setContact_name("name");
			org.setContact_phone("1234");
			org.setContact_email("name@org.com");
			np.setOrganisation(org);
			np.saveNoAdmin();

			Regulator reg = new Regulator();
			Organisation orgreg = new Organisation();
			orgreg.setOrganisation_name("test reg");
			orgreg.setContact_name("name");
			orgreg.setContact_phone("1234");
			orgreg.setContact_email("name@org.com");
			reg.setOrganisation(orgreg);			
			reg.save();			
			
			ActivityApplication aa = getAA();
			aa.setNoiseproducer(np);			
			aa.setRegulator(reg);	

			aa.save();
			em.flush();
			aa.cancel();			
			
			assertEquals(aa.getStatus(), "Cancelled");
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void test_saveDraftApplication() {
		try {
			NoiseProducer np = new NoiseProducer();
			Organisation org = new Organisation();
			org.setOrganisation_name("test org");
			org.setContact_name("name");
			org.setContact_phone("1234");
			org.setContact_email("name@org.com");
			np.setOrganisation(org);
			np.saveNoAdmin();

			Regulator reg = new Regulator();
			Organisation orgreg = new Organisation();
			orgreg.setOrganisation_name("test reg");
			orgreg.setContact_name("name");
			orgreg.setContact_phone("1234");
			orgreg.setContact_email("name@org.com");
			reg.setOrganisation(orgreg);			
			reg.save();			
			
			ActivityApplication aa = getAA();
			aa.setNoiseproducer(np);			
			aa.setRegulator(reg);	
			aa.setSaveasdraft("true");
			aa.save();
			em.flush();
						
			assertEquals(aa.getStatus(), "Draft");
			
			aa.setNon_licensable(true);
			aa.setSaveasdraft("true");
			aa.update(aa.getId());
			
			assertEquals(aa.getStatus(), "Draft");
			
			aa.setSaveasdraft(null);
			aa.update(aa.getId());
			assertEquals(aa.getStatus(), "Proposed");
			
			//Finally, make sure that attempts to save a non-draft item fail with an exception
			try {
				aa.update(aa.getId());
				fail("Updated a Proposed item.");
			} catch (Exception e) {
				//This is expected!
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void test_linkedApplications() {
		try {
			NoiseProducer np = new NoiseProducer();
			Organisation org = new Organisation();
			org.setOrganisation_name("test org");
			org.setContact_name("name");
			org.setContact_phone("1234");
			org.setContact_email("name@org.com");
			np.setOrganisation(org);
			np.saveNoAdmin();

			Regulator reg = new Regulator();
			Organisation orgreg = new Organisation();
			orgreg.setOrganisation_name("test reg");
			orgreg.setContact_name("name");
			orgreg.setContact_phone("1234");
			orgreg.setContact_email("name@org.com");
			reg.setOrganisation(orgreg);			
			reg.save();			
			
			ActivityApplication aa = getAA();
			aa.setNoiseproducer(np);			
			aa.setRegulator(reg);	
			aa.save();
			em.flush();
			
			assertTrue(aa.getStatus()=="Proposed");
			
			ActivityApplication aa2 = aa.createLinked();
			assertNotNull(aa2);
			assertEquals(aa2.getParent(), aa);
			assertEquals(aa2.getNoiseproducer(), aa.getNoiseproducer());
			assertEquals(aa2.getRegulator(), aa.getRegulator());
			assertEquals(aa2.getDate_start(), aa.getDate_start());
			assertEquals(aa2.getDate_end(), aa.getDate_end());
			assertEquals(aa2.getDuration(), aa.getDuration());
			
			ActivityExplosives exp = new ActivityExplosives();
			exp.setTnt_equivalent(12.5);
			aa2.setActivityExplosives(exp);
					
			aa2.setActivitytype_id(4L);
			
			aa2.save();
			em.flush();
			assertEquals(aa2.getDate_due(), aa.getDate_due());
			assertTrue(aa2.getStatus()=="Proposed");
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
