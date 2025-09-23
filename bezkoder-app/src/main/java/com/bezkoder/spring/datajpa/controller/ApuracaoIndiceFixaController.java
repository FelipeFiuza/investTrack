package com.bezkoder.spring.datajpa.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.datajpa.model.ApuracaoIndiceFixa;
import com.bezkoder.spring.datajpa.model.ApuracaoIndiceId;
import com.bezkoder.spring.datajpa.repository.ApuracaoIndiceFixaRepository;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/apuracoes-indice-fixa")
public class ApuracaoIndiceFixaController {

    @Autowired
    ApuracaoIndiceFixaRepository apuracaoIndiceFixaRepository;

    @GetMapping
    public ResponseEntity<List<ApuracaoIndiceFixa>> getAll() {
        try {
            List<ApuracaoIndiceFixa> lista = new ArrayList<>();
            apuracaoIndiceFixaRepository.findAll().forEach(lista::add);

            if (lista.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(lista, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{codIndice}/{dataApuracao}")
    public ResponseEntity<ApuracaoIndiceFixa> getById(@PathVariable("codIndice") String codIndice,
                                                      @PathVariable("dataApuracao") String dataApuracao) {
        try {
            ApuracaoIndiceId id = new ApuracaoIndiceId(codIndice, java.time.LocalDateTime.parse(dataApuracao));
            return apuracaoIndiceFixaRepository.findById(id)
                    .map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping
    public ResponseEntity<ApuracaoIndiceFixa> create(@RequestBody ApuracaoIndiceFixa entity) {
        try {
            ApuracaoIndiceFixa saved = apuracaoIndiceFixaRepository.save(entity);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{codIndice}/{dataApuracao}")
    public ResponseEntity<HttpStatus> delete(@PathVariable("codIndice") String codIndice,
                                             @PathVariable("dataApuracao") String dataApuracao) {
        try {
            ApuracaoIndiceId id = new ApuracaoIndiceId(codIndice, java.time.LocalDateTime.parse(dataApuracao));
            apuracaoIndiceFixaRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}