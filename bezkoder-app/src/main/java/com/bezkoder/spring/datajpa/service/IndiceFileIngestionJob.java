package com.bezkoder.spring.datajpa.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;

@Service
public class IndiceFileIngestionJob {

    private static final Logger log = LoggerFactory.getLogger(IndiceFileIngestionJob.class);

    private static final String FILE_PATH = "data/COTAHIST_D22092025.TXT";

    @Autowired
    private IndiceRepository indiceRepository;

    // Runs every 10 minutes
    @Scheduled(cron = "0 */3 * * * *")
    public void scanAndIngest() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                log.warn("COTAHIST file not found at {}", FILE_PATH);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processLine(line);
                }
            }
        } catch (Exception e) {
            log.error("Error processing COTAHIST file", e);
        }
    }

    private void processLine(String line) {
        try {
            if (line.length() < 27) {
                return; // too short
            }

            // positions are 1-based in the request; convert to 0-based substring indices
            // chars 24-27 inclusive => substring(23, 27)
            String typeCode = safeSubstring(line, 23, 27).trim();
            if (!"010".equals(typeCode)) {
                return;
            }

            // ticker at chars 12-17 inclusive => substring(11, 17)
            String ticker = safeSubstring(line, 12, 17).trim();
            if (ticker.isEmpty()) {
                return;
            }

            // check if exists by descricao
            if (indiceRepository.existsByDescricao(ticker)) {
                return;
            }

            // create new Indice: descricao=ticker, tipoIndice=F, periodicidade=D
            Indice indice = new Indice();
            indice.setDescricao(ticker);
            indice.setTipoIndice("F");
            indice.setPeriodicidade("D");

            indiceRepository.save(indice);
        } catch (Exception ex) {
            log.warn("Failed to process line: {}", line, ex);
        }
    }

    private String safeSubstring(String s, int begin, int endExclusive) {
        int len = s.length();
        int start = Math.max(0, Math.min(begin, len));
        int end = Math.max(start, Math.min(endExclusive, len));
        return s.substring(start, end);
    }
}
