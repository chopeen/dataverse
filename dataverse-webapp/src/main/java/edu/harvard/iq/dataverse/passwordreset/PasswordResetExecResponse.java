package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.persistence.user.PasswordResetData;

public class PasswordResetExecResponse {

    private String tokenQueried;
    private PasswordResetData passwordResetData;

    public PasswordResetExecResponse(String tokenQueried, PasswordResetData passwordResetData) {
        this.tokenQueried = tokenQueried;
        this.passwordResetData = passwordResetData;
    }

    public String getTokenQueried() {
        return tokenQueried;
    }

    public PasswordResetData getPasswordResetData() {
        return passwordResetData;
    }

    public void setPasswordResetData(PasswordResetData passwordResetData) {
        this.passwordResetData = passwordResetData;
    }

}
