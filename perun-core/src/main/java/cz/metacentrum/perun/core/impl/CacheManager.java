package cz.metacentrum.perun.core.impl;

import cz.metacentrum.perun.core.api.*;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.implApi.CacheManagerApi;
import org.infinispan.Cache;
import org.infinispan.CacheSet;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.QueryFactory;
import org.springframework.jdbc.core.JdbcPerunTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import javax.transaction.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CacheManager implements CacheManagerApi {

	private EmbeddedCacheManager localCacheManager;
	private JdbcPerunTemplate jdbc;

	private static boolean cacheDisabled = false;
	private static boolean cacheTest = false;

	public static final String CACHE_NAME = "transactionalCache";
	public static final String TEST_CACHE_NAME = "testCache";
	private static final String SIMPLE_CACHE_NAME = "simpleCache";

	private static final String PRIMARY_HOLDER = "primaryHolder";
	private static final String SECONDARY_HOLDER = "secondaryHolder";
	private static final String HOLDER_ID = "id";
	private static final String HOLDER_TYPE = "type";
	private static final String NAME = "nameForSearch";
	private static final String NAMESPACE = "namespaceForSearch";
	private static final String FRIENDLY_NAME = "friendlyNameForSearch";
	private static final String TYPE = "typeForSearch";
	private static final String VALUE = "valueForSearch";
	private static final String SUBJECT = "subject";
	private static final String ID = "idForSearch";
	private static final String SAVED_BY_ID = "savedById";

	/* position of the Boolean in the list specifies a cache - if that cache is used true, else false
	for example if list has Boolean.True element at position 2, that means simpleCache2 is currently used */
	private static List<Boolean> usedCacheNames;

	public CacheManager(EmbeddedCacheManager localCacheManager) {
		this.localCacheManager = localCacheManager;
//		localCacheManager = new DefaultCacheManager("infinispan-config.xml");
//		localCacheManager.stop(); //TODO
	}

	public void setPerunPool(DataSource perunPool) throws InternalErrorException {
		this.jdbc = new JdbcPerunTemplate(perunPool);
	}

	public static boolean isCacheDisabled() {
		return cacheDisabled;
	}

	public static void setCacheDisabled(boolean cacheDisabled) {
		CacheManager.cacheDisabled = cacheDisabled;
	}

	public static boolean isCacheTest() {
		return cacheTest;
	}

	public static void setCacheTest(boolean cacheTest) {
		CacheManager.cacheTest = cacheTest;
	}

	@Override
	public void clearTestCache() {
		getCache(TEST_CACHE_NAME).clear();
	}

	@Override
	public boolean wasCacheUpdatedInTransaction() {
		List<String> cacheNames = getCacheNamesFromTransaction();
		//if cacheNames is null, there is no transaction
		if(cacheNames == null || cacheNames.get(0) == null) return false;
		return true;
	}

	/**
	 * Sets cache as updated in transaction. If there si no transaction, nothing happens.
	 */
	private void setCacheUpdatedInTransaction() {
		List<String> cacheNames = getCacheNamesFromTransaction();
		//if cacheNames is null, there is no transaction
		if(cacheNames != null) cacheNames.set(0, "w");
	}

	/**
	 * Gets cache by name.
	 * @param cacheName cache name
	 * @return cache with ignore return values flag set
	 */
	private Cache<Object, Object> getCache(String cacheName) {
		return localCacheManager.getCache(cacheName).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES);
	}

	/**
	 * Gets cache. If in tests, it returns test cache.
	 * @return cache with ignore return values flag set
	 */
	private Cache<Object, Object> getCache() {
		if(isCacheTest()) return localCacheManager.getCache(TEST_CACHE_NAME).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES);
		return localCacheManager.getCache(CACHE_NAME).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES);
	}

	/**
	 * Gets cache. If in nested transaction, it returns nested transaction cache, if in tests, it returns test cache.
	 * @return cache with ignore return values flag set
	 */
	private Cache<Object, Object> getCacheForWrite() {
		List<String> cacheNames = getCacheNamesFromTransaction();

		//if in nested transaction, return nested transaction cache
		if(cacheNames != null && cacheNames.size() > 2) return localCacheManager.getCache(cacheNames.get(cacheNames.size() - 1)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES);

		return getCache();
	}

	/**
	 * Gets list of cache names for read cache operations. The most nested transaction cache is the first in list.
	 * @return list of cache names
	 */
	private List<String> getCacheNamesForRead() {
		List<String> cacheNames = getCacheNamesFromTransaction();
		List<String> cacheNamesToReturn = new ArrayList<>();

		if(cacheNames != null) {
			for(String name: cacheNames) {
				cacheNamesToReturn.add(name);
			}
			Collections.reverse(cacheNamesToReturn);
			cacheNamesToReturn.remove(cacheNamesToReturn.size() - 1);
		} else {
			if(cacheTest) cacheNamesToReturn.add(TEST_CACHE_NAME);
			else cacheNamesToReturn.add(CACHE_NAME);
		}

		return cacheNamesToReturn;
	}

	/**
	 * Gets list of cache names from transaction.
	 * @return list of cache names
	 */
	private List<String> getCacheNamesFromTransaction() {
		return (List<String>) TransactionSynchronizationManager.getResource(this);
	}

	/**
	 * Gets transaction manager from cache.
	 * @return cache transaction manager
	 */
	private TransactionManager getCacheTransactionManager() {
		return this.getCache().getAdvancedCache().getTransactionManager();
	}

	/**
	 * Get the non empty attributes namespaces by primary and secondary holder.
	 *
	 * @param primaryHolderType primary holder type
	 * @param secondaryHolderType secondary holder type
	 * @return list of namespaces
	 * @throws InternalErrorException
	 */
	private List<String> getNonEmptyAttributesNamespaces(Holder.HolderType primaryHolderType, Holder.HolderType secondaryHolderType) throws InternalErrorException {

		List<String> nonEmptyAttrsNamespaces = new ArrayList<>();

		if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == Holder.HolderType.RESOURCE) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_RESOURCE_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_RESOURCE_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == Holder.HolderType.GROUP) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_GROUP_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_GROUP_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.USER && secondaryHolderType == Holder.HolderType.FACILITY) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_FACILITY_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_FACILITY_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.GROUP && secondaryHolderType == Holder.HolderType.RESOURCE) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_RESOURCE_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_RESOURCE_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.FACILITY && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_FACILITY_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_FACILITY_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.VO && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_VO_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_VO_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.GROUP && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.HOST && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_HOST_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_HOST_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.RESOURCE && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_RESOURCE_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_RESOURCE_ATTR_OPT);
		} else if(primaryHolderType == Holder.HolderType.USER && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_ATTR_OPT);
		} else if(primaryHolderType == null && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_ENTITYLESS_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_ENTITYLESS_ATTR_OPT);
		} else {
			throw new InternalErrorException("Holder type combination " + primaryHolderType + "," + secondaryHolderType + " is not defined.");
		}
		return nonEmptyAttrsNamespaces;
	}

	/**
	 * Get core attribute namespaces by primary holder.
	 *
	 * @param primaryHolderType primary holder type
	 * @return list of namespaces
	 * @throws InternalErrorException
	 */
	private List<String> getCoreAttributesNamespace(Holder.HolderType primaryHolderType) throws InternalErrorException {

		List<String> nonEmptyAttrsNamespaces = new ArrayList<>();

		if(primaryHolderType == Holder.HolderType.FACILITY) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_FACILITY_ATTR_CORE);
		} else if(primaryHolderType == Holder.HolderType.MEMBER) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_ATTR_CORE);
		} else if(primaryHolderType == Holder.HolderType.VO) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_VO_ATTR_CORE);
		} else if(primaryHolderType == Holder.HolderType.GROUP) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_ATTR_CORE);
		} else if(primaryHolderType == Holder.HolderType.HOST) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_HOST_ATTR_CORE);
		} else if(primaryHolderType == Holder.HolderType.RESOURCE) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_RESOURCE_ATTR_CORE);
		} else if(primaryHolderType == Holder.HolderType.USER) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_ATTR_CORE);
		} else {
			throw new InternalErrorException("Holder type " + primaryHolderType + " is not defined.");
		}
		return nonEmptyAttrsNamespaces;
	}

	/**
	 * Get virtual attribute namespace by primary and secondary holder.
	 *
	 * @param primaryHolderType primary holder type
	 * @param secondaryHolderType secondary holder type
	 * @return namespace
	 * @throws InternalErrorException
	 */
	private String getVirtualAttributesNamespace(Holder.HolderType primaryHolderType, Holder.HolderType secondaryHolderType) throws InternalErrorException {

		String virtualAttrNamespace;

		if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == Holder.HolderType.RESOURCE) {
			virtualAttrNamespace = AttributesManager.NS_MEMBER_RESOURCE_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.USER && secondaryHolderType == Holder.HolderType.FACILITY) {
			virtualAttrNamespace = AttributesManager.NS_USER_FACILITY_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == Holder.HolderType.GROUP) {
			virtualAttrNamespace = AttributesManager.NS_MEMBER_GROUP_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.FACILITY && secondaryHolderType == null) {
			virtualAttrNamespace = AttributesManager.NS_FACILITY_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == null) {
			virtualAttrNamespace = AttributesManager.NS_MEMBER_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.VO && secondaryHolderType == null) {
			virtualAttrNamespace = AttributesManager.NS_VO_ATTR_VIRT;
		}  else if(primaryHolderType == Holder.HolderType.GROUP && secondaryHolderType == null) {
			virtualAttrNamespace = AttributesManager.NS_GROUP_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.HOST && secondaryHolderType == null) {
			virtualAttrNamespace = AttributesManager.NS_HOST_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.RESOURCE && secondaryHolderType == null) {
			virtualAttrNamespace = AttributesManager.NS_RESOURCE_ATTR_VIRT;
		} else if(primaryHolderType == Holder.HolderType.USER && secondaryHolderType == null) {
			virtualAttrNamespace = AttributesManager.NS_USER_ATTR_VIRT;
		} else {
			throw new InternalErrorException("Holder type combination " + primaryHolderType + "," + secondaryHolderType + " is not defined.");
		}

		return virtualAttrNamespace;
	}

	/**
	 * Get attributes by names namespaces.
	 *
	 * @param primaryHolderType primary holder type
	 * @param secondaryHolderType secondary holder type
	 * @return list of namespaces
	 * @throws InternalErrorException
	 */
	private List<String> getAttributesByNamesNamespaces(Holder.HolderType primaryHolderType, Holder.HolderType secondaryHolderType) throws InternalErrorException {

		List<String> nonEmptyAttrsNamespaces = new ArrayList<>();

		if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == Holder.HolderType.GROUP) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_GROUP_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_GROUP_ATTR_OPT);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_GROUP_ATTR_VIRT);
		} else if(primaryHolderType == Holder.HolderType.VO && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_VO_ATTR_CORE);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_VO_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_VO_ATTR_OPT);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_VO_ATTR_VIRT);
		} else if(primaryHolderType == Holder.HolderType.MEMBER && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_ATTR_CORE);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_ATTR_OPT);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_MEMBER_ATTR_VIRT);
		} else if(primaryHolderType == Holder.HolderType.GROUP && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_ATTR_CORE);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_ATTR_OPT);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_GROUP_ATTR_VIRT);
		} else if(primaryHolderType == Holder.HolderType.USER && secondaryHolderType == null) {
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_ATTR_CORE);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_ATTR_DEF);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_ATTR_OPT);
			nonEmptyAttrsNamespaces.add(AttributesManager.NS_USER_ATTR_VIRT);
		} else {
			throw new InternalErrorException("Holder type combination " + primaryHolderType + "," + secondaryHolderType + " is not defined.");
		}
		return nonEmptyAttrsNamespaces;
	}

	@Override
	public List<Attribute> getAllNonEmptyAttributes(Holder holder) throws InternalErrorException {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(NAMESPACE).in(getCoreAttributesNamespace(holder.getType()))
						.and().having(SAVED_BY_ID).eq(1)
						.and(qf.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(holder.getId())
								.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(holder.getType())
								.and().having(SECONDARY_HOLDER).isNull()
								.or(qf.having(PRIMARY_HOLDER).isNull()
										.and().having(SECONDARY_HOLDER).isNull()
										.and().having(SUBJECT).isNull()))
						.or(qf.having(NAMESPACE).in(getNonEmptyAttributesNamespaces(holder.getType(), null))
							.and().having(SAVED_BY_ID).eq(1)
							.and().not().having(VALUE).isNull()
							.and(qf.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(holder.getId())
									.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(holder.getType())
									.and().having(SECONDARY_HOLDER).isNull()
									.or(qf.having(PRIMARY_HOLDER).isNull()
											.and().having(SECONDARY_HOLDER).isNull()
											.and().having(SUBJECT).isNull())))
						.toBuilder().build();

		return query.list();
	}

	@Override
	public List<Attribute> getAllNonEmptyAttributes(Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(primaryHolder.getId())
						.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(primaryHolder.getType())
						.and().having(SECONDARY_HOLDER + "." + HOLDER_ID).eq(secondaryHolder.getId())
						.and().having(SECONDARY_HOLDER + "." + HOLDER_TYPE).eq(secondaryHolder.getType())
						.and().having(NAMESPACE).in(getNonEmptyAttributesNamespaces(primaryHolder.getType(), secondaryHolder.getType()))
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return query.list();
	}

	@Override
	public List<Attribute> getAllAttributesByStartPartOfName(String startPartOfName, Holder holder) throws InternalErrorException {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		List<String> namespaces = getNonEmptyAttributesNamespaces(holder.getType(), null);
		namespaces.addAll(getCoreAttributesNamespace(holder.getType()));

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(NAMESPACE).in(namespaces)
						.and().having(NAME).like(startPartOfName + "%")
						.and().having(SAVED_BY_ID).eq(1)
						.and(qf.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(holder.getId())
								.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(holder.getType())
								.and().having(SECONDARY_HOLDER).isNull()
								.or(qf.having(PRIMARY_HOLDER).isNull()
										.and().having(SECONDARY_HOLDER).isNull()
										.and().having(SUBJECT).isNull()))
						.toBuilder().build();

		return removeDuplicates(query.<Attribute>list());
	}

	//TODO is this faster than from relational db?
	/**
	 * Removes duplicate attributes.
	 * Duplicate means that the same attribute is there with and also without value. If that happens, it removes the attribute without value.
	 *
	 * @param attributesWithDuplicates attribute list with duplicates
	 * @return list of attributes without duplicates
	 */
	private List<Attribute> removeDuplicates(List<Attribute> attributesWithDuplicates) {
		List<Integer> definitionIds = new ArrayList<>();
		List<Integer> valuesIds = new ArrayList<>();
		List<Attribute> attrsToReturn = new ArrayList<>();

		//find all attributes with value
		for (Attribute attr: attributesWithDuplicates) {
			if(attr.getValue() != null) {
				valuesIds.add(attr.getId());
				attrsToReturn.add(attr);
			} else {
				definitionIds.add(attr.getId());
			}
		}

		//remove definitions that already are in attrsToReturn list with value
		definitionIds.removeAll(valuesIds);

		//add definitions for which no attribute with value was found
		for(Integer defId: definitionIds) {
			for(Attribute attr: attributesWithDuplicates) {
				if(attr.getId() == defId) attrsToReturn.add(attr);
			}
		}

		return attrsToReturn;
	}

	@Override
	public List<Attribute> getUserFacilityAttributesForAnyUser(int facilityId) throws InternalErrorException {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(Holder.HolderType.USER)
						.and().having(SECONDARY_HOLDER + "." + HOLDER_ID).eq(facilityId)
						.and().having(SECONDARY_HOLDER + "." + HOLDER_TYPE).eq(Holder.HolderType.FACILITY)
						.and().having(NAMESPACE).in(getNonEmptyAttributesNamespaces(Holder.HolderType.USER, Holder.HolderType.FACILITY))
						.and().not().having(VALUE).isNull()
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return query.list();
	}

	@Override
	public List<Attribute> getAttributesByNames(List<String> attrNames, Holder holder) throws InternalErrorException {
		if(attrNames.isEmpty()) return new ArrayList<>();

		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(NAMESPACE).in(getAttributesByNamesNamespaces(holder.getType(), null))
						.and().having(NAME).in(attrNames)
						.and().having(SAVED_BY_ID).eq(1)
						.and(qf.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(holder.getId())
								.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(holder.getType())
								.and().having(SECONDARY_HOLDER).isNull()
								.or(qf.having(PRIMARY_HOLDER).isNull()
										.and().having(SECONDARY_HOLDER).isNull()
										.and().having(SUBJECT).isNull()))
						.toBuilder().build();


		return removeDuplicates(query.<Attribute>list());
	}

	@Override
	public List<Attribute> getAttributesByNames(List<String> attrNames, Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException {
		if(attrNames.isEmpty()) return new ArrayList<>();

		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(NAMESPACE).in(getAttributesByNamesNamespaces(primaryHolder.getType(), secondaryHolder.getType()))
						.and().having(NAME).in(attrNames)
						.and().having(SAVED_BY_ID).eq(1)
						.and(qf.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(primaryHolder.getId())
								.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(primaryHolder.getType())
								.and().having(SECONDARY_HOLDER + "." + HOLDER_ID).eq(secondaryHolder.getId())
								.and().having(SECONDARY_HOLDER + "." + HOLDER_TYPE).eq(secondaryHolder.getType())
								.or(qf.having(PRIMARY_HOLDER).isNull()
										.and().having(SECONDARY_HOLDER).isNull()
										.and().having(SUBJECT).isNull()))
						.toBuilder().build();

		return removeDuplicates(query.<Attribute>list());
	}

	//TODO cache
//	public List<Attribute> getAttributesByAttributeDefinition(AttributeDefinition attributeDefinition) throws InternalErrorException {
//		QueryFactory qf = Search.getQueryFactory(this.getCacheForWrite());
//
//		String entity = attributeDefinition.getEntity();
//
//		org.infinispan.query.dsl.Query query =
//				qf.from(AttributeHolders.class)
//						.having(NAMESPACE).in(getAttributesByNamesNamespaces(holderType, null))
//						.and().having(NAME).in(attrNames)
//						.and().having(SAVED_BY_ID).eq(1)
//						.and(
//								qf.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(holderId)
//										.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(holderType)
//										.and().having(SECONDARY_HOLDER).isNull()
//										.or().having(PRIMARY_HOLDER).isNull()
//										.and().having(SECONDARY_HOLDER).isNull()
//										.and().having(SUBJECT).isNull()
//						)
//						.toBuilder().build();
//
//		return query.list();
//	}

	@Override
	public List<Attribute> getVirtualAttributes(Holder.HolderType holderType) throws InternalErrorException {
		return getVirtualAttributes(holderType, null);
	}

	@Override
	public List<Attribute> getVirtualAttributes(Holder.HolderType primaryHolderType, Holder.HolderType secondaryHolderType) throws InternalErrorException {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().having(SUBJECT).isNull()
						.and().having(NAMESPACE).eq(getVirtualAttributesNamespace(primaryHolderType, secondaryHolderType))
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return query.list();
	}

	@Override
	public Attribute getAttributeByName(String attrName, Holder holder) throws AttributeNotExistsException {
		List<String> cacheNames = this.getCacheNamesForRead();

		for(String name: cacheNames) {
			Cache<Object, Object> cache = this.getCache(name);
			AttributeIdWithHolders attrId = new AttributeIdWithHolders(attrName, holder, null);
			Attribute attr = (Attribute) cache.get(attrId);
			if(attr != null) return attr;
		}
		for(String name: cacheNames) {
			Cache<Object, Object> cache = this.getCache(name);
			AttributeIdWithHolders attrId = new AttributeIdWithHolders(attrName);
			Attribute attr = (Attribute) cache.get(attrId);
			if(attr != null) return attr;
		}
		throw new AttributeNotExistsException(holder.getType() + " attribute - Attribute.name='" + attrName + "'");
	}

	@Override
	public Attribute getAttributeByName(String attrName, Holder primaryHolder, Holder secondaryHolder) throws AttributeNotExistsException {
		Cache<Object, Object> cache = this.getCache();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attrName, primaryHolder, secondaryHolder);
		Attribute attr = (Attribute) cache.get(attrId);

		if(attr != null) return attr;
		else {
			attrId = new AttributeIdWithHolders(attrName);
			attr = (Attribute) cache.get(attrId);
			if(attr == null) throw new AttributeNotExistsException(primaryHolder.getType() + "-" + secondaryHolder.getType() + " attribute - Attribute.name='" + attrName + "'");
			return attr;
		}
	}

	@Override
	public Attribute getAttributeById(int id, Holder holder) throws AttributeNotExistsException {
		Cache<Object, Object> cache = this.getCache();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(id, holder, null);
		Attribute attr = (Attribute) cache.get(attrId);

		if(attr != null) return attr;
		else {
			attrId = new AttributeIdWithHolders(id);
			attr = (Attribute) cache.get(attrId);
			if(attr == null) throw new AttributeNotExistsException(holder.getType() + " attribute - Attribute.id='" + id + "'");
			return attr;
		}
	}

	@Override
	public Attribute getAttributeById(int id, Holder primaryHolder, Holder secondaryHolder) throws AttributeNotExistsException {
		Cache<Object, Object> cache = this.getCache();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(id, primaryHolder, secondaryHolder);
		Attribute attr = (Attribute) cache.get(attrId);

		if(attr != null) return attr;
		else {
			attrId = new AttributeIdWithHolders(id);
			attr = (Attribute) cache.get(attrId);
			if(attr == null) throw new AttributeNotExistsException(primaryHolder.getType() + "-" + secondaryHolder.getType() + " attribute - Attribute.id='" + id + "'");
			return attr;
		}
	}

	//TODO
	public List<String> getAllSimilarAttributeNames(String startingPartOfAttributeName) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(NAME).like(startingPartOfAttributeName + "%")
						.and().having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().having(SUBJECT).isNull()
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		List<String> attrNames = new ArrayList<>();
		List<AttributeHolders> attrHolders = query.list();

		for(AttributeHolders attr: attrHolders) {
			attrNames.add(attr.getName());
		}

		return attrNames;
	}

	@Override
	public List<AttributeDefinition> getAttributesDefinitions() {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().having(SUBJECT).isNull()
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return query.list();
	}

	@Override
	public List<AttributeDefinition> getAttributesDefinitionsByNamespace(String namespace) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().having(SUBJECT).isNull()
						.and().having(NAMESPACE).eq(namespace)
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return query.list();
	}

	@Override
	public AttributeDefinition getAttributeDefinition(String attrName) throws AttributeNotExistsException {
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attrName);
		Attribute attr = (Attribute) this.getCache().get(attrId);
		if(attr == null) throw new AttributeNotExistsException("Attribute definition not exists - attribute.name='" + attrName + "'");
		return attr;
	}

	@Override
	public AttributeDefinition getAttributeDefinitionById(int id) throws AttributeNotExistsException {
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(id);
		Attribute attr = (Attribute) this.getCache().get(attrId);
		if(attr == null) throw new AttributeNotExistsException("Attribute definition not exists - attribute.id='" + id + "'");
		return attr;
	}

	@Override
	public List<Attribute> getAllNonEmptyEntitylessAttributes(String key) throws InternalErrorException {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().having(SUBJECT).eq(key)
						.and().having(NAMESPACE).in(getNonEmptyAttributesNamespaces(null, null))
						.and().not().having(VALUE).isNull()
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return query.list();
	}

	@Override
	public List<Attribute> getAllNonEmptyEntitylessAttributesByName(String attrName) throws InternalErrorException {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().having(NAME).eq(attrName)
						.and().having(NAMESPACE).in(getNonEmptyAttributesNamespaces(null, null))
						.and().not().having(VALUE).isNull()
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return query.list();
	}

	@Override
	public Attribute getEntitylessAttributeByNameAndKey(String key, String attrName) throws AttributeNotExistsException {
		Cache<Object, Object> cache = this.getCache();
		AttributeIdWithHolders id = new AttributeIdWithHolders(attrName, key);
		Attribute attr = (Attribute) this.getCache().get(id);

		if(attr != null) return attr;
		else {
			id = new AttributeIdWithHolders(attrName);
			attr = (Attribute) cache.get(id);
			if(attr == null) throw new AttributeNotExistsException("Entityless attribute - attribute.name='" + attrName + "'");
			return attr;
		}
	}

	@Override
	public String getEntitylessAttrValue(int attrId, String key) throws InternalErrorException {
		AttributeIdWithHolders id = new AttributeIdWithHolders(attrId, key);
		Attribute attr = (Attribute) this.getCache().get(id);
		if(attr != null) return BeansUtils.attributeValueToString(attr);
		else return null;
	}

	@Override
	public List<String> getEntitylessAttrKeys(String attrName) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().not().having(SUBJECT).isNull()
						.and().having(NAME).like(attrName)
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		List<String> attrKeys = new ArrayList<>();
		List<AttributeHolders> attrHolders = query.list();

		for(AttributeHolders attr: attrHolders) {
			attrKeys.add(attr.getSubject());
		}

		return attrKeys;
	}

	@Override
	public boolean checkAttributeExists(AttributeDefinition attribute) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.maxResults(1)
						.having(NAME).eq(attribute.getName())
						.and().having(FRIENDLY_NAME).eq(attribute.getFriendlyName())
						.and().having(NAMESPACE).eq(attribute.getNamespace())
						.and().having(ID).eq(attribute.getId())
						.and().having(TYPE).eq(attribute.getType())
						.and().having(PRIMARY_HOLDER).isNull()
						.and().having(SECONDARY_HOLDER).isNull()
						.and().having(SUBJECT).isNull()
						.and().having(SAVED_BY_ID).eq(1)
						.toBuilder().build();

		return 1 == query.getResultSize();
	}

	@Override
	public void setAttribute(Attribute attribute, Holder holder) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();
		this.setAttributeForInit(attribute, holder);
	}

	/**
	 * Store the attribute associated with primary holder.
	 * This method is used in initialization, cache is not set as updated.
	 *
	 * @param attribute attribute to set
	 * @param holder primary holder
	 * @throws InternalErrorException
	 */
	private void setAttributeForInit(Attribute attribute, Holder holder) throws InternalErrorException {
		Cache<Object, Object> cache = this.getCacheForWrite();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), holder, null);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), holder, null);
		AttributeHolders attributeHolders = new AttributeHolders(attribute, holder, null, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attribute, holder, null, 0);

		cache.put(attrId, attributeHolders);
		cache.put(attrId1, attributeHolders1);
	}

	@Override
	public void setAttribute(Attribute attribute, Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();
		this.setAttributeForInit(attribute, primaryHolder, secondaryHolder);
	}

	/**
	 * Store the attribute associated with primary holder and secondary holder.
	 * This method is used in initialization, cache is not set as updated.
	 *
	 * @param attribute attribute to set
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 * @throws InternalErrorException
	 */
	private void setAttributeForInit(Attribute attribute, Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException {
		Cache<Object, Object> cache = this.getCacheForWrite();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), primaryHolder, secondaryHolder);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), primaryHolder, secondaryHolder);
		AttributeHolders attributeHolders = new AttributeHolders(attribute, primaryHolder, secondaryHolder, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attribute, primaryHolder, secondaryHolder, 0);

		cache.put(attrId, attributeHolders);
		cache.put(attrId1, attributeHolders1);
	}

	@Override
	public void setAttributeWithExistenceCheck(Attribute attribute, Holder holder) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();

		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), holder, null);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), holder, null);

		Attribute attr = (Attribute) cache.get(attrId);
		if(attr != null) {
			attribute.setValueCreatedAt(attr.getValueCreatedAt());
			attribute.setValueCreatedBy(attr.getValueCreatedBy());
		}

		AttributeHolders attributeHolders = new AttributeHolders(attribute, holder, null, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attribute, holder, null, 0);

		cache.put(attrId, attributeHolders);
		cache.put(attrId1, attributeHolders1);
	}

	@Override
	public void setAttributeWithExistenceCheck(Attribute attribute, Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();

		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), primaryHolder, secondaryHolder);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), primaryHolder, secondaryHolder);

		Attribute attr = (Attribute) cache.get(attrId);
		if(attr != null) {
			attribute.setValueCreatedAt(attr.getValueCreatedAt());
			attribute.setValueCreatedBy(attr.getValueCreatedBy());
		}

		AttributeHolders attributeHolders = new AttributeHolders(attribute, primaryHolder, secondaryHolder, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attribute, primaryHolder, secondaryHolder, 0);

		cache.put(attrId, attributeHolders);
		cache.put(attrId1, attributeHolders1);
	}

	/**
	 * Store attributes by primary holder, by primary and secondary holder or by key.
	 * Used only in cache initialization.
	 *
	 * @param attributes list of attribute holders to set
	 * @throws InternalErrorException
	 */
	private void setAttributes(List<AttributeHolders> attributes) throws InternalErrorException {
		for(AttributeHolders attr: attributes) {
			if(attr.getSecondaryHolder() != null && attr.getPrimaryHolder() != null) this.setAttributeForInit(attr, attr.getPrimaryHolder(), attr.getSecondaryHolder());
			else if(attr.getPrimaryHolder() != null) this.setAttributeForInit(attr, attr.getPrimaryHolder());
			else if(attr.getSubject() != null) this.setEntitylessAttributeForInit(attr, attr.getSubject());
			else throw new InternalErrorException("Attribute without holders and without subject.");
		}
	}

	@Override
	public void setAttributeDefinition(AttributeDefinition attribute) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();
		this.setAttributeDefinitionForInit(attribute);
	}

	/**
	 * Store the attribute definition.
	 * This method is used in initialization, cache is not set as updated.
	 *
	 * @param attribute attribute definition to set
	 * @throws InternalErrorException
	 */
	private void setAttributeDefinitionForInit(AttributeDefinition attribute) throws InternalErrorException {
		Cache<Object, Object> cache = this.getCacheForWrite();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId());
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName());
		AttributeHolders attributeHolders = new AttributeHolders(attribute, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attribute, 0);

		cache.put(attrId, attributeHolders);
		cache.put(attrId1, attributeHolders1);
	}

	/**
	 * Store attribute definitions.
	 * Used only in cache initialization.
	 *
	 * @param attributes list of attribute definitions to set
	 * @throws InternalErrorException
	 */
	private void setAttributesDefinitions(List<AttributeDefinition> attributes) throws InternalErrorException {
		for(AttributeDefinition attrDef: attributes) {
			this.setAttributeDefinitionForInit(attrDef);
		}
	}

	@Override
	public void setEntitylessAttribute(Attribute attribute, String key) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();
		this.setEntitylessAttributeForInit(attribute, key);
	}

	/**
	 * Store the entityless attribute by key.
	 * This method is used in initialization, cache is not set as updated.
	 *
	 * @param attribute entityless attribute
	 * @param key subject of entityless attribute
	 * @throws InternalErrorException
	 */
	private void setEntitylessAttributeForInit(Attribute attribute, String key) throws InternalErrorException {
		Cache<Object, Object> cache = this.getCacheForWrite();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), key);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), key);
		AttributeHolders attributeHolders = new AttributeHolders(attribute, key, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attribute, key, 0);

		cache.put(attrId, attributeHolders);
		cache.put(attrId1, attributeHolders1);
	}

	@Override
	public void setEntitylessAttributeWithExistenceCheck(Attribute attribute, String key) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();

		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), key);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), key);

		Attribute attr = (Attribute) cache.get(attrId);
		if(attr != null) {
			attribute.setValueCreatedAt(attr.getValueCreatedAt());
			attribute.setValueCreatedBy(attr.getValueCreatedBy());
		}

		AttributeHolders attributeHolders = new AttributeHolders(attribute, key, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attribute, key, 0);

		cache.put(attrId, attributeHolders);
		cache.put(attrId1, attributeHolders1);
	}

	//TODO deleteAttribute??? + tests
	@Override
	public void reinitializeCache(PerunSession session, AttributesManagerImpl attributesManager) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();
		this.initialize(session, attributesManager);

