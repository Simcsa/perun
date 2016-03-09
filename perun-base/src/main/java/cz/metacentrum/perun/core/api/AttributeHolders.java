package cz.metacentrum.perun.core.api;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import java.io.Serializable;

/**
 * Class used as a value for Infinispan database entries. It contains Attribute and its Holders (member, group..).
 *
 * @author Simona Kruppova
 */
@Indexed
public class AttributeHolders extends Attribute implements Serializable {

	@IndexedEmbedded
	private Holder primaryHolder;
	@IndexedEmbedded
	private Holder secondaryHolder;

	/**
	 * Whole attribute name including namespace (attribute namespace + friendly name)
	 * Needed for cache search.
	 */
	@Field
	private String nameForSearch;

	/**
	 * Attribute namespace, including the whole namespace.
	 * Needed for cache search.
	 */
	@Field
	private String namespaceForSearch;

	/**
	 * Value of the attribute, can be Map, List, String, Integer, Boolean...
	 * Needed for cache search. It is used only for checking whether it is null or not - it's converted to String in constructor
	 */
	private String valueForSearch;

	public AttributeHolders(Attribute attribute, Holder primaryHolder, Holder secondaryHolder) {
		super(attribute);
		this.primaryHolder = primaryHolder;
		this.secondaryHolder = secondaryHolder;
		this.nameForSearch = attribute.getNamespace() + ":" + attribute.getFriendlyName();
		this.namespaceForSearch = attribute.getNamespace();
		if(attribute.getValue() == null) {
			this.valueForSearch = null;
		}
		this.valueForSearch = attribute.getValue().toString();
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

	@Override
	public String toString() {
		return "AttributeHolders{" +
				"attribute=" + super.toString() +
				"primaryHolder=" + primaryHolder +
				", secondaryHolder=" + secondaryHolder +
				'}';
	}
}
