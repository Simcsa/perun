package cz.metacentrum.perun.core.impl;

import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;
import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.ExtSourcesManager;
import cz.metacentrum.perun.core.api.Group;
import cz.metacentrum.perun.core.api.Member;
import cz.metacentrum.perun.core.api.Perun;
import cz.metacentrum.perun.core.api.PerunPrincipal;
import cz.metacentrum.perun.core.api.PerunSession;
import cz.metacentrum.perun.core.api.Service;
import cz.metacentrum.perun.core.api.ServiceAttributes;
import cz.metacentrum.perun.core.api.Status;
import cz.metacentrum.perun.core.api.User;
import cz.metacentrum.perun.core.api.Vo;
import cz.metacentrum.perun.core.bl.PerunBl;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.Resource;
import cz.metacentrum.perun.core.api.UserExtSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class with main method to get some special action or write version.
 *
 * @author Michal Stava <stavamichal@gmail.com>
 */
public class Main {

	private PerunBl perun;
	private AbstractApplicationContext springCtx;
	private PerunSession perunSession;
	private final static Logger log = LoggerFactory.getLogger(Main.class);
	private final Integer maxRead = 10000;
	private final Integer maxWrite = 2000;
	private final PerunPrincipal pp = new PerunPrincipal("main", ExtSourcesManager.EXTSOURCE_NAME_INTERNAL, ExtSourcesManager.EXTSOURCE_INTERNAL);

	private static BufferedWriter writer;

	public Main(String fileName) throws Exception{
		try {
			this.springCtx = new ClassPathXmlApplicationContext("perun-beans.xml", "perun-datasources.xml", "perun-transaction-manager.xml");
			this.perun = springCtx.getBean("perun", PerunBl.class);
			this.perunSession = perun.getPerunSession(pp);
		} catch (Exception e) {
			log.error("Application context loading error.", e);
			throw e;
		}
		this.writer = new BufferedWriter(openForFile(fileName));

		int LastMessageBeforeInitializingData = perun.getAuditer().getLastMessageId();
		System.err.println("Last message id before starting initializing: " + LastMessageBeforeInitializingData + '\n');
		vosLdifToWriter();
		groupsLdifToWriter();
		resourcesLdifToWriter();
		usersLdifToWriter();
		int LastMessageAfterInitializingData = perun.getAuditer().getLastMessageId();
		System.err.println("Last message id after initializing: " + LastMessageAfterInitializingData + '\n');
		perun.getAuditer().setLastProcessedId("ldapcConsumer", LastMessageAfterInitializingData);
	}

	//Main for testing attributes cache
	public Main() throws Exception {
		try {
			this.springCtx = new ClassPathXmlApplicationContext("perun-beans.xml", "perun-datasources.xml", "perun-transaction-manager.xml");
			this.perun = springCtx.getBean("perun", PerunBl.class);
			this.perunSession = perun.getPerunSession(pp);
		} catch (Exception e) {
			log.error("Application context loading error.", e);
			throw e;
		}

		testCacheForMeasuring();
		//testCache();
		System.out.println("Cache was tested successfully.");
	}

	public static void main(String[] args) throws Exception {
		if(args.length == 0 || args.length > 2) {
			System.out.println(badUsage(null));
			System.out.println(help());
		} else if(args[0].equals("-h") || args[0].equals("--help")) {
			System.out.println(help());
		} else if(args[0].equals("-v")) {
			System.out.println(version());
		} else if(args[0].equals("-g")) {
			Main main;
			if(args[1] != null && args[1].length() != 0) {
				main = new Main(args[1]);
			} else {
				main = new Main(null);
			}
			writer.close();
		} else if(args[0].equals("-test")) {
			Main main = new Main();
		} else {
			System.out.println(badUsage(args[0]));
			System.out.println(help());
		}
	}

