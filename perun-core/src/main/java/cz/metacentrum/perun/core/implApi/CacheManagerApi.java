package cz.metacentrum.perun.core.implApi;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.Holder;
import cz.metacentrum.perun.core.api.PerunSession;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.impl.AttributesManagerImpl;

import java.util.List;

/**
 * Class used for caching layer management. It deals also with nested transactions and it contains all search and update methods for the cache.
 *
 * @author Simona Kruppova
 */
public interface CacheManagerApi {

	/**
	 * Clears test cache.
	 */
	void clearTestCache();

	/**
	 * Returns true if cache was updated in transaction.
	 *
	 * @return true if cache was updated in transaction, false if it was not updated or if there is no transaction
	 */
	boolean wasCacheUpdatedInTransaction();

	/**
	 * Gets all <b>non-empty</b> attributes associated with the primary holder.
	 * Gets only non-virtual attributes.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param holder primary holder
	 * @return list of attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getAllNonEmptyAttributes(Holder holder) throws InternalErrorException;

	/**
	 * Gets all <b>non-empty</b> attributes associated with the primary holder and secondary holder.
	 * Gets only non-virtual, non-core attributes.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 * @return list of attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getAllNonEmptyAttributes(Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException;

	/**
	 * Gets all attributes associated with the primary holder. Name of attributes starts with startPartOfName.
	 * Gets only non-virtual attributes.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param startPartOfName attribute name starts with this part
	 * @param holder primary holder
	 * @return list of attributes whose name starts with startPartOfName
	 * @throws InternalErrorException
	 */
	List<Attribute> getAllAttributesByStartPartOfName(String startPartOfName, Holder holder) throws InternalErrorException;

	/**
	 * Gets all <b>non-empty</b> attributes associated with any user on the facility.
	 * Gets only non-virtual, non-core attributes.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param facilityId facility id
	 * @return list of attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getUserFacilityAttributesForAnyUser(int facilityId) throws InternalErrorException;

	/**
	 * Gets all attributes associated with the primary holder which have name in list attrNames (empty and virtual too).
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param attrNames list of attributes names
	 * @param holder primary holder
	 * @return list of attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getAttributesByNames(List<String> attrNames, Holder holder) throws InternalErrorException;

	/**
	 * Gets all attributes associated with the primary holder and secondary holder which have name in list attrNames (empty and virtual too).
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param attrNames list of attributes names
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 * @return list of attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getAttributesByNames(List<String> attrNames, Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException;

	/**
	 * Gets all virtual attributes associated with the primary holder.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param holderType primary holder type
	 * @return list of attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getVirtualAttributes(Holder.HolderType holderType) throws InternalErrorException;

	/**
	 * Gets all virtual attributes associated with the primary holder and secondary holder.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param primaryHolderType primary holder type
	 * @param secondaryHolderType secondary holder type
	 * @return list of attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getVirtualAttributes(Holder.HolderType primaryHolderType, Holder.HolderType secondaryHolderType) throws InternalErrorException;

	/**
	 * Gets particular attribute for the primary holder by name. If such attribute does not exist, it returns attribute definition.
	 *
	 * @param attrName attribute name defined in the particular manager
	 * @param holder primary holder
	 * @return attribute
	 * @throws AttributeNotExistsException if the attribute doesn't exists in the underlying data source
	 */
	Attribute getAttributeByName(String attrName, Holder holder) throws AttributeNotExistsException;

	/**
	 * Gets particular attribute for the primary holder and secondary holder by name. If such attribute does not exist, it returns attribute definition.
	 *
	 * @param attrName attribute name defined in the particular manager
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 * @return attribute
	 * @throws AttributeNotExistsException if the attribute doesn't exists in the underlying data source
	 */
	Attribute getAttributeByName(String attrName, Holder primaryHolder, Holder secondaryHolder) throws AttributeNotExistsException;

	/**
	 * Gets attribute by id and primary holder. If such attribute does not exist, it returns attribute definition.
	 *
	 * @param id id of attribute to get
	 * @param holder primary holder
	 * @return attribute
	 * @throws AttributeNotExistsException if the attribute doesn't exists in the underlying data source
	 */
	Attribute getAttributeById(int id, Holder holder) throws AttributeNotExistsException;

	/**
	 * Gets attribute by id, primary holder and secondary holder. If such attribute does not exist, it returns attribute definition.
	 *
	 * @param id id of attribute to get
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 * @return attribute
	 * @throws AttributeNotExistsException if the attribute doesn't exists in the underlying data source
	 */
	Attribute getAttributeById(int id, Holder primaryHolder, Holder secondaryHolder) throws AttributeNotExistsException;

	/**
	 * Gets attributes definitions (attribute without defined value).
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @return list of attributes definitions
	 */
	List<AttributeDefinition> getAttributesDefinitions();

	/**
	 * Gets attributes definition (attribute without defined value) with specified namespace.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param namespace get only attributes with this namespace
	 * @return list of attributes definitions
	 */
	List<AttributeDefinition> getAttributesDefinitionsByNamespace(String namespace);

	/**
	 * Gets attribute definition (attribute without defined value).
	 *
	 * @param attrName attribute name defined in the particular manager
	 * @return attribute definition
	 * @throws AttributeNotExistsException if the attribute doesn't exists in the underlying data source
	 */
	AttributeDefinition getAttributeDefinition(String attrName) throws AttributeNotExistsException;

	/**
	 * Gets attribute definition by id (attribute without defined value).
	 *
	 * @param id id of attribute definition to get
	 * @return attribute definition
	 * @throws AttributeNotExistsException if the attribute doesn't exists in the underlying data source
	 */
	AttributeDefinition getAttributeDefinitionById(int id) throws AttributeNotExistsException;

