package br.com.avia.avia_api.dto.auth;

import br.com.avia.avia_api.validation.SenhaForte;
import jakarta.validation.constraints.*;

public record CadastroRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @SenhaForte
        @Size(max = 72, message = "A senha nao pode ter mais de 72 caracteres")
        String senha
) {
}
