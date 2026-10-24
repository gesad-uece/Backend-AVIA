package br.com.avia.avia_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {
    private static final int TAMANHO_MINIMO = 8;
    private static final String ESPECIAIS = "çÇ!@#$%^&*()_+-=[]{};':\"\\|,.<>/?";


    private static boolean isEspecial(int character){
        return ESPECIAIS.indexOf(character) >= 0;
    }

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {

        // caso a senha for nulla deixamos o @NotNull tratar
        if(senha == null)
            return true;

        List<String> erros = new ArrayList<>();
        if(senha.length() < TAMANHO_MINIMO){
            erros.add("A senha precisa conter 8 dígitos ou mais");
        }
        if (senha.chars().noneMatch(Character::isDigit)){
            erros.add("A senha precisa conter pelo menos um numero");
        }
        if(senha.chars().noneMatch(Character::isUpperCase)){
            erros.add("A senha precisa conter pelo menos uma letra maiúscula");
        }
        if (senha.chars().noneMatch(SenhaForteValidator::isEspecial)){
            erros.add("A senha precisa conter pelo menos um caractere especial");
        }
        if (erros.isEmpty()){
            return true;
        }
        context.disableDefaultConstraintViolation();
        erros.forEach(erro ->
                            context.buildConstraintViolationWithTemplate(erro).addConstraintViolation());
        return false;
    }

}
