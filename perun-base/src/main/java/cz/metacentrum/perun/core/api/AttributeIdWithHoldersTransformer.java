package cz.metacentrum.perun.core.api;

import org.infinispan.query.Transformer;

/**
 * Transformer for AtributeIdWithHolders class.
 * AttributeIdWithHolders is used as a key for entries in Infinispan database and as such needs transformer from and to string.
 *
 * @author Simona Kruppova
 */
public class AttributeIdWithHoldersTransformer implements Transformer {

	public Object fromString(String s) {
		String[] ids = s.split(":");
		String[] primHolder = ids[1].split(",");
		Holder primaryHolder = new Holder(Integer.parseInt(primHolder[0]), Holder.HolderType.valueOf(primHolder[1]));
		Holder secondaryHolder = null;

		if(ids.length > 2) {
			String[] secHolder = ids[2].split(",");
			secondaryHolder = new Holder(Integer.parseInt(secHolder[0]), Holder.HolderType.valueOf(secHolder[1]));
		}

		return new AttributeIdWithHolders(Integer.parseInt(ids[0]), primaryHolder, secondaryHolder);
	}

	public String toString(Object attributeType) {
		AttributeIdWithHolders attr = (AttributeIdWithHolders) attributeType;
		StringBuilder str = new StringBuilder();

		str.append(attr.getAttributeId()).append(":")
				.append(attr.getPrimaryHolder().getHolderId()).append(",").append(attr.getPrimaryHolder().getHolderType());

		if(attr.getSecondaryHolder() != null) {
			str.append(":").append(attr.getSecondaryHolder().getHolderId()).append(",").append(attr.getSecondaryHolder().getHolderType());
		}

		return str.toString();
	}
}

