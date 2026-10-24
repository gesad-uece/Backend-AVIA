package br.com.avia.avia_api.exception.projeto;

import lombok.NoArgsConstructor;

//correto o no args? ja que o handle ja envia uma mensgaem
@NoArgsConstructor
public class ProjetoNaoEncontradoException extends RuntimeException {
    public ProjetoNaoEncontradoException(String message) {
        super(message);
    }
}
