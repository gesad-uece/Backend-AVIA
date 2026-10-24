package br.com.avia.avia_api.exception.projeto;

import br.com.avia.avia_api.exception.ErroResponseDTO;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProjetoExceptionHandler {

    @ExceptionHandler(CargoException.class)
    public ResponseEntity<ErroResponseDTO> handleCargoException(CargoException ex){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErroResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(UsuarioException.class)
    public ResponseEntity<ErroResponseDTO> handleUsuarioException(UsuarioException ex){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErroResponseDTO("Usuario não encontrado"));
                // generalizar para nao vazar informações
    }

    @ExceptionHandler(UltimoGerenteException.class)
    public ResponseEntity<ErroResponseDTO> handleUltimoGerente(UltimoGerenteException ex){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErroResponseDTO("Por favor indique um novo gerente para conseguir sair"));
        // talvez padronizar as as mensagens de erro, para mensagem: <tipo do erro>
        // como retorna o back? qual o erro status, conversar com o front para descobrir
    }

    @ExceptionHandler(ProjetoNaoEncontradoException.class)
    public ResponseEntity<ErroResponseDTO> handleProjetoNaoEncontrado(ProjetoNaoEncontradoException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponseDTO("Projeto não encontrado"));
    }
}
