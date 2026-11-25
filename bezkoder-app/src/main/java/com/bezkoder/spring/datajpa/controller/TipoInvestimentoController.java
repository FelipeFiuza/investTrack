package com.bezkoder.spring.datajpa.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.datajpa.dto.TipoInvestimentoDTO;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.repository.TipoInvestimentoRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tipos-investimento")
public class TipoInvestimentoController {

    private static final int LIMITE_ACOES = 15;

    @Autowired
    private TipoInvestimentoRepository tipoInvestimentoRepository;

    @GetMapping
    public ResponseEntity<List<TipoInvestimentoDTO>> getByDescricao(@RequestParam("descricao") String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<TipoInvestimentoDTO> tipos = tipoInvestimentoRepository.findByDescricao(descricao.trim()).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/acoes")
    public ResponseEntity<List<TipoInvestimentoDTO>> searchAcoes(@RequestParam("q") String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        String termo = q.trim();
        if (termo.length() > 40) {
            termo = termo.substring(0, 40);
        }
        List<TipoInvestimentoDTO> acoes = tipoInvestimentoRepository
            .searchAcoes(termo, PageRequest.of(0, LIMITE_ACOES))
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(acoes);
    }

    private TipoInvestimentoDTO toDTO(TipoInvestimento tipo) {
        return new TipoInvestimentoDTO(
            tipo.getCodInvestimento(),
            tipo.getDescricao(),
            tipo.getIncideIof(),
            tipo.getIncideIr()
        );
    }
}
