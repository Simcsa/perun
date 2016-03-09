package cz.metacentrum.perun.core.api;

import org.infinispan.query.Transformable;

import java.io.Serializable;

/**
 * Class used as a key for infinispan database entries. Uniquely identifies Attribute and also who does it belong to (holders).
 *
 * @author Simona Kruppova
 */
@Transformable(transformer = AttributeIdWithHoldersTransformer.class)
public class AttributeIdWithHolders implements Serializable {

	private int attributeId;
	private Holder primaryHolder;
	private Holder secondaryHolder;

	public AttributeIdWithHolders(int attributeId, Holder primaryHolder, Holder secondaryHolder) {
		this.attributeId = attributeId;
		this.primaryHolder = primaryHolder;
		this.secondaryHolder = secondaryHolder;
	}

	public int getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(int attributeId) {
		this.attributeId = attributeId;
	}

	public Holder getSecondaryHolder() {
		return secondaryHolder;
	}

	public void setSecondaryHolder(Holder secondaryHolder) {
		this.secondaryHolder = secondaryHolder;
	}

	public Holder getPrimaryHolder() {
		return primaryHolder;
	}

	public void setPrimaryHolder(Holder primaryHolder) {
		this.primaryHolder = primaryHolder;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AttributeIdWithHolders that = (AttributeIdWithHolders) o;

		if (attributeId != that.attributeId) return false;
		if (!primaryHolder.equals(that.primaryHolder)) return false;
		if (secondaryHolder != null ? !secondaryHolder.equals(that.secondaryHolder) : that.secondaryHolder != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = attributeId;
		result = 31 * result + primaryHolder.hashCode();
		result = 31 * result + (secondaryHolder != null ? secondaryHolder.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("AttributeIdWithHolders{");
		sb.append("attributeId=").append(attributeId);
		sb.append(", primaryHolder=").append(primaryHolder);
		sb.append(", secondaryHolder=").append(secondaryHolder);
		sb.append('}');
		return sb.toString();
	}
}