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

import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/indices")
public class IndiceController {

    @Autowired
    IndiceRepository indiceRepository;

    @GetMapping
    public ResponseEntity<List<Indice>> getAll(@RequestParam(required = false) String descricao) {
        try {
            List<Indice> indices = new ArrayList<>();

            if (descricao == null)
                indiceRepository.findAll().forEach(indices::add);
            else
                indiceRepository.findByDescricaoContaining(descricao).forEach(indices::add);

            if (indices.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(indices, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{codIndice}")
    public ResponseEntity<Indice> getById(@PathVariable("codIndice") String codIndice) {
        Optional<Indice> indiceData = indiceRepository.findById(codIndice);

        return indiceData.map(indice -> new ResponseEntity<>(indice, HttpStatus.OK))
                         .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Indice> create(@RequestBody Indice indice) {
        try {
            Indice _indice = indiceRepository.save(indice);
            return new ResponseEntity<>(_indice, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{codIndice}")
    public ResponseEntity<Indice> update(@PathVariable("codIndice") String codIndice, @RequestBody Indice indice) {
        Optional<Indice> indiceData = indiceRepository.findById(codIndice);

        if (indiceData.isPresent()) {
            Indice _indice = indiceData.get();
            _indice.setDescricao(indice.getDescricao());
            _indice.setPeriodicidade(indice.getPeriodicidade());
            _indice.setTipoIndice(indice.getTipoIndice());
            return new ResponseEntity<>(indiceRepository.save(_indice), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{codIndice}")
    public ResponseEntity<HttpStatus> delete(@PathVariable("codIndice") String codIndice) {
        try {
            indiceRepository.deleteById(codIndice);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping
    public ResponseEntity<HttpStatus> deleteAll() {
        try {
            indiceRepository.deleteAll();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}