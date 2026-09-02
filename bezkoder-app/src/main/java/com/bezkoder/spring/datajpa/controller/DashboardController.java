package com.bezkoder.spring.datajpa.controller;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.datajpa.dto.DashboardPosicaoResponse;
import com.bezkoder.spring.datajpa.service.DashboardService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/posicoes")
    public ResponseEntity<DashboardPosicaoResponse> getPosicoes(
            @RequestParam(defaultValue = "1") Long idUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        try {
            DashboardPosicaoResponse response = dashboardService.getPosicoes(idUsuario, inicio, fim);
            return new ResponseEntity<DashboardPosicaoResponse>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Falha ao montar dashboard de posicoes. usuario={}, inicio={}, fim={}", idUsuario, inicio, fim, e);
            return new ResponseEntity<DashboardPosicaoResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
