package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author michael
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiredPermissions {
    Permission[] value();

    boolean isAllPermissionsRequired() default true;

    String dataverseName() default ""; // TODO change to "dvObjectName"
}
