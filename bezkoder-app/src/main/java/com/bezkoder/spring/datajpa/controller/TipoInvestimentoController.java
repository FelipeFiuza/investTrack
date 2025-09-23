package com.bezkoder.spring.datajpa.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.repository.TipoInvestimentoRepository;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/tipos-investimento")
public class TipoInvestimentoController {

    @Autowired
    TipoInvestimentoRepository tipoInvestimentoRepository;

    @GetMapping
    public ResponseEntity<List<TipoInvestimento>> getAll(@RequestParam(required = false) String descricao) {
        try {
            List<TipoInvestimento> tipos = new ArrayList<>();

            if (descricao == null)
                tipoInvestimentoRepository.findAll().forEach(tipos::add);
            else
                tipoInvestimentoRepository.findByDescricaoContaining(descricao).forEach(tipos::add);

            if (tipos.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(tipos, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoInvestimento> getById(@PathVariable("id") Long id) {
        Optional<TipoInvestimento> tipo = tipoInvestimentoRepository.findById(id);

        return tipo.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                   .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<TipoInvestimento> create(@RequestBody TipoInvestimento tipoInvestimento) {
        try {
            TipoInvestimento _tipo = tipoInvestimentoRepository.save(tipoInvestimento);
            return new ResponseEntity<>(_tipo, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoInvestimento> update(@PathVariable("id") Long id, @RequestBody TipoInvestimento tipoInvestimento) {
        Optional<TipoInvestimento> tipoData = tipoInvestimentoRepository.findById(id);

        if (tipoData.isPresent()) {
            TipoInvestimento _tipo = tipoData.get();
            _tipo.setDescricao(tipoInvestimento.getDescricao());
            _tipo.setCodIndice(tipoInvestimento.getCodIndice());
            _tipo.setIncideIof(tipoInvestimento.getIncideIof());
            _tipo.setIncideIr(tipoInvestimento.getIncideIr());
            return new ResponseEntity<>(tipoInvestimentoRepository.save(_tipo), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable("id") Long id) {
        try {
            tipoInvestimentoRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping
    public ResponseEntity<HttpStatus> deleteAll() {
        try {
            tipoInvestimentoRepository.deleteAll();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}