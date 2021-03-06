package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.User;

import java.util.Set;

/**
 * Creates and manages groups. Also looks up groups for a
 * {@link User} and a {@link UserRequestMetadata}.
 *
 * @param <T> the actual type of the group.
 * @author michael
 */
public interface GroupProvider<T extends Group> {


    /**
     * Returns class type of group that can be provided with
     * this provider
     */
    Class<T> providerFor();
    
    /**
     * The alias of this provider. Has to be unique in the system.
     *
     * @return The alias of the factory.
     */
    String getGroupProviderAlias();

    /**
     * @return A human readable display string describing this factory.
     */
    String getGroupProviderInfo();

    /**
     * Looks up the groups this provider has for a role assignee, in the context of a {@link DvObject}.
     * <B>This method should be used for group management. Groups for actual requests should be determined
     * by calling {@link #groupsFor(edu.harvard.iq.dataverse.engine.command.DataverseRequest, edu.harvard.iq.dataverse.persistence.DvObject)}.</B>
     *
     * @param ra
     * @param dvo the DvObject which is the context for the groups. May be {@code null}
     * @return The set of groups the role assignee is a member of.
     * @see #groupsFor(edu.harvard.iq.dataverse.engine.command.DataverseRequest, edu.harvard.iq.dataverse.persistence.DvObject)
     */
    Set<T> groupsFor(RoleAssignee ra, DvObject dvo);

    /**
     * Looks up the groups this provider has for a dataverse request, in the context of a {@link DvObject}.
     *
     * @param req The request whose group memberships we evaluate.
     * @param dvo the DvObject which is the context for the groups. May be {@code null}.
     * @return The set of groups the user is member of.
     */
    Set<T> groupsFor(DataverseRequest req, DvObject dvo);

    Set<T> groupsFor(RoleAssignee ra);

    Set<T> groupsFor(DataverseRequest req);

    T get(String groupAlias);

    Set<T> findGlobalGroups();
    
    /**
     * Tests to see whether {@code aRequest} is a part of {@code this} group. Inclusion
     * in groups is not just a matter of user, as it can be specified also by, e.g. IP addresses.
     * The containment may not always be present in the DB - for example,
     * some groups may determine membership based on request properties, such as IP address.
     *
     * @param aRequest The request whose inclusion we test
     * @return {@code true} iff {@code anAssignee} is in this group; {@code false} otherwise.
     */
     boolean contains(DataverseRequest aRequest, T group);
}
