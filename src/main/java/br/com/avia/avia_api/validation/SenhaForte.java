package br.com.avia.avia_api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {
    String message() default "Senha invalida";

    Class<?>[] groups() default{};

    Class<? extends Payload>[] payload() default {};
}
