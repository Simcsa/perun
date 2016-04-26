package cz.metacentrum.perun.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import cz.metacentrum.perun.core.AbstractPerunIntegrationTest;
import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.Holder;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache manager tests.
 *
 * @author Simona Kruppova
 */
public class CacheManagerTest extends AbstractPerunIntegrationTest {

	private final static String CLASS_NAME = "CacheManager.";

	private CacheManager cacheManager;

	private static int id = 0;

	@Before
	public void setUp() throws Exception {
		cacheManager = perun.getCacheManager();
		CacheManager.setCacheDisabled(false);
		CacheManager.setCacheTest(true);
		//CacheManagerTest counts with empty cache
		cacheManager.clearTestCache();
	}

	@After
	public void tearDown() throws Exception {
		CacheManager.setCacheTest(false);
	}



	// GET METHODS TESTS ----------------------------------------------


	@Test
	public void getAllNonEmptyAttributesByPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyAttributesByPrimaryHolder");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		Attribute groupAttr = setUpGroupAttribute();
		Attribute groupResourceAttr = setUpGroupResourceAttribute();
		setUpGroupAttributeDefinition();
		cacheManager.setAttribute(groupAttr, primaryHolder);
		cacheManager.setAttribute(groupResourceAttr, primaryHolder, secondaryHolder);
		setUpVirtualGroupAttribute();

		List<Attribute> attrs = cacheManager.getAllNonEmptyAttributes(primaryHolder);

