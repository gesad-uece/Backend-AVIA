package br.com.avia.avia_api.exception.auth;

import br.com.avia.avia_api.exception.ErroResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler {
    // troquei o metodo para receber o exception lançado pelo authenticationManager
    // que ja faz a verificação tanto de email como de senha, de forma mais segura
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponseDTO> handleUsuarioNaoEncontrado(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponseDTO("Credenciais inválidas"));

    }
    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ErroResponseDTO> handleEmailDuplicado(EmailDuplicadoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErroResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> handleSenhaInvalida(MethodArgumentNotValidException ex){
        Map<String, List<String>> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                erros.computeIfAbsent(erro.getField(), k -> new ArrayList<>())
                        .add(erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResponseDTO("Dados inválidos", erros));
    }

}
