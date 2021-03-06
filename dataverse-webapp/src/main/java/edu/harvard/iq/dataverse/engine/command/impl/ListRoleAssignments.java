package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

import java.util.List;

/**
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class ListRoleAssignments extends AbstractCommand<List<RoleAssignment>> {

    private final DvObject definitionPoint;

    public ListRoleAssignments(DataverseRequest aRequest, DvObject aDefinitionPoint) {
        super(aRequest, aDefinitionPoint);
        definitionPoint = aDefinitionPoint;
    }

    @Override
    public List<RoleAssignment> execute(CommandContext ctxt) {
        return ctxt.permissions().assignmentsOn(definitionPoint);
    }

}
