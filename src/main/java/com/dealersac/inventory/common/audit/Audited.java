package com.dealersac.inventory.common.audit;

import java.lang.annotation.*;

/**
 * Mark any service method to be captured in the audit log.
 * The value should describe the action, e.g. "CREATE_DEALER".
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    String action();
    String entityType() default "";
}