	/**
	 * Gets all <b>non-empty</b> entityless attributes where subject equals key.
	 * Gets only non-virtual, non-core attributes.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param key subject of entityless attribute
	 * @return list of entityless attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getAllNonEmptyEntitylessAttributes(String key) throws InternalErrorException;

	/**
	 * Gets all <b>non-empty</b> entityless attributes by attrName.
	 * Gets only non-virtual, non-core attributes.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param attrName attribute name defined in the particular manager
	 * @return list of entityless attributes
	 * @throws InternalErrorException
	 */
	List<Attribute> getAllNonEmptyEntitylessAttributesByName(String attrName) throws InternalErrorException;

	/**
	 * Gets particular entityless attribute by name and key. If such attribute does not exist, it returns attribute definition.
	 *
	 * @param key subject of entityless attribute
	 * @param attrName attribute name defined in the particular manager
	 * @return entityless attribute
	 * @throws AttributeNotExistsException if the attribute doesn't exists in the underlying data source
	 */
	Attribute getEntitylessAttributeByNameAndKey(String key, String attrName) throws AttributeNotExistsException;

	/**
	 * Gets particular entityless attribute value by key and id. If such attribute does not exist, it returns null.
	 *
	 * @param attrId id of attribute
	 * @param key subject of entityless attribute
	 * @return attribute value in String
	 * @throws InternalErrorException
	 */
	String getEntitylessAttrValue(int attrId, String key) throws InternalErrorException;

	/**
	 * Gets list of keys of entityless attributes by attrName.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param attrName attribute name defined in the particular manager
	 * @return list of keys (subjects) of entityless attributes
	 */
	List<String> getEntitylessAttrKeys(String attrName);

	/**
	 * Check if attribute exists in underlying data source.
	 * Should be used only if wasCacheUpdatedInTransaction is false, else may return incorrect results.
	 *
	 * @param attribute attribute to check
	 * @return true if attribute exists in underlying data source, false otherwise
	 */
	boolean checkAttributeExists(AttributeDefinition attribute);

	/**
	 * Stores the attribute associated with primary holder.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute to set
	 * @param holder primary holder
	 * @throws InternalErrorException
	 */
	void setAttribute(Attribute attribute, Holder holder) throws InternalErrorException;

	/**
	 * Stores the attribute associated with primary holder and secondary holder.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute to set
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 * @throws InternalErrorException
	 */
	void setAttribute(Attribute attribute, Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException;

	/**
	 * Stores the attribute associated with primary holder.
	 * It checks whether the attribute already exists. If it does, it does not update createdBy and createdAt values.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute to set
	 * @param holder primary holder
	 * @throws InternalErrorException
	 */
	void setAttributeWithExistenceCheck(Attribute attribute, Holder holder) throws InternalErrorException;

	/**
	 * Stores the attribute associated with primary holder and secondary holder.
	 * It checks whether the attribute already exists. If it does, it does not update createdBy and createdAt values.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute to set
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 * @throws InternalErrorException
	 */
	void setAttributeWithExistenceCheck(Attribute attribute, Holder primaryHolder, Holder secondaryHolder) throws InternalErrorException;

	/**
	 * Stores the attribute definition.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute definition to set
	 * @throws InternalErrorException
	 */
	void setAttributeDefinition(AttributeDefinition attribute) throws InternalErrorException;

	/**
	 * Stores the entityless attribute.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute entityless attribute to set
	 * @param key subject of entityless attribute
	 * @throws InternalErrorException
	 */
	void setEntitylessAttribute(Attribute attribute, String key) throws InternalErrorException;

	/**
	 * Stores the entityless attribute.
	 * It checks whether the attribute already exists. If it does, it does not update createdBy and createdAt values.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute entityless attribute to set
	 * @param key subject of entityless attribute
	 * @throws InternalErrorException
	 */
	void setEntitylessAttributeWithExistenceCheck(Attribute attribute, String key) throws InternalErrorException;

	/**
	 * Reinitialize cache. It clears it and fills it with contents of underlying relational database.
	 * Used when deleting attribute - definition and all values. There is no other consistent way to delete the attribute from cache.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param session perun session
	 * @param attributesManager attributes manager
	 * @throws InternalErrorException
	 */
	void reinitializeCache(PerunSession session, AttributesManagerImpl attributesManager) throws InternalErrorException;

	/**
	 * Unset particular attribute for the primary holder.
	 * Should be used only when there is attribute to remove.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute to remove
	 * @param holder primary holder
	 */
	void removeAttribute(AttributeDefinition attribute, Holder holder);

	/**
	 * Unset particular attribute for holders.
	 * Should be used only when there is attribute to remove.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute to remove
	 * @param primaryHolder primary holder
	 * @param secondaryHolder secondary holder
	 */
	void removeAttribute(AttributeDefinition attribute, Holder primaryHolder, Holder secondaryHolder);

	/**
	 * Unset particular entityless attribute with subject equals key.
	 * Should be used only when there is attribute to remove.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attribute attribute to remove
	 * @param key subject of entityless attribute
	 */
	void removeEntitylessAttribute(AttributeDefinition attribute, String key);

	/**
	 * Updates attribute definition. It does not update createdBy, createdByUid and createdAt value.
	 * If there is transaction, it sets cache as updated in transaction.
	 *
	 * @param attributeDefinition attribute definition to update
	 * @throws InternalErrorException
	 */
	void updateAttributeDefinition(AttributeDefinition attributeDefinition) throws InternalErrorException;
}