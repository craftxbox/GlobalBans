package com.craftxbox.globalbans.command;

import discord4j.core.object.util.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Describe {

    boolean botOwner() default false;
    boolean hidden() default false;
    String commandDescription() default "";
    Permission[] requiredPermissions() default {};

}
