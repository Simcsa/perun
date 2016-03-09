package cz.metacentrum.perun.core.impl;

import cz.metacentrum.perun.core.api.*;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.dsl.QueryFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class used for caching layer management. It deals also with nested transactions and it contains all search methods for the cache.
 *
 * @author Simona Kruppova
 */
public class CacheManager {

	@Autowired
	private EmbeddedCacheManager localCacheManager;

	private static final String CACHE_NAME = "transactionalCache";
	private static final String SIMPLE_CACHE_NAME = "simpleCache";

	private static final String DELIMITER = ".";
	private static final String PRIMARY_HOLDER = "primaryHolder";
	private static final String SECONDARY_HOLDER = "secondaryHolder";
	private static final String HOLDER_ID = "holderId";
	private static final String HOLDER_TYPE = "holderType";
	private static final String NAMESPACE = "namespaceForSearch";

	//position of the Boolean in the list specifies a cache - if that cache is used true, else false
	//for example if list has Boolean.True element at position 2, that means simpleCache2 is currently used
	private static List<Boolean> usedCacheNames;

	public Cache<AttributeIdWithHolders, AttributeHolders> getCache() {
		return localCacheManager.getCache(CACHE_NAME);
	}

	public TransactionManager getCacheTransactionManager() {
		return getCache().getAdvancedCache().getTransactionManager();
	}

	public SearchManager getSearchManager() { // will not need probably
		return Search.getSearchManager(getCache());
	}

	public List<Attribute> getAllNonEmptyAttributesByPrimaryHolder(int holderId, Holder.HolderType holderType) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having(PRIMARY_HOLDER + DELIMITER + HOLDER_ID).eq(holderId)
						.and().having(PRIMARY_HOLDER + DELIMITER + HOLDER_TYPE).eq(holderType)
						.and().having(NAMESPACE).in(AttributesManager.NS_FACILITY_ATTR_CORE, AttributesManager.NS_FACILITY_ATTR_DEF, AttributesManager.NS_FACILITY_ATTR_OPT)
						.toBuilder().build();

		return query.list();
	}

	public List<Attribute> getVirtualAttributesByPrimaryHolder(int primaryHolderId, Holder.HolderType primaryholderType) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having("primaryHolder.holderId").eq(primaryHolderId)
						.and().having("primaryHolder.holderType").eq(primaryholderType)
						.toBuilder().build();

		return query.list();
	}

	public List<Attribute> getVirtualAttributesByHolders(int primaryHolderId, Holder.HolderType primaryholderType, int secondaryHolderId, Holder.HolderType secondaryHolderType) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having("primaryHolder.holderId").eq(primaryHolderId)
						.and().having("primaryHolder.holderType").eq(primaryholderType)
						.and().having("primaryHolder.holderId").eq(secondaryHolderId)
						.and().having("primaryHolder.holderType").eq(secondaryHolderType)
						.toBuilder().build();

		return query.list();
	}

	public List<Attribute> getAllNonEmptyAttributesByHolders(int primaryHolderId, Holder.HolderType primaryholderType, int secondaryHolderId, Holder.HolderType secondaryHolderType) {
		QueryFactory qf = Search.getQueryFactory(this.getCache());

		org.infinispan.query.dsl.Query query =
				qf.from(AttributeHolders.class)
						.having("primaryHolder.holderId").eq(primaryHolderId)
						.and().having("primaryHolder.holderType").eq(primaryholderType)
						.and().having("primaryHolder.holderId").eq(secondaryHolderId)
						.and().having("primaryHolder.holderType").eq(secondaryHolderType)
						.toBuilder().build();

		return query.list();
	}

	public void newTopLevelTransaction() {
		try {
			this.getCacheTransactionManager().begin();
		} catch (NotSupportedException | SystemException e) {
			e.printStackTrace();
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
			cacheName = SIMPLE_CACHE_NAME + cacheNumber;
			usedCacheNames.set(cacheNumber, Boolean.TRUE);
		}

		localCacheManager.getCache(cacheName);

	}
}
