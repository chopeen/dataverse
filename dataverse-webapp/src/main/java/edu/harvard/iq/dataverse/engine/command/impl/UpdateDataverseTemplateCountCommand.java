/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseTemplateCountCommand extends AbstractVoidCommand {

    private final Dataset editedDs;
    private final Template template;

    public UpdateDataverseTemplateCountCommand(DataverseRequest aRequest, Template templateIn, DvObject anAffectedDvObject) {
        super(aRequest, anAffectedDvObject);
        this.editedDs = (Dataset) anAffectedDvObject;
        this.template = templateIn;
    }

    @Override
    protected void executeImpl(CommandContext ctxt)  {
        template.setUsageCount(template.getUsageCount() + 1);
        ctxt.em().merge(this.template);
    }
}
