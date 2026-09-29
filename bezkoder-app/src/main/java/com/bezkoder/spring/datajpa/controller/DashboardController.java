package com.bezkoder.spring.datajpa.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.bezkoder.spring.datajpa.dto.DashboardDTO;
import com.bezkoder.spring.datajpa.dto.DashboardWriteDTO;
import com.bezkoder.spring.datajpa.service.DashboardService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<List<DashboardDTO>> listByUsuario(@RequestParam(defaultValue = "1") Long idUsuario) {
        try {
            return new ResponseEntity<List<DashboardDTO>>(dashboardService.listByUsuario(idUsuario), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Falha ao listar dashboards. usuario={}", idUsuario, e);
            return new ResponseEntity<List<DashboardDTO>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DashboardDTO> getById(@PathVariable("id") Long id) {
        try {
            DashboardDTO dashboard = dashboardService.getById(id);
            if (dashboard == null) {
                return new ResponseEntity<DashboardDTO>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<DashboardDTO>(dashboard, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Falha ao carregar dashboard. id={}", id, e);
            return new ResponseEntity<DashboardDTO>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<DashboardDTO> create(@RequestBody DashboardWriteDTO dto) {
        try {
            DashboardDTO dashboard = dashboardService.create(dto);
            if (dashboard == null) {
                return new ResponseEntity<DashboardDTO>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<DashboardDTO>(dashboard, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<DashboardDTO>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Falha ao criar dashboard", e);
            return new ResponseEntity<DashboardDTO>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DashboardDTO> update(@PathVariable("id") Long id, @RequestBody DashboardWriteDTO dto) {
        try {
            DashboardDTO dashboard = dashboardService.update(id, dto);
            if (dashboard == null) {
                return new ResponseEntity<DashboardDTO>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<DashboardDTO>(dashboard, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<DashboardDTO>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Falha ao atualizar dashboard. id={}", id, e);
            return new ResponseEntity<DashboardDTO>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
