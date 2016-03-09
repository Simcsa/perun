package cz.metacentrum.perun.core.api;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;

import java.io.Serializable;

/**
 * Holder of an attribute. Represents who does the attribute belong to. Holder is identified by id and type (member, group..)
 *
 * @author Simona Kruppova
 */
@Indexed
public class Holder implements Serializable {
	@Field
	@NumericField
	private int holderId;

	@Field
	private HolderType holderType;

	public enum HolderType {
		FACILITY, MEMBER, VO, GROUP, HOST, RESOURCE, USER
	};

	public Holder(int holderId, HolderType holderType) {
		this.holderId = holderId;
		this.holderType = holderType;
	}

	public int getHolderId() {
		return holderId;
	}

	public void setHolderId(int holderId) {
		this.holderId = holderId;
	}

	public HolderType getHolderType() {
		return holderType;
	}

	public void setHolderType(HolderType holderType) {
		this.holderType = holderType;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("Holder{");
		sb.append("holderId=").append(holderId);
		sb.append(", holderType=").append(holderType);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Holder holder = (Holder) o;

		if (holderId != holder.holderId) return false;
		if (holderType != holder.holderType) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = holderId;
		result = 31 * result + holderType.hashCode();
		return result;
	}
}
