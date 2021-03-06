package cz.metacentrum.perun.core.impl.modules.attributes;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.User;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.ConsistencyErrorException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeAssignmentException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException;
import cz.metacentrum.perun.core.api.exceptions.WrongReferenceAttributeValueException;
import cz.metacentrum.perun.core.blImpl.ModulesUtilsBlImpl;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Class for checking logins uniqueness in the namespace and filling vsup id.
 *
 * @author Pavel Zlámal <zlamal@cesnet.cz>
 */
public class urn_perun_user_attribute_def_def_login_namespace_vsup extends urn_perun_user_attribute_def_def_login_namespace {

	private final static Logger log = LoggerFactory.getLogger(urn_perun_user_attribute_def_def_login_namespace_vsup.class);
	private final static Set<String> unpermittedLogins = new HashSet<String>(Arrays.asList("administrator", "admin", "guest", "host", "vsup", "umprum", "root"));
	private final static String EDUROAM_VSUP_NAMESPACE = AttributesManager.NS_USER_ATTR_DEF + ":login-namespace:eduroam-vsup";

	/**
	 * Checks if the user's login is unique in the namespace organization.
	 * Check if maximum length is 20 chars, because of MSAD limitations.
	 *
	 * @param sess PerunSession
	 * @param user User to check attribute for
	 * @param attribute Attribute to check value to
	 * @throws cz.metacentrum.perun.core.api.exceptions.InternalErrorException
	 * @throws cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException
	 * @throws cz.metacentrum.perun.core.api.exceptions.WrongAttributeAssignmentException
	 */
	@Override
	public void checkAttributeValue(PerunSessionImpl sess, User user, Attribute attribute) throws InternalErrorException, WrongAttributeValueException, WrongAttributeAssignmentException {

		if (attribute != null && unpermittedLogins.contains((String)attribute.getValue())) throw new WrongAttributeValueException(attribute, user, "Login '" + attribute.getValue() + "' is not permitted.");

		// check uniqueness
		super.checkAttributeValue(sess, user, attribute);

		// plus check, that login is max 20 chars.
		if (attribute.getValue() != null) {
			if (((String)attribute.getValue()).length() > 20) throw new WrongAttributeValueException(attribute, user, "Login '" + attribute.getValue() + "' exceeds 20 chars limit.");
		}

	}

	/**
	 * Filling implemented for:
	 * - namespaces configured in /etc/perun/perun.properties as property: "perun.loginNamespace.generated"
	 *
	 * Resulting format/rules:
	 * - "first char of firstName + up to 5 chars of lastName + [number]" where number is opt and start with 1 when same login is already present.
	 * - Only first part of "firstName" and last part of "lastName" is taken.
	 * - All accented chars are unaccented and all non (a-z,A-Z) chars are removed from name and value is lowered.
	 *
	 * @param perunSession PerunSession
	 * @param user User to fill attribute for
	 * @param attribute Attribute to fill value to
	 * @return Filled attribute
	 * @throws InternalErrorException
	 * @throws WrongAttributeAssignmentException
	 */
	@Override
	public Attribute fillAttribute(PerunSessionImpl perunSession, User user, AttributeDefinition attribute) throws InternalErrorException, WrongAttributeAssignmentException {

		Attribute filledAttribute = new Attribute(attribute);

		if (generatedNamespaces.contains(attribute.getFriendlyNameParameter())) {

			ModulesUtilsBlImpl.LoginGenerator generator = new ModulesUtilsBlImpl.LoginGenerator();
			String login = generator.generateLogin(user, new ModulesUtilsBlImpl.LoginGenerator.LoginGeneratorFunction() {
				@Override
				public String generateLogin(String firstName, String lastName) {
					String login = firstName.substring(0, 1)+lastName.substring(0, (6 <= lastName.length()) ? 6 : lastName.length());
					if (login.length()>20) {
						login = login.substring(0, 20);
					}
					return login;
				}
			});

			if (login == null) return filledAttribute;

			// fill value - start as login, login2, login3, ....
			int iterator = 1;
			while (iterator >= 1) {
				if (iterator > 1) {
					int iteratorLength = String.valueOf(iterator).length();
					if (login.length() + iteratorLength > 20) {
						// if login+iterator > 20 => crop login & reset iterator
						login = login.substring(0, login.length()-1);
						iterator = 1;
						filledAttribute.setValue(login);
					} else {
						filledAttribute.setValue(login + iterator);
					}

				} else {
					filledAttribute.setValue(login);
				}
				try {
					checkAttributeValue(perunSession, user, filledAttribute);
					return filledAttribute;
				} catch (WrongAttributeValueException ex) {
					// continue in a WHILE cycle
					iterator++;
				}
			}

			return filledAttribute;

		} else {
			// without value
			return filledAttribute;
		}

	}

	/**
	 * When login changes: first set / changed always change eduroam-vsup login too !!
	 *
	 * @param session
	 * @param user
	 * @param attribute
	 * @throws InternalErrorException
	 * @throws WrongReferenceAttributeValueException
	 */
	@Override
	public void changedAttributeHook(PerunSessionImpl session, User user, Attribute attribute) throws InternalErrorException, WrongReferenceAttributeValueException {

		if(attribute.getValue() != null) {
			Attribute eduroamLogin = null;
			try {
				eduroamLogin = session.getPerunBl().getAttributesManagerBl().getAttribute(session, user, EDUROAM_VSUP_NAMESPACE);
				if(!Objects.equals(attribute.getValue(), eduroamLogin.getValue())) {
					eduroamLogin.setValue(attribute.getValue());
					session.getPerunBl().getAttributesManagerBl().setAttribute(session, user, eduroamLogin);
				}
			} catch (WrongAttributeAssignmentException ex) {
				throw new InternalErrorException(ex);
			} catch (AttributeNotExistsException ex) {
				throw new ConsistencyErrorException(ex);
			} catch (WrongAttributeValueException ex) {
				throw new WrongReferenceAttributeValueException(attribute, eduroamLogin, "Mismatch in checking of users VŠUP login and eduroam login.", ex);
			}
		}

	}

	public AttributeDefinition getAttributeDefinition() {
		AttributeDefinition attr = new AttributeDefinition();
		attr.setNamespace(AttributesManager.NS_USER_ATTR_DEF);
		attr.setFriendlyName("login-namespace:vsup");
		attr.setDisplayName("Login in namespace: vsup");
		attr.setType(String.class.getName());
		attr.setDescription("Logname in namespace 'vsup'.");
		return attr;
	}

}
