package com.bezkoder.spring.datajpa.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.datajpa.dto.PanelCreateDTO;
import com.bezkoder.spring.datajpa.dto.PanelDTO;
import com.bezkoder.spring.datajpa.dto.PanelPosicaoResponse;
import com.bezkoder.spring.datajpa.dto.PanelUpdateDTO;
import com.bezkoder.spring.datajpa.service.PanelService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/panel")
public class PanelController {

    private static final Logger log = LoggerFactory.getLogger(PanelController.class);

    @Autowired
    private PanelService panelService;

    @GetMapping
    public ResponseEntity<List<PanelDTO>> listByUsuario(@RequestParam(defaultValue = "1") Long idUsuario) {
        try {
            return new ResponseEntity<List<PanelDTO>>(panelService.listByUsuario(idUsuario), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Falha ao listar panels. usuario={}", idUsuario, e);
            return new ResponseEntity<List<PanelDTO>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<PanelDTO> getById(@PathVariable("id") Long id) {
        try {
            PanelDTO panel = panelService.getById(id);
            if (panel == null) {
                return new ResponseEntity<PanelDTO>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<PanelDTO>(panel, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Falha ao carregar panel. id={}", id, e);
            return new ResponseEntity<PanelDTO>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<PanelDTO> create(@RequestBody PanelCreateDTO dto) {
        try {
            PanelDTO panel = panelService.create(dto);
            if (panel == null) {
                return new ResponseEntity<PanelDTO>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<PanelDTO>(panel, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<PanelDTO>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Falha ao criar panel", e);
            return new ResponseEntity<PanelDTO>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PanelDTO> update(@PathVariable("id") Long id, @RequestBody PanelUpdateDTO dto) {
        try {
            PanelDTO panel = panelService.update(id, dto);
            if (panel == null) {
                return new ResponseEntity<PanelDTO>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<PanelDTO>(panel, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<PanelDTO>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Falha ao atualizar panel. id={}", id, e);
            return new ResponseEntity<PanelDTO>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/posicoes")
    public ResponseEntity<PanelPosicaoResponse> getPosicoes(
            @RequestParam(defaultValue = "1") Long idUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String idsExcluidos) {
        try {
            PanelPosicaoResponse response = panelService.getPosicoes(idUsuario, inicio, fim,
                    parseIds(idsExcluidos));
            return new ResponseEntity<PanelPosicaoResponse>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Falha ao montar panel de posicoes. usuario={}, inicio={}, fim={}", idUsuario, inicio, fim, e);
            return new ResponseEntity<PanelPosicaoResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Long> parseIds(String csv) {
        List<Long> ids = new ArrayList<Long>();
        if (csv == null || csv.trim().isEmpty()) {
            return ids;
        }
        String[] parts = csv.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(part));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }
}
