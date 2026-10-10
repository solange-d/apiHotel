package com.esos.apiHotel.controller;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.esos.apiHotel.exception.RegraNegocioException;

final class Paginacao {
    private Paginacao() { }

    static Pageable criar(int pagina, int tamanho, String ordenarPor, String direcao, Set<String> permitidos) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new RegraNegocioException("Página deve ser >= 0 e tamanho entre 1 e 100.");
        }
        if (!permitidos.contains(ordenarPor) || !(direcao.equalsIgnoreCase("asc") || direcao.equalsIgnoreCase("desc"))) {
            throw new RegraNegocioException("Ordenação inválida.");
        }
        Sort ordem = Sort.by(Sort.Direction.fromString(direcao), ordenarPor);
        if (!ordenarPor.equals("id")) ordem = ordem.and(Sort.by("id"));
        return PageRequest.of(pagina, tamanho, ordem);
    }
}