//		Cache<Object, Object> cache = this.getCacheForWrite();
//		QueryFactory qf = Search.getQueryFactory(cache);
//
//		org.infinispan.query.dsl.Query query =
//				qf.from(AttributeHolders.class)
//						.having(ID).eq(id)
//						.toBuilder().build();
//
//		List<AttributeHolders> attrsToDelete = query.list();
//
//		for(AttributeHolders attr: attrsToDelete) {
//			if(attr.getSubject() != null) cache.remove(new AttributeIdWithHolders(attr.getId(), attr.getSubject()));
//			else cache.remove(new AttributeIdWithHolders(attr.getId(), attr.getPrimaryHolder(), attr.getSecondaryHolder()));
//		}
	}

	@Override
	public void removeAttribute(AttributeDefinition attribute, Holder holder) {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), holder, null);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), holder, null);
		cache.remove(attrId);
		cache.remove(attrId1);
	}

	@Override
	public void removeAttribute(AttributeDefinition attribute, Holder primaryHolder, Holder secondaryHolder) {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), primaryHolder, secondaryHolder);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), primaryHolder, secondaryHolder);
		cache.remove(attrId);
		cache.remove(attrId1);
	}

	@Override
	public void removeEntitylessAttribute(AttributeDefinition attribute, String key) {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();
		AttributeIdWithHolders attrId = new AttributeIdWithHolders(attribute.getId(), key);
		AttributeIdWithHolders attrId1 = new AttributeIdWithHolders(attribute.getName(), key);
		cache.remove(attrId);
		cache.remove(attrId1);
	}

	//TODO if cache was updated in transaction, reinitialize??
	public void removeAllAttributes(Holder holder) {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();
		QueryFactory qf = Search.getQueryFactory(cache);

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(holder.getId())
						.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(holder.getType())
						.and().having(SECONDARY_HOLDER).isNull()
						.toBuilder().build();

		List<AttributeHolders> attrsToDelete = query.list();

		for(AttributeHolders attr: attrsToDelete) {
			cache.remove(new AttributeIdWithHolders(attr.getId(), attr.getPrimaryHolder(), null));
		}
	}

	public void removeAllAttributes(Holder primaryHolder, Holder secondaryHolder) {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();
		QueryFactory qf = Search.getQueryFactory(cache);

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(primaryHolder.getId())
						.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(primaryHolder.getType())
						.and().having(SECONDARY_HOLDER + "." + HOLDER_ID).eq(secondaryHolder.getId())
						.and().having(SECONDARY_HOLDER + "." + HOLDER_TYPE).eq(secondaryHolder.getType())
						.toBuilder().build();

		List<AttributeHolders> attrsToDelete = query.list();

		for(AttributeHolders attr: attrsToDelete) {
			cache.remove(new AttributeIdWithHolders(attr.getId(), attr.getPrimaryHolder(), attr.getSecondaryHolder()));
		}
	}

	public void removeAllAttributesByPrimaryHolder(Holder primaryHolder, Holder.HolderType secondaryHolderType) {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();
		QueryFactory qf = Search.getQueryFactory(cache);

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER + "." + HOLDER_ID).eq(primaryHolder.getId())
						.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(primaryHolder.getType())
						.and().having(SECONDARY_HOLDER + "." + HOLDER_TYPE).eq(secondaryHolderType)
						.toBuilder().build();

		List<AttributeHolders> attrsToDelete = query.list();

		for(AttributeHolders attr: attrsToDelete) {
			cache.remove(new AttributeIdWithHolders(attr.getId(), attr.getPrimaryHolder(), attr.getSecondaryHolder()));
		}
	}

	public void removeAllAttributesBySecondaryHolder(Holder.HolderType primaryHolderType, Holder secondaryHolder) {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();
		QueryFactory qf = Search.getQueryFactory(cache);

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(primaryHolderType)
						.and().having(SECONDARY_HOLDER + "." + HOLDER_ID).eq(secondaryHolder.getId())
						.and().having(SECONDARY_HOLDER + "." + HOLDER_TYPE).eq(secondaryHolder.getType())
						.toBuilder().build();

		List<AttributeHolders> attrsToDelete = query.list();

		for(AttributeHolders attr: attrsToDelete) {
			cache.remove(new AttributeIdWithHolders(attr.getId(), attr.getPrimaryHolder(), attr.getSecondaryHolder()));
		}
	}

	@Override
	public void updateAttributeDefinition(AttributeDefinition attributeDefinition) throws InternalErrorException {
		this.setCacheUpdatedInTransaction();

		Cache<Object, Object> cache = this.getCacheForWrite();

		AttributeIdWithHolders id = new AttributeIdWithHolders(attributeDefinition.getId());
		AttributeIdWithHolders id1 = new AttributeIdWithHolders(attributeDefinition.getName());

		Attribute attr = (Attribute) cache.get(id);
		attributeDefinition.setCreatedBy(attr.getCreatedBy());
		attributeDefinition.setCreatedByUid(attr.getCreatedByUid());
		attributeDefinition.setCreatedAt(attr.getCreatedAt());

		AttributeHolders attributeHolders = new AttributeHolders(attributeDefinition, 1);
		AttributeHolders attributeHolders1 = new AttributeHolders(attributeDefinition, 0);
		cache.put(id, attributeHolders);
		cache.put(id1, attributeHolders1);
	}

	//TODO this is not used?
	//getAllResourceValues
	//		if(!CacheManager.isCacheDisabled()) return perun.getCacheManager().getAllValues(Holder.HolderType.RESOURCE, attributeDefinition);
	//TODO is this effective??