	private void testCacheForMeasuring() throws Exception {
		long startTime;
		long endTime;
		BufferedWriter bw = null;
		int n = 1000;

		try {
			File file = new File("measuring.csv");
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			System.out.println(bw);
			User user = perun.getUsersManagerBl().getUserById(perunSession, 6701); // Michal Stava
			AttributeDefinition attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 1364); //user:def:address
			Attribute attr;

			//Test of getting non-virtual attribute before cache is initialized
			bw.write("getAttribute (non-virtual) - cache uninitialized");
			System.out.print("getAttribute (non-virtual) - cache uninitialized");
			for (int i = 0; i < n; i++) {
				//startTime = System.currentTimeMillis();
				startTime = System.nanoTime();
				attr = perun.getAttributesManagerBl().getAttribute(perunSession, user, attrDef.getName());
				//endTime = System.currentTimeMillis();
				endTime = System.nanoTime();
				bw.write("," + (endTime - startTime));
				System.out.print("," + (endTime - startTime));
				perun.getAttributesManagerBl().removeAttributeFromCache(user, attr); //method added to attributesManager for testing purposes
			}

			bw.newLine();
			System.out.println();

			//Test of getting non-virtual attribute after cache is initialized
			bw.write("getAttribute (non-virtual) - cache initialized");
			System.out.print("getAttribute (non-virtual) - cache initialized");
			for (int i = 0; i < n; i++) {
				//startTime = System.currentTimeMillis();
				startTime = System.nanoTime();
				attr = perun.getAttributesManagerBl().getAttribute(perunSession, user, attrDef.getName());
				endTime = System.nanoTime();
				//endTime = System.currentTimeMillis();
				bw.write("," + (endTime - startTime));
				System.out.print("," + (endTime - startTime));
			}

			bw.newLine();
			System.out.println();
//
//			Facility facility = perun.getFacilitiesManagerBl().getFacilityById(perunSession, 2662); //acct.metacentrum.cz
//			attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 260); //user-fac:virt:login
//
//			// x * using of getAttribute from DB (every call is one sql query) (where x is something big like 1000 or more) - this should be much quicker with cache
//			bw.write("getAttribute - every call is one query (" + maxRead + " queries)");
//			System.out.print("getAttribute - every call is one query (" + maxRead + " queries)");
//			for (int i = 0; i < n; i++) {
//				startTime = System.currentTimeMillis();
//				for(int j = 0; j < maxRead; j++) {
//					attr = perun.getAttributesManagerBl().getAttribute(perunSession, facility, user, attrDef.getName());
//				}
//				endTime = System.currentTimeMillis();
//				bw.write("," + (endTime - startTime));
//				System.out.print("," + (endTime - startTime));
//			}
//
//			bw.newLine();
//			System.out.println();
//
//			facility = perun.getFacilitiesManagerBl().getFacilityById(perunSession, 2662); //acct.metacentrum.cz
//			attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 260); //user-fac:virt:login
//			List<Attribute> attrs = null;
//
//			// x * attribute getting by one query from DB (the same like the one before but now only in 1 query) - this should be almost same quick with cache like with no cache
//			//cache uninitialized
//			bw.write("getAttributes - in one big query (cache uninitialized)");
//			System.out.print("getAttributes - in one big query (cache uninitialized)");
//			for (int i = 0; i < n; i++) {
//				startTime = System.currentTimeMillis();
//				attrs = perun.getAttributesManagerBl().getAttributes(perunSession, facility, user); //special version getAttributes is used (modified!)
//				endTime = System.currentTimeMillis();
//				bw.write("," + (endTime - startTime));
//				System.out.print("," + (endTime - startTime));
//				perun.getAttributesManagerBl().removeAllUserFacilityAttributesFromCache(user);
//			}
//
//			bw.newLine();
//			System.out.println();
//
//			attrs = perun.getAttributesManagerBl().getAttributes(perunSession, facility, user);
//
//			// x * attribute getting by one query from DB (the same like the one before but now only in 1 query) - this should be almost same quick with cache like with no cache
//			//cache initialized
//			bw.write("getAttributes - in one big query (cache initialized)");
//			System.out.print("getAttributes - in one big query (cache initialized)");
//			for (int i = 0; i < n; i++) {
//				startTime = System.currentTimeMillis();
//				attrs = perun.getAttributesManagerBl().getAttributes(perunSession, facility, user); //special version getAttributes is used (modified!)
//				endTime = System.currentTimeMillis();
//				bw.write("," + (endTime - startTime));
//				System.out.print("," + (endTime - startTime));
//			}
//
//			bw.newLine();
//			System.out.println();
//
//			String textA = "Na Kopečku 324 Dobšice 67182";
//			String textB = "Na Kopečku 324 Dobšice 67182 Jihomoravský kraj";
//			attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 1364);
//			attr = perun.getAttributesManagerBl().getAttribute(perunSession, user, attrDef.getName());
//
//			// x * write attributes to DB - because we need to know if cache slows these queries
//			bw.write("write using setAttribute (" + maxWrite + " attributes)");
//			System.out.print("write using setAttribute (" + maxWrite + " attributes)");
//			for (int i = 0; i < n; i++) {
//				startTime = System.currentTimeMillis();
//				for(int j = 0; j < maxWrite; j++) {
//					if((i % 2) == 0) attr.setValue(textA);
//					else attr.setValue(textB);
//					perun.getAttributesManagerBl().setAttribute(perunSession, user, attr);
//				}
//				endTime = System.currentTimeMillis();
//				bw.write("," + (endTime - startTime));
//				System.out.print("," + (endTime - startTime));
//			}
//
//			bw.newLine();
//			System.out.println();
//
//			// x * read/write attribute from/to DB (we need to invalidate DB cache)
//			bw.write("read/write using getAttribute/setAttribute (" + maxWrite + " attributes)");
//			System.out.print("read/write using getAttribute/setAttribute (" + maxWrite + " attributes)");
//			for (int i = 0; i < n; i++) {
//				startTime = System.currentTimeMillis();
//				for(int j = 0; j < maxWrite; j++) {
//					Attribute attrIn = perun.getAttributesManagerBl().getAttribute(perunSession, user, attr.getName());
//					if((i % 2) == 0) attrIn.setValue(textA);
//					else attrIn.setValue(textB);
//					perun.getAttributesManagerBl().setAttribute(perunSession, user, attrIn);
//				}
//				endTime = System.currentTimeMillis();
//				bw.write("," + (endTime - startTime));
//				System.out.print("," + (endTime - startTime));
//			}
//
//			bw.newLine();
//			System.out.println();
		}
		finally {
			bw.close();
		}
	}

	private void testCache() throws Exception {

		//Test of getting non-virtual attribute before cache is initialized
		User user = perun.getUsersManagerBl().getUserById(perunSession, 6701); // Michal Stava
		long startTime;
		long endTime;
		AttributeDefinition attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 1364); //user:def:address

		startTime = System.currentTimeMillis();
		Attribute attr = perun.getAttributesManagerBl().getAttribute(perunSession, user, attrDef.getName());
		endTime = System.currentTimeMillis();
		System.out.println("Non-virtual attribute user-def-address was get in: " + (endTime-startTime) + " ms before initializing");
		System.out.println(attr);

		//Test of getting non-virtual attribute after cache is initialized
		startTime = System.currentTimeMillis();
		attr = perun.getAttributesManagerBl().getAttribute(perunSession, user, attrDef.getName());
		endTime = System.currentTimeMillis();
		System.out.println("Non-virtual attribute user-def-address was get in: " + (endTime-startTime) + " ms after initializing");
		System.out.println(attr);

		System.out.println("\n---------------------------------------------\n");

		// x * using of getAttribute from DB (every call is one sql query) (where x is something big like 1000 or more) - this should be much quicker with cache
		Facility facility = perun.getFacilitiesManagerBl().getFacilityById(perunSession, 2662); //acct.metacentrum.cz
		attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 260); //user-fac:virt:login
		startTime = System.currentTimeMillis();
		for(int i = 0; i < maxRead; i++) {
			attr = perun.getAttributesManagerBl().getAttribute(perunSession, facility, user, attrDef.getName());
		}
		endTime = System.currentTimeMillis();
		System.out.println( maxRead + " user-facility attributes in " + maxRead + " queries were get in: " + (endTime-startTime) + " ms after initializing");
		System.out.println("\n---------------------------------------------\n");

		// x * attribute getting by one query from DB (the same like the one before but now only in 1 query) - this should be almost same quick with cache like with no cache
		facility = perun.getFacilitiesManagerBl().getFacilityById(perunSession, 2662); //acct.metacentrum.cz
		attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 260); //user-fac:virt:login
		startTime = System.currentTimeMillis();
		//special version getAttributes is used (modified!)
		List<Attribute> attrs = perun.getAttributesManagerBl().getAttributes(perunSession, facility, user);
		endTime = System.currentTimeMillis();
		System.out.println("All user-facility attributes in one query were get in: " + (endTime-startTime) + " ms before initializing");
		System.out.println("\n---------------------------------------------\n");
		//results - the first call lasted longer (8000 ms vs 6500 ms) probably because of DB cache

		attr = null;
		for(int j = 0; j < 10; j++) {
			attrs = new ArrayList<>();
			startTime = System.currentTimeMillis();
			//special version getAttributes is used (modified!)
			attrs = perun.getAttributesManagerBl().getAttributes(perunSession, facility, user);
			endTime = System.currentTimeMillis();
			System.out.println("All user-facility attributes in one query were get in: " + (endTime-startTime) + " ms after initializing");
		}
		System.out.println("\n---------------------------------------------\n");

		String textA = "Na Kopečku 324 Dobšice 67182";
		String textB = "Na Kopečku 324 Dobšice 67182 Jihomoravský kraj";
		attrDef = perun.getAttributesManagerBl().getAttributeDefinitionById(perunSession, 1364);
		attr = perun.getAttributesManagerBl().getAttribute(perunSession, user, attrDef.getName());

		// x * write attributes to DB - because we need to know if cache slows these queries
		startTime = System.currentTimeMillis();
		for(Integer i = 0; i < maxWrite; i++) {
			if((i % 2) == 0) attr.setValue(textA);
			else attr.setValue(textB);
			if((i % 1000) == 0) System.out.println(i);
			perun.getAttributesManagerBl().setAttribute(perunSession, user, attr);
		}
		endTime = System.currentTimeMillis();
		System.out.println("Writing " + maxWrite + " attributes to db was done in: " + (endTime-startTime) + " ms");
		System.out.println("\n---------------------------------------------\n");

		// x * read/write attribute from/to DB (we need to invalidate DB cache)
		startTime = System.currentTimeMillis();
		for(Integer i = 0; i < maxWrite; i++) {
			Attribute attrIn = perun.getAttributesManagerBl().getAttribute(perunSession, user, attr.getName());
			if((i % 2) == 0) attrIn.setValue(textA);
			else attrIn.setValue(textB);
			perun.getAttributesManagerBl().setAttribute(perunSession, user, attrIn);
		}
		endTime = System.currentTimeMillis();
		System.out.println("Reading/Writing " + maxWrite + " (for each) attributes to db was done in: " + (endTime-startTime) + " ms");
		System.out.println("\n---------------------------------------------\n");
	}

	private static String badUsage(String badArgument) {
		StringBuilder sb = new StringBuilder();
		sb.append("Bad usage. Wrong argument or less than 1 or more than 2 arguments (not supported).");
		sb.append('\n');
		if(badArgument != null) {
			sb.append("Bad argument is '" + badArgument + "'.");
			sb.append('n');
		}
		return sb.toString();
	}

	private static String help() {
		StringBuilder sb = new StringBuilder();
		sb.append("--------------HELP-------------");
		sb.append('\n');
		sb.append("-h | --help =>  help");
		sb.append('\n');
		sb.append("-g          =>  generate ldifs (second argument can be path to the file for generating ldif)");
		sb.append('\n');
		sb.append("-v          =>  version");
		sb.append('\n');
		return sb.toString();
	}

	private static String version() throws IOException {
		Package p = Main.class.getPackage();
		StringBuilder sb = new StringBuilder();
		sb.append("Title: "+ p.getImplementationTitle());
		sb.append('\n');
		sb.append("Version: "+ p.getImplementationVersion());
		sb.append('\n');
		sb.append("Vendor: "+ p.getImplementationVendor());
		sb.append('\n');

		Properties pom = new Properties();
		pom.load(Main.class.getResourceAsStream("/META-INF/maven/cz.metacentrum.perun/perun-core/pom.properties"));

		sb.append("pom = " + pom);
		sb.append('\n');
		return sb.toString();
	}

	//Prepare for using file insted of stdout
	private Writer openForFile(String fileName) {
		try {
			if (fileName != null) return new PrintWriter(fileName);
			else return new OutputStreamWriter(System.out);
		} catch (FileNotFoundException ex) {
			System.out.println("# File you specify not exist and can't be created! Script will be canceled now!");
			throw new RuntimeException("File with ldif can't be created.", ex);
		}
	}

	private void vosLdifToWriter() throws Exception {
		List<Vo> vos = perun.getVosManagerBl().getVos(perunSession);

		for(Vo v: vos) {
			String dn = "dn: ";
			String desc = "description: ";
			String oc1 = "objectclass: top";
			String oc2 = "objectclass: organization";
			String oc3 = "objectclass: perunVO";
			String o = "o: ";
			String perunVoId = "perunVoId: ";
			perunVoId+= String.valueOf(v.getId());
			o+= v.getShortName();
			desc+= v.getName();
			dn+= "perunVoId=" + v.getId() + ",dc=perun,dc=cesnet,dc=cz";
			writer.write(dn + '\n');
			writer.write(oc1 + '\n');
			writer.write(oc2 + '\n');
			writer.write(oc3 + '\n');
			writer.write(o + '\n');
			writer.write(perunVoId + '\n');
			writer.write(desc + '\n');
			//Generate all members in member groups of this vo and add them here (only members with status Valid)
			List<Member> validMembers = perun.getMembersManagerBl().getMembers(perunSession, v, Status.VALID);
			for(Member m: validMembers) {
				writer.write("uniqueMember: perunUserId=" + m.getUserId() + ",ou=People,dc=perun,dc=cesnet,dc=cz" + '\n');
			}
			writer.write('\n');
		}
	}

	private void resourcesLdifToWriter() throws Exception {
		List<Vo> vos = perun.getVosManagerBl().getVos(perunSession);

		for(Vo v: vos) {
			List<Resource> resources = new ArrayList<Resource>();
			resources = perun.getResourcesManagerBl().getResources(perunSession, v);
			for(Resource r: resources) {
				String dn = "dn: ";
				String oc1 = "objectclass: top";
				String oc2 = "objectclass: groupOfUniqueNames";
				String oc3 = "objectclass: perunResource";
				String cn = "cn: ";
				String perunVoId = "perunVoId: ";
				String perunFacilityId = "perunFacilityId: ";
				String perunResourceId = "perunResourceId: ";
				String description = "description: ";

				perunVoId+= String.valueOf(r.getVoId());
				perunFacilityId+= String.valueOf(r.getFacilityId());
				perunResourceId+= String.valueOf(r.getId());
				dn+= "perunResourceId=" + r.getId() + ",perunVoId=" + r.getVoId() + ",dc=perun,dc=cesnet,dc=cz";
				cn+= r.getName();
				String descriptionValue = r.getDescription();
				if(descriptionValue != null) {
					if(descriptionValue.matches("^[ ]*$")) descriptionValue = null;
				}
				writer.write(dn + '\n');
				writer.write(oc1 + '\n');
				writer.write(oc2 + '\n');
				writer.write(oc3 + '\n');
				writer.write(cn + '\n');
				writer.write(perunResourceId + '\n');
				if(descriptionValue != null) writer.write(description + descriptionValue + '\n');
				writer.write(perunVoId + '\n');
				writer.write(perunFacilityId + '\n');
				//ADD resources which group is assigned to
				List<Group> associatedGroups = perun.getResourcesManagerBl().getAssignedGroups(perunSession, r);
				for(Group g: associatedGroups) {
					writer.write("assignedGroupId: " + g.getId());
					writer.write('\n');
				}
				writer.write('\n');
			}
		}
	}

	private void groupsLdifToWriter() throws Exception {
		List<Vo> vos = perun.getVosManagerBl().getVos(perunSession);

		for(Vo v: vos) {
			List<Group> groups = new ArrayList<Group>();
			groups = perun.getGroupsManagerBl().getGroups(perunSession, v);
			for(Group g: groups) {
				String dn = "dn: ";
				String oc1 = "objectclass: top";
				String oc2 = "objectclass: groupOfUniqueNames";
				String oc3 = "objectclass: perunGroup";
				String cn = "cn: ";
				String perunVoId = "perunVoId: ";
				String parentGroup = "perunParentGroup: ";
				String parentGroupId = "perunParentGroupId: ";
				String perunGroupId = "perunGroupId: ";
				String owner = "owner: ";
				String description = "description: ";
				String perunUniqueGroupName = "perunUniqueGroupName: ";
				List<Member> members = new ArrayList<Member>();
				members = perun.getGroupsManagerBl().getGroupMembers(perunSession, g, Status.VALID);
				perunGroupId+= String.valueOf(g.getId());
				perunVoId+= String.valueOf(g.getVoId());
				dn+= "perunGroupId=" + g.getId() + ",perunVoId=" + g.getVoId() + ",dc=perun,dc=cesnet,dc=cz";
				cn+= g.getName();
				perunUniqueGroupName+= v.getShortName() + ":" + g.getName();
				if(g.getDescription() != null) description+= g.getDescription();
				if(g.getParentGroupId() != null) {
					parentGroupId+= g.getParentGroupId();
					parentGroup+= "perunGroupId=" + g.getParentGroupId()+ ",perunVoId=" + g.getVoId() + ",dc=perun,dc=cesnet,dc=cz";
				}
				List<Member> admins = new ArrayList<Member>();
				writer.write(dn + '\n');
				writer.write(oc1 + '\n');
				writer.write(oc2 + '\n');
				writer.write(oc3 + '\n');
				writer.write(cn + '\n');
				writer.write(perunUniqueGroupName + '\n');
				writer.write(perunGroupId + '\n');
				writer.write(perunVoId + '\n');
				if(g.getDescription() != null) writer.write(description + '\n');
				if(g.getParentGroupId() != null) {
					writer.write(parentGroupId + '\n');
					writer.write(parentGroup + '\n');
				}
				//ADD Group Members
				for(Member m: members) {
					writer.write("uniqueMember: " + "perunUserId=" + m.getUserId() + ",ou=People,dc=perun,dc=cesnet,dc=cz");
					writer.write('\n');
				}
				//ADD resources which group is assigned to
				List<Resource> associatedResources = perun.getResourcesManagerBl().getAssignedResources(perunSession, g);
				for(Resource r: associatedResources) {
					writer.write("assignedToResourceId: " + r.getId());
					writer.write('\n');
				}
				//FOR NOW No groups has owner
				writer.write(owner + '\n');
				writer.write('\n');
			}
		}
	}

	private void usersLdifToWriter() throws Exception {
		List<User> users = perun.getUsersManagerBl().getUsers(perunSession);

		for(User u: users) {
			String dn = "dn: ";
			String entryStatus = "entryStatus: active";
			String oc1 = "objectclass: top";
			String oc2 = "objectclass: person";
			String oc3 = "objectclass: organizationalPerson";
			String oc4 = "objectclass: inetOrgPerson";
			String oc5 = "objectclass: perunUser";
			String oc6 = "objectclass: tenOperEntry";
			String oc7 = "objectclass: inetUser";
			String sn = "sn: ";
			String cn = "cn: ";
			String givenName = "givenName: ";
			String perunUserId = "perunUserId: ";
			String mail = "mail: ";
			String preferredMail = "preferredMail: ";
			String o = "o: ";
			String isServiceUser = "isServiceUser: ";
			String userPassword = "userPassword: ";
			List<String> membersOf = new ArrayList<String>();
			List<Member> members = new ArrayList<Member>();
			Set<String> membersOfPerunVo = new HashSet<>();
			members = perun.getMembersManagerBl().getMembersByUser(perunSession, u);
			for(Member m: members) {
				if(m.getStatus().equals(Status.VALID)) {
					membersOfPerunVo.add("memberOfPerunVo: " + m.getVoId());
					List<Group> groups = new ArrayList<Group>();
					groups = perun.getGroupsManagerBl().getAllMemberGroups(perunSession, m);
					for(Group g: groups) {
						membersOf.add("memberOf: " + "perunGroupId=" + g.getId() + ",perunVoId=" + g.getVoId() + ",dc=perun,dc=cesnet,dc=cz");
					}
				}
			}
			//Attribute attrMail = perun.getAttributesManagerBl().getAttribute(perunSession, u, AttributesManager.NS_USER_ATTR_DEF + ":mail");
			Attribute attrPreferredMail = perun.getAttributesManagerBl().getAttribute(perunSession, u, AttributesManager.NS_USER_ATTR_DEF + ":preferredMail");
			Attribute attrOrganization = perun.getAttributesManagerBl().getAttribute(perunSession, u, AttributesManager.NS_USER_ATTR_DEF + ":organization");
			Attribute attrVirtCertDNs = perun.getAttributesManagerBl().getAttribute(perunSession, u, AttributesManager.NS_USER_ATTR_VIRT + ":userCertDNs");
			perunUserId+= String.valueOf(u.getId());
			dn+= "perunUserId=" + u.getId() + ",ou=People,dc=perun,dc=cesnet,dc=cz";
			String firstName = u.getFirstName();
			String lastName = u.getLastName();
			if(firstName == null) firstName = "";
			if(lastName == null || lastName.isEmpty()) lastName = "N/A";
			sn+= lastName;
			cn+= firstName + " " + lastName;
			if(u.isServiceUser()) isServiceUser+= "1";
			else isServiceUser+= "0";
			if(firstName.isEmpty()) givenName = null;
			else givenName+= firstName;
			if(attrPreferredMail == null || attrPreferredMail.getValue() == null) mail = null;
			else mail+= (String) attrPreferredMail.getValue();
			if(attrPreferredMail == null || attrPreferredMail.getValue() == null) preferredMail =null;
			else preferredMail+= (String) attrPreferredMail.getValue();
			if(attrOrganization == null || attrOrganization.getValue() == null) o= null;
			else o+= (String) attrOrganization.getValue();
			Map<String, String> certDNs = null;
			Set<String> certSubjects = null;
			if(attrVirtCertDNs != null && attrVirtCertDNs.getValue() != null) {
				certDNs = (Map) attrVirtCertDNs.getValue();
				certSubjects = certDNs.keySet();
			}
			writer.write(dn + '\n');
			writer.write(oc1 + '\n');
			writer.write(oc2 + '\n');
			writer.write(oc3 + '\n');
			writer.write(oc4 + '\n');
			writer.write(oc5 + '\n');
			writer.write(oc6 + '\n');
			writer.write(oc7 + '\n');
			writer.write(entryStatus + '\n');
			writer.write(sn + '\n');
			writer.write(cn + '\n');
			if(givenName != null) writer.write(givenName + '\n');
			writer.write(perunUserId + '\n');
			writer.write(isServiceUser + '\n');
			if(mail != null) writer.write(mail + '\n');
			if(preferredMail != null) writer.write(preferredMail + '\n');
			if(o != null) writer.write(o + '\n');
			if(certSubjects != null && !certSubjects.isEmpty()) {
				for(String s: certSubjects) {
					writer.write("userCertificateSubject: " + s + '\n');
				}
			}
			//GET ALL USERS UIDs
			List<String> similarUids = perun.getAttributesManagerBl().getAllSimilarAttributeNames(perunSession, AttributesManager.NS_USER_ATTR_DEF + ":uid-namespace:");
			if(similarUids != null && !similarUids.isEmpty()) {
				for(String s: similarUids) {
					Attribute uidNamespace = perun.getAttributesManagerBl().getAttribute(perunSession, u, s);
					if(uidNamespace != null && uidNamespace.getValue() != null) {
						writer.write("uidNumber;x-ns-" + uidNamespace.getFriendlyNameParameter() + ": " + uidNamespace.getValue().toString() + '\n');
					}
				}
			}
			//GET ALL USERS LOGINs
			List<String> similarLogins = perun.getAttributesManagerBl().getAllSimilarAttributeNames(perunSession, AttributesManager.NS_USER_ATTR_DEF + ":login-namespace:");
			if(similarLogins != null && !similarLogins.isEmpty()) {
				for(String s: similarLogins) {
					Attribute loginNamespace = perun.getAttributesManagerBl().getAttribute(perunSession, u, s);
					if(loginNamespace != null && loginNamespace.getValue() != null) {
						writer.write("login;x-ns-" + loginNamespace.getFriendlyNameParameter() + ": " + loginNamespace.getValue().toString() + '\n');
						if(loginNamespace.getFriendlyNameParameter().equals("einfra")) {
							writer.write(userPassword + "{SASL}" + loginNamespace.getValue().toString()  + '@' + loginNamespace.getFriendlyNameParameter().toUpperCase() + '\n');
						}
					}
				}
			}
			//GET ALL USERS EXTlogins FOR EVERY EXTSOURCE WITH TYPE EQUALS IDP
			List<UserExtSource> userExtSources = perun.getUsersManagerBl().getUserExtSources(perunSession, u);
			List<String> extLogins = new ArrayList<String>();
			for(UserExtSource ues: userExtSources) {
				if(ues != null && ues.getExtSource() != null) {
					String type = ues.getExtSource().getType();
					if(type != null) {
						if(type.equals(ExtSourcesManager.EXTSOURCE_IDP)) {
							String extLogin;
							extLogin = ues.getLogin();
							if(extLogin == null) extLogin = "";
							writer.write("eduPersonPrincipalNames: " + extLogin + '\n');
						}
					}
				}
			}
			//ADD MEMBEROF ATTRIBUTE TO WRITER
			for(String s: membersOf) {
				writer.write(s + '\n');
			}
			//ADD MEMBEROFPERUNVO ATTRIBUTE TO WRITER
			for(String s: membersOfPerunVo) {
				writer.write(s + '\n');
			}
			writer.write('\n');
		}

	}
}