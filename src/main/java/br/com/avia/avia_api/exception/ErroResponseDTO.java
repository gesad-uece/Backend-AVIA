package br.com.avia.avia_api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponseDTO(String menssagem, Map<String, List<String>> erros) {

    public ErroResponseDTO(String menssagem){
        this(menssagem, null);
    }
}