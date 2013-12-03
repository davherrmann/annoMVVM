package de.davherrmann.mvvm.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.davherrmann.mvvm.ActionHandler;

@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BindAction {
	Class<? extends ActionHandler> value();
	String[] source() default {};
}