		assertTrue("result should contain group attribute", attrs.contains(groupAttr));
		assertTrue("result should not contain group-resource attribute", !attrs.contains(groupResourceAttr));
		assertTrue("it should return only 1 attribute", attrs.size() == 1);
	}

	@Test
	public void getAllNonEmptyAttributesByPrimaryHolderEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyAttributesByPrimaryHolderEmpty");

		Holder holder = new Holder(0, Holder.HolderType.GROUP);

		List<Attribute> attrs = cacheManager.getAllNonEmptyAttributes(holder);

		assertTrue("there should be no returned attributes", attrs.isEmpty());
	}

	@Test
	public void getAllNonEmptyAttributesByHolders() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyAttributesByHolders");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		Attribute groupAttr = setUpGroupAttribute();
		Attribute groupResourceAttr = setUpGroupResourceAttribute();
		setUpGroupAttributeDefinition();
		cacheManager.setAttribute(groupAttr, primaryHolder);
		cacheManager.setAttribute(groupResourceAttr, primaryHolder, secondaryHolder);
		setUpVirtualGroupResourceAttribute();

		List<Attribute> attrs = cacheManager.getAllNonEmptyAttributes(primaryHolder, secondaryHolder);

		assertTrue("result should contain group-resource attribute", attrs.contains(groupResourceAttr));
		assertTrue("result should not contain group attribute", !attrs.contains(groupAttr));
		assertTrue("it should return only 1 attribute", attrs.size() == 1);
	}

	@Test
	public void getAllNonEmptyAttributesByHoldersEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyAttributesByHoldersEmpty");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		List<Attribute> attrs = cacheManager.getAllNonEmptyAttributes(primaryHolder, secondaryHolder);

		assertTrue("there should be no returned attributes", attrs.isEmpty());
	}

	@Test
	public void getAllNonEmptyAttributesByStartPartOfName() throws Exception {
		System.out.println(CLASS_NAME + "getAllAttributesByStartPartOfName");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		Attribute groupAttr = setUpGroupAttribute();
		Attribute groupResourceAttr = setUpGroupResourceAttribute();
		AttributeDefinition attrDef = setUpGroupAttributeDefinition();
		cacheManager.setAttribute(groupAttr, primaryHolder);
		cacheManager.setAttribute(groupResourceAttr, primaryHolder, secondaryHolder);
		setUpVirtualGroupAttribute();

		List<Attribute> attrs = cacheManager.getAllAttributesByStartPartOfName("urn:perun:group:attribute-def:opt", primaryHolder);

		assertTrue("result should contain group attribute", attrs.contains(groupAttr));
		assertTrue("result should contain attribute definition", attrs.contains(attrDef));
		assertTrue("result should not contain group-resource attribute", !attrs.contains(groupResourceAttr));
		assertTrue("it should return 2 attributes", attrs.size() == 2);
	}

	@Test
	public void getAllNonEmptyAttributesByStartPartOfNameEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyAttributesByStartPartOfNameEmpty");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		List<Attribute> attrs = cacheManager.getAllAttributesByStartPartOfName("urn:perun:group:attribute-def:opt", primaryHolder);

		assertTrue("there should be no returned attributes", attrs.size() == 0);
	}

	@Test
	public void getUserFacilityAttributesForAnyUser() throws Exception {
		System.out.println(CLASS_NAME + "getUserFacilityAttributesForAnyUser");

		Holder primaryHolder = new Holder(0, Holder.HolderType.USER);
		Holder primaryHolder1 = new Holder(1, Holder.HolderType.USER);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.FACILITY);

		Attribute userFacilityAttr = setUpUserFacilityAttribute();
		Attribute userAttr = setUpUserAttribute();
		cacheManager.setAttribute(userFacilityAttr, primaryHolder, secondaryHolder);
		cacheManager.setAttribute(userFacilityAttr, primaryHolder1, secondaryHolder);
		cacheManager.setAttribute(userAttr, primaryHolder);
		setUpGroupAttributeDefinition();
		setUpVirtualUserFacilityAttribute();

		List<Attribute> attrs = cacheManager.getUserFacilityAttributesForAnyUser(secondaryHolder.getId());

		assertTrue("result should contain user-facility attribute", attrs.contains(userFacilityAttr));
		assertTrue("it should return 2 attributes", attrs.size() == 2);
	}

	@Test
	public void getUserFacilityAttributesForAnyUserEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getUserFacilityAttributesForAnyUserEmpty");

		List<Attribute> attrs = cacheManager.getUserFacilityAttributesForAnyUser(0);

		assertTrue("there should be no returned attributes", attrs.size() == 0);
	}

	@Test
	public void getAttributesByNamesAndPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesByNamesAndPrimaryHolder");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		Attribute groupAttr = setUpGroupAttribute();
		Attribute groupResourceAttr = setUpGroupResourceAttribute();
		AttributeDefinition groupAttrDef = setUpGroupAttributeDefinition();
		AttributeDefinition resourceAttrDef = setUpResourceAttributeDefinition();
		cacheManager.setAttribute(groupAttr, primaryHolder);
		cacheManager.setAttribute(groupResourceAttr, primaryHolder, secondaryHolder);

		List<String> attributeNames = new ArrayList<>();
		attributeNames.add(groupAttr.getName());
		attributeNames.add(groupResourceAttr.getName());
		attributeNames.add(groupAttrDef.getName());
		attributeNames.add(resourceAttrDef.getName());

		List<Attribute> attrs = cacheManager.getAttributesByNames(attributeNames, primaryHolder);

		assertTrue("result should contain group attribute", attrs.contains(groupAttr));
		assertTrue("result should contain group attribute definition", attrs.contains(groupAttrDef));
		assertTrue("it should return 2 attributes", attrs.size() == 2);
	}

	@Test
	public void getAttributesByNamesAndPrimaryHolderEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesByNamesAndPrimaryHolderEmpty");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		List<String> attributeNames = new ArrayList<>();
		List<Attribute> attrs = cacheManager.getAttributesByNames(attributeNames, primaryHolder);

		assertTrue("there should be no returned attributes", attrs.size() == 0);
	}

	@Test
	public void getAttributesByNamesAndHolders() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesByNamesAndHolders");

		Holder primaryHolder = new Holder(0, Holder.HolderType.MEMBER);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.GROUP);

		Attribute groupAttr = setUpGroupAttribute();
		Attribute memberGroupAttr = setUpMemberGroupAttribute();
		AttributeDefinition memberGroupAttrDef = setUpMemberGroupAttributeDefinition();
		AttributeDefinition groupResourceAttrDef = setUpGroupResourceAttributeDefinition();
		cacheManager.setAttribute(groupAttr, secondaryHolder);
		cacheManager.setAttribute(memberGroupAttr, primaryHolder, secondaryHolder);

		List<String> attributeNames = new ArrayList<>();
		attributeNames.add(groupAttr.getName());
		attributeNames.add(memberGroupAttr.getName());
		attributeNames.add(memberGroupAttrDef.getName());
		attributeNames.add(groupResourceAttrDef.getName());

		List<Attribute> attrs = cacheManager.getAttributesByNames(attributeNames, primaryHolder, secondaryHolder);

		assertTrue("result should contain member-group attribute", attrs.contains(memberGroupAttr));
		assertTrue("result should contain member-group attribute definition", attrs.contains(memberGroupAttrDef));
		assertTrue("it should return 2 attributes", attrs.size() == 2);
	}

	@Test
	public void getAttributesByNamesAndHoldersEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesByNamesAndHoldersEmpty");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);
		List<String> attributeNames = new ArrayList<>();
		List<Attribute> attrs = cacheManager.getAttributesByNames(attributeNames, primaryHolder, secondaryHolder);

		assertTrue("there should be no returned attributes", attrs.size() == 0);
	}

	@Test
	public void getVirtualAttributesByPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "getVirtualAttributesByPrimaryHolder");

		AttributeDefinition memberVirtAttr = setUpVirtualMemberAttribute();
		AttributeDefinition memberResourceVirtAttr = setUpVirtualMemberResourceAttribute();
		setUpGroupAttribute();
		setUpMemberGroupAttributeDefinition();

		List<Attribute> attrs = cacheManager.getVirtualAttributes(Holder.HolderType.MEMBER);

		assertTrue("result should contain virtual member attribute definition", attrs.contains(memberVirtAttr));
		assertTrue("result should not contain virtual member-resource attribute definition", !attrs.contains(memberResourceVirtAttr));
		assertTrue("it should return only 1 attribute", attrs.size() == 1);
	}

	@Test
	public void getVirtualAttributesByPrimaryHolderEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getVirtualAttributesByPrimaryHolderEmpty");

		List<Attribute> attrs = cacheManager.getVirtualAttributes(Holder.HolderType.MEMBER);

		assertTrue("there should be no returned attributes", attrs.size() == 0);
	}

	@Test
	public void getVirtualAttributesByHolders() throws Exception {
		System.out.println(CLASS_NAME + "getVirtualAttributesByHolders");

		AttributeDefinition memberVirtAttr = setUpVirtualMemberAttribute();
		AttributeDefinition memberResourceVirtAttr = setUpVirtualMemberResourceAttribute();
		setUpGroupAttribute();
		setUpMemberGroupAttributeDefinition();

		List<Attribute> attrs = cacheManager.getVirtualAttributes(Holder.HolderType.MEMBER, Holder.HolderType.RESOURCE);

		assertTrue("result should contain virtual member-resource attribute definition", attrs.contains(memberResourceVirtAttr));
		assertTrue("result should not contain virtual member attribute definition", !attrs.contains(memberVirtAttr));
		assertTrue("it should return only 1 attribute", attrs.size() == 1);
	}

	@Test
	public void getVirtualAttributesByHoldersEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getVirtualAttributesByHoldersEmpty");

		List<Attribute> attrs = cacheManager.getVirtualAttributes(Holder.HolderType.MEMBER, Holder.HolderType.RESOURCE);

		assertTrue("there should be no returned attributes", attrs.size() == 0);
	}

	@Test
	public void getAttributeByNameAndPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndPrimaryHolder");

		Attribute attr = setUpGroupAttribute();
		Holder holder = new Holder(0, Holder.HolderType.GROUP);
		cacheManager.setAttribute(attr, holder);

		Attribute returnedAttr = cacheManager.getAttributeByName(attr.getName(), holder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test
	public void getAttributeByNameAndPrimaryHolderWhenOnlyDefinitionExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndPrimaryHolderWhenOnlyDefinitionExists");

		AttributeDefinition attr = setUpGroupAttributeDefinition();
		Holder holder = new Holder(0, Holder.HolderType.GROUP);

		Attribute returnedAttr = cacheManager.getAttributeByName(attr.getName(), holder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test(expected = AttributeNotExistsException.class)
	public void getAttributeByNameAndPrimaryHolderNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndPrimaryHolderNotExists");

		Holder holder = new Holder(0, Holder.HolderType.GROUP);

		cacheManager.getAttributeByName("urn:perun:group:attribute-def:opt:group-test-attribute", holder);
	}

	@Test
	public void getAttributeByNameAndHolders() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndHolders");

		Attribute attr = setUpGroupResourceAttribute();
		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);
		cacheManager.setAttribute(attr, primaryHolder, secondaryHolder);

		Attribute returnedAttr = cacheManager.getAttributeByName(attr.getName(), primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test
	public void getAttributeByNameAndHoldersWhenOnlyDefinitionExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndHoldersWhenOnlyDefinitionExists");

		AttributeDefinition attr = setUpGroupResourceAttributeDefinition();
		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		Attribute returnedAttr = cacheManager.getAttributeByName(attr.getName(), primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test(expected = AttributeNotExistsException.class)
	public void getAttributeByNameAndHoldersNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByNameAndHoldersNotExists");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		cacheManager.getAttributeByName("urn:perun:group_resource:attribute-def:opt:group-resource-test-attribute", primaryHolder, secondaryHolder);
	}

	@Test
	public void getAttributeByIdAndPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByIdAndPrimaryHolder");

		Attribute attr = setUpGroupAttribute();
		Holder holder = new Holder(0, Holder.HolderType.GROUP);
		cacheManager.setAttribute(attr, holder);

		Attribute returnedAttr = cacheManager.getAttributeById(attr.getId(), holder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test
	public void getAttributeByIdAndPrimaryHolderWhenOnlyDefinitionExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByIdAndPrimaryHolderWhenOnlyDefinitionExists");

		AttributeDefinition attr = setUpGroupAttributeDefinition();
		Holder holder = new Holder(0, Holder.HolderType.GROUP);

		Attribute returnedAttr = cacheManager.getAttributeById(attr.getId(), holder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test(expected = AttributeNotExistsException.class)
	public void getAttributeByIdAndPrimaryHolderNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByIdAndPrimaryHolderNotExists");

		Holder holder = new Holder(0, Holder.HolderType.GROUP);

		cacheManager.getAttributeById(1, holder);
	}

	@Test
	public void getAttributeByIdAndHolders() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByIdAndHolders");

		Attribute attr = setUpGroupResourceAttribute();
		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);
		cacheManager.setAttribute(attr, primaryHolder, secondaryHolder);

		Attribute returnedAttr = cacheManager.getAttributeById(attr.getId(), primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test
	public void getAttributeByIdAndHoldersWhenOnlyDefinitionExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByIdAndHoldersWhenOnlyDefinitionExists");

		AttributeDefinition attr = setUpGroupResourceAttributeDefinition();
		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		Attribute returnedAttr = cacheManager.getAttributeById(attr.getId(), primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test(expected = AttributeNotExistsException.class)
	public void getAttributeByIdAndHoldersNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeByIdAndHoldersNotExists");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);

		cacheManager.getAttributeById(1, primaryHolder, secondaryHolder);
	}

	@Test
	public void getAttributesDefinitions() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesDefinitions");

		List<AttributeDefinition> attributeDefinitions = setUpAttributesDefinitions();
		List<AttributeDefinition> returnedAttrDefinitions = cacheManager.getAttributesDefinitions();

		assertEquals("returned attributes are not same as stored", attributeDefinitions, returnedAttrDefinitions);
	}

	@Test
	public void getAttributesDefinitionsEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesDefinitionsEmpty");

		List<AttributeDefinition> returnedAttrDefinitions = cacheManager.getAttributesDefinitions();

		assertTrue("there should be no returned attributes", returnedAttrDefinitions.isEmpty());
	}

	@Test
	public void getAttributesDefinitionsByNamespace() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesDefinitionsByNamespace");

		List<AttributeDefinition> attributeDefinitions = setUpAttributesDefinitions();
		String namespace = attributeDefinitions.get(0).getNamespace();

		List<AttributeDefinition> returnedAttrDefinitions = cacheManager.getAttributesDefinitionsByNamespace(namespace);

		assertTrue("it should return only 1 attribute", returnedAttrDefinitions.size() == 1);
		assertEquals("namespace should be same", namespace, returnedAttrDefinitions.get(0).getNamespace());

	}

	@Test
	public void getAttributesDefinitionsByNamespaceNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributesDefinitionsByNamespaceNotExists");

		setUpAttributesDefinitions();
		List<AttributeDefinition> returnedAttrDefinitions = cacheManager.getAttributesDefinitionsByNamespace("urn:perun:groupB:attribute-def:opt");

		assertTrue("it should not return any attribute", returnedAttrDefinitions.size() == 0);
	}

	@Test
	public void getAttributeDefinition() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeDefinition");

		AttributeDefinition attrDef = setUpGroupAttributeDefinition();
		AttributeDefinition returnedAttr = cacheManager.getAttributeDefinition(attrDef.getName());

		assertEquals("returned attribute is not same as stored", attrDef, returnedAttr);
	}


	@Test(expected = AttributeNotExistsException.class)
	public void getAttributeDefinitionNotExists() throws Exception{
		System.out.println(CLASS_NAME + "getAttributeDefinitionNotExists");

		cacheManager.getAttributeDefinition("urn:perun:group:attribute-def:opt:group-test-attribute");
	}

	@Test
	public void getAttributeDefinitionById() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeDefinitionById");

		AttributeDefinition attrDef = setUpGroupAttributeDefinition();
		AttributeDefinition returnedAttr = cacheManager.getAttributeDefinitionById(attrDef.getId());

		assertEquals("returned attribute is not same as stored", attrDef, returnedAttr);
	}

	@Test(expected = AttributeNotExistsException.class)
	public void getAttributeDefinitionByIdNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getAttributeDefinitionByIdNotExists");

		AttributeDefinition returnedAttr = cacheManager.getAttributeDefinitionById(0);

		assertTrue("it should not return attribute", returnedAttr == null);
	}

	@Test
	public void getAllNonEmptyEntitylessAttributesByKey() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyEntitylessAttributesByKey");

		String key = "subject";

		Attribute entitylessAttr = setUpEntitylessAttribute();
		setUpEntitylessAttributeDefinition();
		setUpGroupAttribute();
		setUpVirtualGroupAttribute();
		cacheManager.setEntitylessAttribute(entitylessAttr, key);

		List<Attribute> attrs = cacheManager.getAllNonEmptyEntitylessAttributes(key);

		assertTrue("result should contain entityless attribute", attrs.contains(entitylessAttr));
		assertTrue("it should return only 1 attribute", attrs.size() == 1);
	}

	@Test
	public void getAllNonEmptyEntitylessAttributesByKeyEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyEntitylessAttributesByKeyEmpty");

		List<Attribute> attrs = cacheManager.getAllNonEmptyEntitylessAttributes("subject");

		assertTrue("there should be no returned attributes", attrs.isEmpty());
	}

	@Test
	public void getAllNonEmptyEntitylessAttributesByName() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyEntitylessAttributesByName");

		String key = "subject";

		Attribute entitylessAttr = setUpEntitylessAttribute();
		setUpEntitylessAttributeDefinition();
		setUpGroupAttribute();
		setUpVirtualGroupAttribute();
		cacheManager.setEntitylessAttribute(entitylessAttr, key);

		List<Attribute> attrs = cacheManager.getAllNonEmptyEntitylessAttributesByName(entitylessAttr.getName());

		assertTrue("result should contain entityless attribute", attrs.contains(entitylessAttr));
		assertTrue("it should return only 1 attribute", attrs.size() == 1);
	}

	@Test
	public void getAllNonEmptyEntitylessAttributesByNameWhenOnlyDefinitionExists() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyEntitylessAttributesByNameWhenOnlyDefinitionExists");

		String key = "Test subject";

		Attribute entitylessAttr = setUpEntitylessAttribute();
		AttributeDefinition entitylessAttrDef = setUpEntitylessAttributeDefinition();
		cacheManager.setEntitylessAttribute(entitylessAttr, key);

		List<Attribute> attrs = cacheManager.getAllNonEmptyEntitylessAttributesByName(entitylessAttrDef.getName());

		assertTrue("result should not contain entityless attribute definition", !attrs.contains(entitylessAttrDef));
		assertTrue("there should be no returned attributes", attrs.isEmpty());
	}

	@Test
	public void getAllNonEmptyEntitylessAttributesByNameEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getAllNonEmptyEntitylessAttributesByNameEmpty");

		List<Attribute> attrs = cacheManager.getAllNonEmptyEntitylessAttributesByName("attr-name");

		assertTrue("there should be no returned attributes", attrs.isEmpty());
	}

	@Test
	public void getEntitylessAttributeByNameAndKey() throws Exception {
		System.out.println(CLASS_NAME + "getEntitylessAttributeByNameAndKey");

		String key = "Test subject";
		Attribute attr = setUpEntitylessAttribute();
		cacheManager.setEntitylessAttribute(attr, key);

		Attribute returnedAttr = cacheManager.getEntitylessAttributeByNameAndKey(key, attr.getName());

		assertEquals("returned attribute is not same as stored", attr, returnedAttr);
	}

	@Test
	public void getEntitylessAttributeByNameAndKeyWhenOnlyDefinitionExists() throws Exception {
		System.out.println(CLASS_NAME + "getEntitylessAttributeByNameAndKeyWhenOnlyDefinitionExists");

		String key = "Test subject";
		AttributeDefinition attrDef = setUpEntitylessAttributeDefinition();

		Attribute returnedAttr = cacheManager.getEntitylessAttributeByNameAndKey(key, attrDef.getName());

		assertEquals("returned attribute is not same as stored", attrDef, returnedAttr);
	}

	@Test(expected = AttributeNotExistsException.class)
	public void getEntitylessAttributeByNameAndKeyNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getEntitylessAttributeByNameAndKeyNotExists");

		cacheManager.getEntitylessAttributeByNameAndKey("subject", "name");
	}

	@Test
	public void getEntitylessAttrValue() throws Exception {
		System.out.println(CLASS_NAME + "getEntitylessAttrValue");

		String key = "Test subject";
		Attribute attr = setUpEntitylessAttribute();
		cacheManager.setEntitylessAttribute(attr, key);

		String value = cacheManager.getEntitylessAttrValue(attr.getId(), key);

		assertEquals("returned attribute value is not same as stored", attr.getValue(), value);
	}

	@Test
	public void getEntitylessAttrValueNotExists() throws Exception {
		System.out.println(CLASS_NAME + "getEntitylessAttrValueNotExists");

		String value = cacheManager.getEntitylessAttrValue(0, "Test subject");

		assertEquals("returned attribute value is not same as stored", null, value);
	}

	@Test
	public void getEntitylessAttrKeys() throws Exception {
		System.out.println(CLASS_NAME + "getEntitylessAttrKeys");

		String key = "Test subject";
		String key1 = "Test subject1";
		Attribute attr = setUpEntitylessAttribute();
		cacheManager.setEntitylessAttribute(attr, key);
		cacheManager.setEntitylessAttribute(attr, key1);

		List<String> keys = cacheManager.getEntitylessAttrKeys(attr.getName());

		assertTrue("result should contain this entityless attribute subject", keys.contains(key));
		assertTrue("result should contain this entityless attribute subject", keys.contains(key1));
		assertTrue("it should return 2 attributes", keys.size() == 2);
	}

	@Test
	public void getEntitylessAttrKeysEmpty() throws Exception {
		System.out.println(CLASS_NAME + "getEntitylessAttrKeysEmpty");

		AttributeDefinition attrDef = setUpEntitylessAttributeDefinition();
		List<String> keys = cacheManager.getEntitylessAttrKeys(attrDef.getName());

		assertTrue("there should be no returned keys", keys.isEmpty());
	}

	@Test
	public void checkAttributeExists() throws Exception {
		System.out.println(CLASS_NAME + "checkAttributeExists");

		AttributeDefinition attrDef = setUpEntitylessAttributeDefinition();

		assertTrue("attribute should exist", cacheManager.checkAttributeExists(attrDef));
	}









