package cz.metacentrum.perun.core.api;

import java.util.List;

/**
 * Candidate with information about membership in vo and group.
 *
 * @author Simona Kruppova, Oliver Mrazik
 */
public class MemberCandidate {

	private Member member;
	private UserExtSource userExtSource;
	private List<UserExtSource> userExtSources;
	private List<Attribute> attributes;
	private Boolean isMemberOfGroup;

	public MemberCandidate(UserExtSource userExtSource, List<UserExtSource> userExtSources, List<Attribute> attributes) {
		this(null, userExtSource, userExtSources, attributes, false);
	}

	public MemberCandidate(Member member, UserExtSource userExtSource, List<UserExtSource> userExtSources, List<Attribute> attributes) {
		this(member, userExtSource, userExtSources, attributes, true);
	}

	public MemberCandidate(Member member, UserExtSource userExtSource, List<UserExtSource> userExtSources, List<Attribute> attributes, Boolean isMemberOfGroup) {
		this.member = member;
		this.userExtSource = userExtSource;
		this.userExtSources = userExtSources;
		this.attributes = attributes;
		this.isMemberOfGroup = isMemberOfGroup;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public UserExtSource getUserExtSource() {
		return userExtSource;
	}

	public void setUserExtSource(UserExtSource userExtSource) {
		this.userExtSource = userExtSource;
	}

	public List<UserExtSource> getUserExtSources() {
		return userExtSources;
	}

	public void setUserExtSources(List<UserExtSource> userExtSources) {
		this.userExtSources = userExtSources;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public Boolean getMemberOfGroup() {
		return isMemberOfGroup;
	}

	public void setMemberOfGroup(Boolean memberOfGroup) {
		isMemberOfGroup = memberOfGroup;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MemberCandidate)) return false;
		if (!super.equals(o)) return false;

		MemberCandidate that = (MemberCandidate) o;

		if (member != null ? !member.equals(that.member) : that.member != null) return false;
		if (!userExtSource.equals(that.userExtSource)) return false;
		if (userExtSources != null ? !userExtSources.equals(that.userExtSources) : that.userExtSources != null)
			return false;
		return isMemberOfGroup != null ? isMemberOfGroup.equals(that.isMemberOfGroup) : that.isMemberOfGroup == null;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (member != null ? member.hashCode() : 0);
		result = 31 * result + userExtSource.hashCode();
		result = 31 * result + (userExtSources != null ? userExtSources.hashCode() : 0);
		result = 31 * result + (isMemberOfGroup != null ? isMemberOfGroup.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "MemberCandidate{" +
				"member=" + member +
				", userExtSource=" + userExtSource +
				", userExtSources=" + userExtSources +
				", attributes=" + attributes +
				", isMemberOfGroup=" + isMemberOfGroup +
				'}';
	}
}
