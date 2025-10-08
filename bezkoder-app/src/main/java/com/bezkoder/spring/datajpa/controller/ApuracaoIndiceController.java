package com.bezkoder.spring.datajpa.controller;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bezkoder.spring.datajpa.dto.ApuracaoIndiceCreateDTO;
import com.bezkoder.spring.datajpa.dto.ApuracaoIndiceDTO;
import com.bezkoder.spring.datajpa.dto.ApuracaoIndiceUpdateDTO;
import com.bezkoder.spring.datajpa.service.ApuracaoIndiceService;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/apuracoes-indice")
public class ApuracaoIndiceController {

    @Autowired
    private ApuracaoIndiceService apuracaoIndiceService;

    @GetMapping
    public ResponseEntity<List<ApuracaoIndiceDTO>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        try {
            List<ApuracaoIndiceDTO> apuracoes = apuracaoIndiceService.getAll(inicio, fim);

            if (apuracoes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(apuracoes, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/indice/{codIndice}")
    public ResponseEntity<List<ApuracaoIndiceDTO>> getByIndice(@PathVariable("codIndice") Long codIndice) {
        try {
            List<ApuracaoIndiceDTO> apuracoes = apuracaoIndiceService.getByIndice(codIndice);

            if (apuracoes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(apuracoes, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/indice/{codIndice}/periodo")
    public ResponseEntity<List<ApuracaoIndiceDTO>> getByIndiceAndPeriodo(
            @PathVariable("codIndice") Long codIndice,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        try {
            List<ApuracaoIndiceDTO> apuracoes = apuracaoIndiceService.getByIndiceAndPeriodo(codIndice, inicio, fim);

            if (apuracoes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(apuracoes, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{codIndice}/{dataApuracao}")
    public ResponseEntity<ApuracaoIndiceDTO> getById(
            @PathVariable("codIndice") Long codIndice,
            @PathVariable("dataApuracao") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataApuracao) {
        Optional<ApuracaoIndiceDTO> apuracao = apuracaoIndiceService.getById(codIndice, dataApuracao);

        return apuracao.map(it -> new ResponseEntity<>(it, HttpStatus.OK))
                       .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ApuracaoIndiceDTO> create(@RequestBody ApuracaoIndiceCreateDTO createDTO) {
        try {
            Optional<ApuracaoIndiceDTO> apuracao = apuracaoIndiceService.create(createDTO);
            if (apuracao.isPresent()) {
                return new ResponseEntity<>(apuracao.get(), HttpStatus.CREATED);
            }
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{codIndice}/{dataApuracao}")
    public ResponseEntity<ApuracaoIndiceDTO> update(
            @PathVariable("codIndice") Long codIndice,
            @PathVariable("dataApuracao") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataApuracao,
            @RequestBody ApuracaoIndiceUpdateDTO updateDTO) {
        Optional<ApuracaoIndiceDTO> apuracao = apuracaoIndiceService.update(codIndice, dataApuracao, updateDTO);

        return apuracao.map(it -> new ResponseEntity<>(it, HttpStatus.OK))
                       .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{codIndice}/{dataApuracao}")
    public ResponseEntity<HttpStatus> delete(
            @PathVariable("codIndice") Long codIndice,
            @PathVariable("dataApuracao") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataApuracao) {
        try {
            if (apuracaoIndiceService.delete(codIndice, dataApuracao)) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping
    public ResponseEntity<HttpStatus> deleteAll() {
        try {
            if (apuracaoIndiceService.deleteAll()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