//	public List<Object> getAllValues(Holder.HolderType holderType, AttributeDefinition attributeDefinition) {
//		QueryFactory qf = Search.getQueryFactory(this.getCacheForWrite());
//
//		org.infinispan.query.dsl.Query query =
//				qf.from(AttributeHolders.class)
//						.having(ID).eq(attributeDefinition.getId())
//						.and().having(PRIMARY_HOLDER + "." + HOLDER_TYPE).eq(holderType)
//						.and().having(SECONDARY_HOLDER).isNull()
//						.toBuilder().build();
//
//		List<Attribute> attrs = query.list();
//		List<Object> values = new ArrayList<>();
//
//		for(Attribute a: attrs) {
////			values.add(BeansUtils.stringToAttributeValue(a.getValue(), attributeDefinition.getType());)
//			values.add(a.getValue());
//		}
//
//		return values;
//	}

	public void commit() {
		try {
			this.getCacheTransactionManager().commit();
		} catch (RollbackException e) {
			throw new UnexpectedRollbackException("Transaction has been unexpectedly rolled back instead of committed.", e);
		} catch (HeuristicMixedException | HeuristicRollbackException | SystemException e) {
			throw new TransactionSystemException("Unexpected system error occurred.", e);
		}
	}

	public void rollback() {
		try {
			this.getCacheTransactionManager().rollback();
		} catch (SystemException e) {
			throw new TransactionSystemException("Unexpected system error occurred.", e);
		}
	}

	public void newTopLevelTransaction() {
		// list of cache names used with the current transaction
		// first string in the list is set to null if no write to cache was performed. If a write was performed than its set to "w"
		List<String> cacheNames = getCacheNamesFromTransaction();

		if (cacheNames == null) {
			cacheNames = new ArrayList<>();
			cacheNames.add(null);
			if(cacheTest) cacheNames.add(TEST_CACHE_NAME);
			else cacheNames.add(CACHE_NAME);

			TransactionSynchronizationManager.bindResource(this, cacheNames);
		} else {
			if(cacheTest) cacheNames.add(TEST_CACHE_NAME);
			else cacheNames.add(CACHE_NAME);
		}

		try {
			this.getCacheTransactionManager().begin();
		} catch (NotSupportedException e) {
			throw new CannotCreateTransactionException("The thread is already associated with a transaction.", e);
		} catch (SystemException e) {
			throw new TransactionSystemException("Unexpected system error occurred.", e);
		}
	}

	public void newNestedTransaction() {
		if(usedCacheNames == null) usedCacheNames = Collections.synchronizedList(new ArrayList<Boolean>());

		int cacheNumber = -1;
		String cacheName;

		//choosing first unused cache
		for (int i = 0; i < usedCacheNames.size(); i++) {
			if(!usedCacheNames.get(i)) cacheNumber = i;
		}

		//if unused cache is not found, we need to create new cache
		if(cacheNumber == -1) {
			cacheName = SIMPLE_CACHE_NAME + usedCacheNames.size();
			Configuration configuration = localCacheManager.getCacheConfiguration(SIMPLE_CACHE_NAME);
			localCacheManager.defineConfiguration(cacheName, configuration);
			usedCacheNames.add(Boolean.TRUE);
		} else {
			//if unused cache is found, we set it as used and use it
			cacheName = SIMPLE_CACHE_NAME + cacheNumber;
			usedCacheNames.set(cacheNumber, Boolean.TRUE);
		}

		List<String> transactionCacheNames = getCacheNamesFromTransaction();
		transactionCacheNames.add(cacheName);
	}

	public void cleanNestedTransaction() {
		List<String> transactionCacheNames = getCacheNamesFromTransaction();
		int transactionCacheToClean = transactionCacheNames.size() - 1;
		//clear the cache used in nested transaction
		this.getCache(transactionCacheNames.get(transactionCacheToClean)).clear();
		//remove the cache name from list of caches used in transaction
		transactionCacheNames.remove(transactionCacheToClean);
		//set cache as unused
		usedCacheNames.set(transactionCacheToClean - 2, Boolean.FALSE);
	}

	public void flushNestedTransaction() {
		List<String> transactionCacheNames = getCacheNamesFromTransaction();

		//get the cache to be flushed
		Cache<Object, Object> cacheToFlush = this.getCache(transactionCacheNames.get(transactionCacheNames.size() - 1));
		Cache<Object, Object> cache = this.getCache(transactionCacheNames.get(transactionCacheNames.size() - 2));

		for (Object o: cacheToFlush.keySet()) {
			//TODO check if this works
			cache.put((AttributeIdWithHolders) o, (AttributeHolders) cacheToFlush.get(o));
		}

		cleanNestedTransaction();
	}

	public void clean() {
		List<String> transactionCacheNames = getCacheNamesFromTransaction();
		transactionCacheNames.clear();
		TransactionSynchronizationManager.unbindResourceIfPossible(this);
	}

	public void initialize(PerunSession sess, AttributesManagerImpl attributesManagerImpl) throws InternalErrorException {
		Cache<Object, Object> cache = this.getCache();

		CacheSet<Object> keySet = cache.keySet();
		for(Object key: keySet) {
			cache.remove(key);
		}

		List<AttributeDefinition> attrDefs;
		List<AttributeHolders> attrs;

		//save attribute definitions
		attrDefs = jdbc.query("select " + AttributesManagerImpl.attributeDefinitionMappingSelectQuery + " from attr_names ", AttributesManagerImpl.ATTRIBUTE_DEFINITION_MAPPER);
		this.setAttributesDefinitions(attrDefs);

		//save attributes with value
		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("facility_attr_values") + ", facility_id as primary_holder_id from attr_names " +
				"join facility_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.FACILITY, null));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("vo_attr_values") + ", vo_id as primary_holder_id from attr_names " +
				"join vo_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.VO, null));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("group_attr_values") + ", group_id as primary_holder_id from attr_names " +
				"join group_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.GROUP, null));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("host_attr_values") + ", host_id as primary_holder_id from attr_names " +
				"join host_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.HOST, null));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("resource_attr_values") + ", resource_id as primary_holder_id from attr_names " +
				"join resource_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.RESOURCE, null));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("member_attr_values") + ", member_id as primary_holder_id from attr_names " +
				"join member_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.MEMBER, null));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("user_attr_values") + ", user_id as primary_holder_id from attr_names " +
				"join user_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.USER, null));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("member_resource_attr_values") + ", member_id as primary_holder_id, resource_id as secondary_holder_id from attr_names " +
				"join member_resource_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.MEMBER, Holder.HolderType.RESOURCE));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("member_group_attr_values") + ", member_id as primary_holder_id, group_id as secondary_holder_id from attr_names " +
				"join member_group_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.MEMBER, Holder.HolderType.GROUP));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("user_facility_attr_values") + ", user_id as primary_holder_id, facility_id as secondary_holder_id from attr_names " +
				"join user_facility_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.USER, Holder.HolderType.FACILITY));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("group_resource_attr_values") + ", group_id as primary_holder_id, resource_id as secondary_holder_id from attr_names " +
				"join group_resource_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, Holder.HolderType.GROUP, Holder.HolderType.RESOURCE));
		this.setAttributes(attrs);

		attrs = jdbc.query("select " + AttributesManagerImpl.getAttributeMappingSelectQuery("entityless_attr_values") + ", subject from attr_names " +
				"join entityless_attr_values on id=attr_id where (attr_value is not null or attr_value_text is not null)",
				new AttributesManagerImpl.AttributeHoldersRowMapper(sess, attributesManagerImpl, null, null));
		this.setAttributes(attrs);

		String cacheName;
		if(isCacheTest()) cacheName = TEST_CACHE_NAME;
		else cacheName = CACHE_NAME;

		System.out.println("Initialization of " + cacheName + " finished");
	}
}