// SET METHODS TESTS ----------------------------------------------


	@Test
	public void setAttributeWithPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "setAttributeWithPrimaryHolder");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Attribute attribute = setUpGroupAttribute();
		cacheManager.setAttribute(attribute, primaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeById(attribute.getId(), primaryHolder));
		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeByName(attribute.getName(), primaryHolder));
	}

	@Test
	public void setAttributeWithHolders() throws Exception {
		System.out.println(CLASS_NAME + "setAttributeWithHolders");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);
		Attribute attribute = setUpGroupResourceAttribute();
		cacheManager.setAttribute(attribute, primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeById(attribute.getId(), primaryHolder, secondaryHolder));
		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeByName(attribute.getName(), primaryHolder, secondaryHolder));
	}

	@Test
	public void setAttributeWithExistenceCheckWithPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "setAttributeWithExistenceCheckWithPrimaryHolder");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Attribute attribute = setUpGroupAttribute();
		cacheManager.setAttributeWithExistenceCheck(attribute, primaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeById(attribute.getId(), primaryHolder));
		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeByName(attribute.getName(), primaryHolder));

		String createdAt = attribute.getValueCreatedAt();
		String createdBy = attribute.getValueCreatedBy();
		String time = "2016-04-25";
		String creator = "Test";

		attribute.setValueCreatedAt(time);
		attribute.setValueCreatedBy(creator);
		attribute.setValueModifiedAt(time);
		attribute.setValueModifiedBy(creator);
		cacheManager.setAttributeWithExistenceCheck(attribute, primaryHolder);

		Attribute attributeById = cacheManager.getAttributeById(attribute.getId(), primaryHolder);
		Attribute attributeByName = cacheManager.getAttributeByName(attribute.getName(), primaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, attributeById);
		assertEquals("returned attribute is not same as stored", attribute, attributeByName);
		assertEquals("returned attribute valueModifiedAt is not same as stored", time, attributeById.getValueModifiedAt());
		assertEquals("returned attribute valueModifiedAt is not same as stored", time, attributeByName.getValueModifiedAt());
		assertEquals("returned attribute valueModifiedBy is not same as stored", creator, attributeById.getValueModifiedBy());
		assertEquals("returned attribute valueModifiedBy is not same as stored", creator, attributeByName.getValueModifiedBy());
		assertEquals("returned attribute valueCreatedAt is not same as stored", createdAt, attributeById.getValueCreatedAt());
		assertEquals("returned attribute valueCreatedAt is not same as stored", createdAt, attributeByName.getValueCreatedAt());
		assertEquals("returned attribute valueCreatedBy is not same as stored", createdBy, attributeById.getValueCreatedBy());
		assertEquals("returned attribute valueCreatedBy is not same as stored", createdBy, attributeByName.getValueCreatedBy());
	}

	@Test
	public void setAttributeWithExistenceCheckWithHolders() throws Exception {
		System.out.println(CLASS_NAME + "setAttributeWithExistenceCheckWithHolders");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);
		Attribute attribute = setUpGroupAttribute();
		cacheManager.setAttributeWithExistenceCheck(attribute, primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeById(attribute.getId(), primaryHolder, secondaryHolder));
		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeByName(attribute.getName(), primaryHolder, secondaryHolder));

		String createdAt = attribute.getValueCreatedAt();
		String createdBy = attribute.getValueCreatedBy();
		String time = "2016-04-25";
		String creator = "Test";

		attribute.setValueCreatedAt(time);
		attribute.setValueCreatedBy(creator);
		attribute.setValueModifiedAt(time);
		attribute.setValueModifiedBy(creator);
		cacheManager.setAttributeWithExistenceCheck(attribute, primaryHolder, secondaryHolder);

		Attribute attributeById = cacheManager.getAttributeById(attribute.getId(), primaryHolder, secondaryHolder);
		Attribute attributeByName = cacheManager.getAttributeByName(attribute.getName(), primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, attributeById);
		assertEquals("returned attribute is not same as stored", attribute, attributeByName);
		assertEquals("returned attribute valueModifiedAt is not same as stored", time, attributeById.getValueModifiedAt());
		assertEquals("returned attribute valueModifiedAt is not same as stored", time, attributeByName.getValueModifiedAt());
		assertEquals("returned attribute valueModifiedBy is not same as stored", creator, attributeById.getValueModifiedBy());
		assertEquals("returned attribute valueModifiedBy is not same as stored", creator, attributeByName.getValueModifiedBy());
		assertEquals("returned attribute valueCreatedAt is not same as stored", createdAt, attributeById.getValueCreatedAt());
		assertEquals("returned attribute valueCreatedAt is not same as stored", createdAt, attributeByName.getValueCreatedAt());
		assertEquals("returned attribute valueCreatedBy is not same as stored", createdBy, attributeById.getValueCreatedBy());
		assertEquals("returned attribute valueCreatedBy is not same as stored", createdBy, attributeByName.getValueCreatedBy());
	}

	@Test
	public void setAttributeDefinition() throws Exception {
		System.out.println(CLASS_NAME + "setAttributeDefinition");

		AttributeDefinition attrDef = setUpGroupAttributeDefinition();

		assertEquals("returned attribute definition is not same as stored", attrDef, cacheManager.getAttributeDefinitionById(attrDef.getId()));
		assertEquals("returned attribute definition is not same as stored", attrDef, cacheManager.getAttributeDefinition(attrDef.getName()));
	}

	@Test
	public void setEntitylessAttribute() throws Exception {
		System.out.println(CLASS_NAME + "setEntitylessAttribute");

		String key = "subject";

		Attribute entitylessAttr = setUpEntitylessAttribute();
		cacheManager.setEntitylessAttribute(entitylessAttr, key);

		assertEquals("returned entityless attribute is not same as stored", entitylessAttr, cacheManager.getEntitylessAttributeByNameAndKey(key, entitylessAttr.getName()));
		assertEquals("returned entityless attribute value is not same as stored", entitylessAttr.getValue(), cacheManager.getEntitylessAttrValue(entitylessAttr.getId(), key));
	}

	@Test
	public void setEntitylessAttributeWithExistenceCheck() throws Exception {
		System.out.println(CLASS_NAME + "setEntitylessAttributeWithExistenceCheck");

		String key = "subject";

		Attribute entitylessAttr = setUpEntitylessAttribute();
		cacheManager.setEntitylessAttributeWithExistenceCheck(entitylessAttr, key);

		assertEquals("returned entityless attribute is not same as stored", entitylessAttr, cacheManager.getEntitylessAttributeByNameAndKey(key, entitylessAttr.getName()));
		assertEquals("returned entityless attribute value is not same as stored", entitylessAttr.getValue(), cacheManager.getEntitylessAttrValue(entitylessAttr.getId(), key));

		String createdAt = entitylessAttr.getValueCreatedAt();
		String createdBy = entitylessAttr.getValueCreatedBy();
		String time = "2016-04-25";
		String creator = "Test";

		entitylessAttr.setValueCreatedAt(time);
		entitylessAttr.setValueCreatedBy(creator);
		entitylessAttr.setValueModifiedAt(time);
		entitylessAttr.setValueModifiedBy(creator);
		cacheManager.setEntitylessAttributeWithExistenceCheck(entitylessAttr, key);

		Attribute attrByNameAndKey = cacheManager.getEntitylessAttributeByNameAndKey(key, entitylessAttr.getName());

		assertEquals("returned attribute is not same as stored", entitylessAttr, attrByNameAndKey);
		assertEquals("returned attribute valueModifiedAt is not same as stored", time, attrByNameAndKey.getValueModifiedAt());
		assertEquals("returned attribute valueModifiedBy is not same as stored", creator, attrByNameAndKey.getValueModifiedBy());
		assertEquals("returned attribute valueCreatedAt is not same as stored", createdAt, attrByNameAndKey.getValueCreatedAt());
		assertEquals("returned attribute valueCreatedBy is not same as stored", createdBy, attrByNameAndKey.getValueCreatedBy());
	}

	@Test
	public void removeAttributeWithPrimaryHolder() throws Exception {
		System.out.println(CLASS_NAME + "removeAttributeWithPrimaryHolder");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		AttributeDefinition attrDef = setUpGroupAttributeDefinition();
		Attribute attribute = new Attribute(attrDef);
		attribute.setValue("Test");
		cacheManager.setAttribute(attribute, primaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeById(attribute.getId(), primaryHolder));
		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeByName(attribute.getName(), primaryHolder));

		cacheManager.removeAttribute(attribute, primaryHolder);

		assertEquals("should return only attribute definition", attrDef, cacheManager.getAttributeById(attribute.getId(), primaryHolder));
		assertEquals("should return only attribute definition", attrDef, cacheManager.getAttributeByName(attribute.getName(), primaryHolder));
	}

	@Test
	public void removeAttributeWithHolders() throws Exception {
		System.out.println(CLASS_NAME + "removeAttributeWithHolders");

		Holder primaryHolder = new Holder(0, Holder.HolderType.GROUP);
		Holder secondaryHolder = new Holder(0, Holder.HolderType.RESOURCE);
		AttributeDefinition attrDef = setUpGroupResourceAttributeDefinition();
		Attribute attribute = new Attribute(attrDef);
		attribute.setValue("Test");
		cacheManager.setAttribute(attribute, primaryHolder, secondaryHolder);

		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeById(attribute.getId(), primaryHolder, secondaryHolder));
		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getAttributeByName(attribute.getName(), primaryHolder, secondaryHolder));

		cacheManager.removeAttribute(attribute, primaryHolder, secondaryHolder);

		assertEquals("should return only attribute definition", attrDef, cacheManager.getAttributeById(attribute.getId(), primaryHolder, secondaryHolder));
		assertEquals("should return only attribute definition", attrDef, cacheManager.getAttributeByName(attribute.getName(), primaryHolder, secondaryHolder));
	}

	@Test
	public void removeEntitylessAttribute() throws Exception {
		System.out.println(CLASS_NAME + "removeEntitylessAttribute");

		String key = "subject";
		AttributeDefinition attrDef = setUpEntitylessAttributeDefinition();
		Attribute attribute = new Attribute(attrDef);
		attribute.setValue("Test");
		cacheManager.setEntitylessAttribute(attribute, key);

		assertEquals("returned attribute is not same as stored", attribute, cacheManager.getEntitylessAttributeByNameAndKey(key, attribute.getName()));

		cacheManager.removeEntitylessAttribute(attribute, key);

		assertEquals("should return only attribute definition", attrDef, cacheManager.getEntitylessAttributeByNameAndKey(key, attribute.getName()));
	}

	@Test
	public void updateAttributeDefinition() throws Exception {
		System.out.println(CLASS_NAME + "updateAttributeDefinition");

		AttributeDefinition attrDef = setUpGroupAttributeDefinition();

		String createdAt = attrDef.getCreatedAt();
		String createdBy = attrDef.getCreatedBy();
		String time = "2016-04-25";
		String creator = "Test";

		attrDef.setCreatedAt(time);
		attrDef.setCreatedBy(creator);
		attrDef.setModifiedAt(time);
		attrDef.setModifiedBy(creator);
		cacheManager.updateAttributeDefinition(attrDef);

		AttributeDefinition attrDefById = cacheManager.getAttributeDefinitionById(attrDef.getId());
		AttributeDefinition attrDefByName = cacheManager.getAttributeDefinition(attrDef.getName());

		assertEquals("returned attribute definition is not same as stored", attrDef, attrDefById);
		assertEquals("returned attribute definition is not same as stored", attrDef, attrDefByName);
		assertEquals("returned attribute definition modifiedAt is not same as stored", time, attrDefById.getModifiedAt());
		assertEquals("returned attribute definition modifiedAt is not same as stored", time, attrDefByName.getModifiedAt());
		assertEquals("returned attribute definition modifiedBy is not same as stored", creator, attrDefById.getModifiedBy());
		assertEquals("returned attribute definition modifiedBy is not same as stored", creator, attrDefByName.getModifiedBy());
		assertEquals("returned attribute definition createdAt is not same as stored", createdAt, attrDefById.getCreatedAt());
		assertEquals("returned attribute definition createdAt is not same as stored", createdAt, attrDefByName.getCreatedAt());
		assertEquals("returned attribute definition createdBy is not same as stored", createdBy, attrDefById.getCreatedBy());
		assertEquals("returned attribute definition createdBy is not same as stored", createdBy, attrDefByName.getCreatedBy());
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

		String time = "2016-04-24";
		String creator = "Admin";

		attr.setCreatedAt(time);
		attr.setCreatedBy(creator);
		attr.setModifiedAt(time);
		attr.setModifiedBy(creator);

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
