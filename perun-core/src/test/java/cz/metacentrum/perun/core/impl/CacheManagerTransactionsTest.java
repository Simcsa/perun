package cz.metacentrum.perun.core.impl;

import cz.metacentrum.perun.core.AbstractPerunIntegrationTest;
import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.Holder;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Cache manager tests for cache transactions.
 *
 * @author Simona Kruppova
 */
public class CacheManagerTransactionsTest extends AbstractPerunIntegrationTest {

	private final static String CLASS_NAME = "CacheManagerTransactions.";

	private CacheManager cacheManager;

	private static int id = 0;

	@Before
	public void setUp() throws Exception {
		cacheManager = perun.getCacheManager();
		CacheManager.setCacheDisabled(false);
		CacheManager.setCacheTest(true);
		//CacheManagerTransactionsTest counts with empty cache
		cacheManager.clearTestCache();
	}

	@After
	public void tearDown() throws Exception {
		CacheManager.setCacheTest(false);
	}

	@Test
	public void wasCacheUpdatedInTransaction() throws Exception {
		System.out.println(CLASS_NAME + "wasCacheUpdatedInTransaction");

		cacheManager.newTopLevelTransaction();

		Attribute attr = setUpGroupAttribute();
		Holder holder = new Holder(0, Holder.HolderType.GROUP);
		cacheManager.setAttribute(attr, holder);

		assertTrue("cache should have bee updated in transaction", cacheManager.wasCacheUpdatedInTransaction());

		cacheManager.rollback();
		cacheManager.clean();
	}

	@Test
	public void getAttributeByNameAndPrimaryHolderInTransaction() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndPrimaryHolder");

		cacheManager.newTopLevelTransaction();

		Attribute attr = setUpGroupAttribute();
		Holder holder = new Holder(0, Holder.HolderType.GROUP);
		cacheManager.setAttribute(attr, holder);

		Attribute returnedAttr = cacheManager.getAttributeByName(attr.getName(), holder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);

		cacheManager.rollback();
		cacheManager.clean();
	}

	@Test
	public void getAttributeByNameAndPrimaryHolderInNestedTransaction() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndPrimaryHolder");

		cacheManager.newTopLevelTransaction();
		cacheManager.newNestedTransaction();

		Attribute attr = setUpGroupAttribute();
		Holder holder = new Holder(0, Holder.HolderType.GROUP);
		cacheManager.setAttribute(attr, holder);

		Attribute returnedAttr = cacheManager.getAttributeByName(attr.getName(), holder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);

		cacheManager.cleanNestedTransaction();
		cacheManager.rollback();
		cacheManager.clean();
	}








	// PRIVATE METHODS ----------------------------------------------


	private List<AttributeDefinition> setUpAttributesDefinitions() throws InternalErrorException {

		List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
		attributeDefinitions.add(setUpGroupAttributeDefinition());
		attributeDefinitions.add(setUpResourceAttributeDefinition());

		return attributeDefinitions;
	}

	private AttributeDefinition setUpGroupAttributeDefinition() throws InternalErrorException {
		return setUpAttributeDefinition(AttributesManager.NS_GROUP_ATTR_OPT, "group-test-attribute-definition");
	}

	private AttributeDefinition setUpResourceAttributeDefinition() throws InternalErrorException {
		return setUpAttributeDefinition(AttributesManager.NS_RESOURCE_ATTR_OPT, "resource-test-attribute-definition");
	}

	private AttributeDefinition setUpGroupResourceAttributeDefinition() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_GROUP_RESOURCE_ATTR_OPT, "group-resource-test-attribute-definition");
	}

	private AttributeDefinition setUpMemberGroupAttributeDefinition() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_MEMBER_GROUP_ATTR_OPT, "member-group-test-attribute-definition");
	}

	private AttributeDefinition setUpEntitylessAttributeDefinition() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_ENTITYLESS_ATTR_DEF, "entityless-test-attribute-definition");
	}

	private AttributeDefinition setUpVirtualMemberAttribute() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_MEMBER_ATTR_VIRT, "member-test-virtual-attribute");
	}

	private AttributeDefinition setUpVirtualGroupAttribute() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_GROUP_ATTR_VIRT, "group-test-virtual-attribute");
	}

	private AttributeDefinition setUpVirtualMemberResourceAttribute() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_MEMBER_RESOURCE_ATTR_VIRT,"member-resource-test-virtual-attribute");
	}

	private AttributeDefinition setUpVirtualGroupResourceAttribute() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_GROUP_RESOURCE_ATTR_VIRT, "group-resource-test-virtual-attribute");
	}

	private AttributeDefinition setUpVirtualUserFacilityAttribute() throws Exception {
		return setUpAttributeDefinition(AttributesManager.NS_USER_FACILITY_ATTR_VIRT, "user-facility-test-virtual-attribute");
	}

	private Attribute setUpGroupAttribute() throws InternalErrorException {
		return setUpAttribute(AttributesManager.NS_GROUP_ATTR_OPT, "group-test-attribute", "GroupAttribute");
	}

	private Attribute setUpUserAttribute() throws InternalErrorException {
		return setUpAttribute(AttributesManager.NS_USER_ATTR_OPT, "user-test-attribute", "UserAttribute");
	}

	private Attribute setUpGroupResourceAttribute() throws Exception {
		return setUpAttribute(AttributesManager.NS_GROUP_RESOURCE_ATTR_OPT, "group-resource-test-attribute", "GroupResourceAttribute");
	}

	private Attribute setUpMemberGroupAttribute() throws Exception {
		return setUpAttribute(AttributesManager.NS_MEMBER_GROUP_ATTR_OPT, "member-group-test-attribute", "MemberGroupAttribute");
	}

	private Attribute setUpUserFacilityAttribute() throws Exception {
		return setUpAttribute(AttributesManager.NS_USER_FACILITY_ATTR_OPT, "user-facility-test-attribute", "UserFacilityAttribute");
	}

	private Attribute setUpEntitylessAttribute() throws Exception {
		return setUpAttribute(AttributesManager.NS_ENTITYLESS_ATTR_DEF, "entityless-test-attribute", "EntitylessAttribute");
	}

	private AttributeDefinition setUpAttributeDefinition(String namespace, String friendlyName) throws InternalErrorException {

		AttributeDefinition attr = new Attribute();
		attr.setNamespace(namespace);
		attr.setFriendlyName(friendlyName);
		attr.setType(String.class.getName());
		attr.setId(id);
		id++;

		cacheManager.setAttributeDefinition(attr);

		return attr;
	}

	private Attribute setUpAttribute(String namespace, String friendlyName, String value) throws InternalErrorException {

		Attribute attr = new Attribute();
		attr.setNamespace(namespace);
		attr.setFriendlyName(friendlyName);
		attr.setType(String.class.getName());
		attr.setId(id);
		id++;

		String time = "2016-04-24";
		String creator = "Admin";

		attr.setValue(value);
		attr.setValueCreatedAt(time);
		attr.setValueCreatedBy(creator);
		attr.setValueModifiedAt(time);
		attr.setValueModifiedBy(creator);

		cacheManager.setAttributeDefinition(attr);

		return attr;
	}
}
