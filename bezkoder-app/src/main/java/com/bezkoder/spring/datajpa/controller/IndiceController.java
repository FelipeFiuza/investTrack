package com.bezkoder.spring.datajpa.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bezkoder.spring.datajpa.dto.IndiceCreateDTO;
import com.bezkoder.spring.datajpa.dto.IndiceDTO;
import com.bezkoder.spring.datajpa.dto.IndiceUpdateDTO;
import com.bezkoder.spring.datajpa.service.IndiceService;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api/indices")
public class IndiceController {

    @Autowired
    private IndiceService indiceService;

    @GetMapping
    public ResponseEntity<List<IndiceDTO>> getAll(
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String tipoIndice) {
        try {

            List<IndiceDTO> indices = new ArrayList<>();

            if(descricao != null && !descricao.isEmpty()) {
                indices = indiceService.getIndicesByDescricao(descricao);
            } else if(tipoIndice != null && !tipoIndice.isEmpty()) {
                indices = indiceService.getIndicesByTipoIndice(tipoIndice);
            } else {
                indices = indiceService.getAllIndices(descricao, tipoIndice);
            }

            if (indices.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(indices, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{codIndice}")
    public ResponseEntity<IndiceDTO> getById(@PathVariable("codIndice") Long codIndice) {
        Optional<IndiceDTO> indiceData = indiceService.getIndiceById(codIndice);

        return indiceData.map(indice -> new ResponseEntity<>(indice, HttpStatus.OK))
                          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<IndiceDTO> create(@RequestBody IndiceCreateDTO indiceCreateDTO) {
        try {
            IndiceDTO indice = indiceService.createIndice(indiceCreateDTO);
            return new ResponseEntity<>(indice, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{codIndice}")
    public ResponseEntity<IndiceDTO> update(@PathVariable("codIndice") Long codIndice, 
                                           @RequestBody IndiceUpdateDTO indiceUpdateDTO) {
        Optional<IndiceDTO> indiceData = indiceService.updateIndice(codIndice, indiceUpdateDTO);

        if (indiceData.isPresent()) {
            return new ResponseEntity<>(indiceData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{codIndice}")
    public ResponseEntity<HttpStatus> delete(@PathVariable("codIndice") Long codIndice) {
        try {
            if (indiceService.deleteIndice(codIndice)) {
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
            if (indiceService.deleteAllIndices()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
