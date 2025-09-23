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
import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.datajpa.model.Transacao;
import com.bezkoder.spring.datajpa.repository.TransacaoRepository;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    @Autowired
    TransacaoRepository transacaoRepository;

    @GetMapping
    public ResponseEntity<List<Transacao>> getAll() {
        try {
            List<Transacao> transacoes = new ArrayList<>();
            transacaoRepository.findAll().forEach(transacoes::add);

            if (transacoes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(transacoes, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transacao> getById(@PathVariable("id") Long id) {
        Optional<Transacao> transacao = transacaoRepository.findById(id);

        return transacao.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Transacao> create(@RequestBody Transacao transacao) {
        try {
            Transacao _transacao = transacaoRepository.save(transacao);
            return new ResponseEntity<>(_transacao, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transacao> update(@PathVariable("id") Long id, @RequestBody Transacao transacao) {
        Optional<Transacao> transacaoData = transacaoRepository.findById(id);

        if (transacaoData.isPresent()) {
            Transacao _transacao = transacaoData.get();
            _transacao.setCodInvestimento(transacao.getCodInvestimento());
            _transacao.setIdUsuario(transacao.getIdUsuario());
            _transacao.setDataTransacao(transacao.getDataTransacao());
            _transacao.setDataVencimento(transacao.getDataVencimento());
            _transacao.setInstituicao(transacao.getInstituicao());
            _transacao.setValor(transacao.getValor());
            _transacao.setQuantidade(transacao.getQuantidade());
            return new ResponseEntity<>(transacaoRepository.save(_transacao), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable("id") Long id) {
        try {
            transacaoRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping
    public ResponseEntity<HttpStatus> deleteAll() {
        try {
            transacaoRepository.deleteAll();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}