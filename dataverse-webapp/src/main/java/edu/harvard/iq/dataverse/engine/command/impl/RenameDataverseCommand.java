package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author michael
 */
@RequiredPermissions(Permission.EditDataverse)
public class RenameDataverseCommand extends AbstractCommand<Dataverse> {

    private final String newName;
    private final Dataverse renamed;

    public RenameDataverseCommand(DataverseRequest aRequest, Dataverse aDataverse, String aNewName) {
        super(aRequest, aDataverse);
        newName = aNewName;
        renamed = aDataverse;
    }

    @Override
    public Dataverse execute(CommandContext ctxt)  {
        if (newName.trim().isEmpty()) {
            throw new IllegalCommandException("Dataverse name cannot be empty", this);
        }

        renamed.setName(newName);
        return ctxt.dataverses().save(renamed);
    }

}
