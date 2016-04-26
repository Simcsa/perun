package cz.metacentrum.perun.core.api;

import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import org.hibernate.search.annotations.*;

import java.io.Serializable;

/**
 * Class used as a value for Infinispan database entries. It contains Attribute and its Holders (member, group..).
 *
 * @author Simona Kruppova
 */
@Indexed
public class AttributeHolders extends Attribute implements Serializable {

	@IndexedEmbedded(indexNullAs = Field.DEFAULT_NULL_TOKEN)
	private Holder primaryHolder;
	@IndexedEmbedded(indexNullAs = Field.DEFAULT_NULL_TOKEN)
	private Holder secondaryHolder;

	/**
	 * Subject of the attribute, used only with entityless attributes
	 */
	@Field(analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
	private String subject;

	@Field
	@NumericField
	private int idForSearch;

	/**
	 * Whole attribute name including namespace (attribute namespace + friendly name)
	 * Needed for cache search.
	 */
	@Field(analyze = Analyze.NO)
	private String nameForSearch;

	/**
	 * Attribute namespace, including the whole namespace.
	 * Needed for cache search.
	 */
	@Field(analyze = Analyze.NO)
	private String namespaceForSearch;

	/**
	 * Attribute name, <strong>excluding</strong> the whole namespace.
	 * Needed for cache search.
	 */
	@Field(analyze = Analyze.NO)
	private String friendlyNameForSearch;

	/**
	 * Type of attribute's value. It's a name of java class. "Java.lang.String" for expample. (To get this use something like <em>String.class.getName()</em>)
	 * Needed for cache search.
	 */
	@Field(analyze = Analyze.NO)
	private String typeForSearch;

	/**
	 * Value of the attribute, can be Map, List, String, Integer, Boolean...
	 * Needed for cache search. It is used only for checking whether it is null or not. Converted to string in constructor.
	 */
	@Field(analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
	private String valueForSearch;

	/**
	 * Specifies if this AttributeHolders entity was saved by id or by name in cache. 1 if by id, 0 if by name.
	 */
	@Field
	@NumericField
	private int savedById;

	public AttributeHolders(Attribute attribute, Holder primaryHolder, Holder secondaryHolder, int savedById) throws InternalErrorException {
		super(attribute, true);
		this.primaryHolder = primaryHolder;
		this.secondaryHolder = secondaryHolder;
		this.nameForSearch = attribute.getNamespace() + ":" + attribute.getFriendlyName();
		this.namespaceForSearch = attribute.getNamespace();
		this.friendlyNameForSearch = attribute.getFriendlyName();
		this.typeForSearch = attribute.getType();
		this.idForSearch = attribute.getId();
		this.valueForSearch = BeansUtils.attributeValueToString(attribute);
		this.savedById = savedById;
	}

	public AttributeHolders(AttributeDefinition attribute, int savedById) {
		super(attribute);
		this.nameForSearch = attribute.getNamespace() + ":" + attribute.getFriendlyName();
		this.namespaceForSearch = attribute.getNamespace();
		this.friendlyNameForSearch = attribute.getFriendlyName();
		this.typeForSearch = attribute.getType();
		this.idForSearch = attribute.getId();
		this.valueForSearch = null;
		this.savedById = savedById;
	}

	public AttributeHolders(Attribute attribute, String subject, int savedById) throws InternalErrorException {
		this(attribute, null, null, savedById);
		this.subject = subject;

	}

	public Holder getPrimaryHolder() {
		return primaryHolder;
	}

	public void setPrimaryHolder(Holder primaryHolder) {
		this.primaryHolder = primaryHolder;
	}

	public Holder getSecondaryHolder() {
		return secondaryHolder;
	}

	public void setSecondaryHolder(Holder secondaryHolder) {
		this.secondaryHolder = secondaryHolder;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	public String toString() {
		return "AttributeHolders{" +
				"attribute=" + super.toString() +
				", nameForSearch=" + nameForSearch +
				", primaryHolder=" + primaryHolder +
				", secondaryHolder=" + secondaryHolder +
				'}';
	}
}
