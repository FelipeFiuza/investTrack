package com.bezkoder.spring.datajpa.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.datajpa.model.ApuracaoIndice;
import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.repository.ApuracaoIndiceRepository;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;
import com.bezkoder.spring.datajpa.repository.TipoInvestimentoRepository;

@Service
public class IndiceFileIngestionJob implements CommandLineRunner{

    private static final Logger log = LoggerFactory.getLogger(IndiceFileIngestionJob.class);

    private static final String FILE_PATH = "data/COTAHIST_A2024.TXT";

    @Autowired
    private IndiceRepository indiceRepository;

    @Autowired
    private TipoInvestimentoRepository tipoInvestimentoRepository;

    @Autowired
    private ApuracaoIndiceRepository apuracaoIndiceRepository;

    @Override
    public void run(String... args) throws Exception {
        scanAndIngest();
    }
	


    // Runs every 10 minutes
    @Scheduled(cron = "0 */30 * * * *")
    public void scanAndIngest() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                log.warn("COTAHIST file not found at {}", FILE_PATH);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
                String line;
                log.info("Processing COTAHIST file at {}", LocalDateTime.now());
                while ((line = reader.readLine()) != null) {
                    processLine(line);
                }
                log.info("Process finished COTAHIST file at {}", LocalDateTime.now());
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
            String typeCode = safeSubstring(line, 24, 27).trim();
            if (!"010".equals(typeCode) && !"020".equals(typeCode)) {
                return;
            }            

            String specificationCode = safeSubstring(line, 39, 49).trim();
            if (!specificationCode.startsWith("ON") && !specificationCode.startsWith("PN")) {
                return;
            }

            // ticker at chars 12-17 inclusive => substring(11, 17)
            String ticker = safeSubstring(line, 12, 24).trim();
            if (ticker.isEmpty()) {
                return;
            }

            Indice savedIndice;

            // check if exists by descricao
            if (!indiceRepository.existsByDescricao(ticker)) {
                Indice indice = new Indice();
                indice.setDescricao(ticker);
                indice.setTipoIndice("F");
                indice.setPeriodicidade("D");
                savedIndice = indiceRepository.save(indice);
            } else {
                savedIndice = indiceRepository.findByDescricao(ticker);
            } 

            String descricao = safeSubstring(line, 27, 39).trim();

            if(!tipoInvestimentoRepository.existsByIndice(savedIndice)) {
                TipoInvestimento tipoInvestimento = new TipoInvestimento();
                tipoInvestimento.setIndice(savedIndice);
                tipoInvestimento.setDescricao(ticker + " - " + descricao);
                tipoInvestimento.setIncideIof("N");
                tipoInvestimento.setIncideIr("A");
                tipoInvestimentoRepository.save(tipoInvestimento);
            }

            String year = safeSubstring(line, 2, 6).trim();
            String month = safeSubstring(line, 6, 8).trim();
            String day = safeSubstring(line, 8, 10).trim();
            LocalDateTime dataApuracao = LocalDateTime.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day), 0, 0);

            BigDecimal valorAbertura = new BigDecimal(safeSubstring(line, 56, 69).trim());
            BigDecimal valorMaximo = new BigDecimal(safeSubstring(line, 69, 82).trim());
            BigDecimal valorMinimo = new BigDecimal(safeSubstring(line, 82, 95).trim());
            BigDecimal valorFechamento = new BigDecimal(safeSubstring(line, 95, 108).trim());

            ApuracaoIndice apuracaoIndice = new ApuracaoIndice();
            apuracaoIndice.setIndice(savedIndice);
            apuracaoIndice.setDataApuracao(dataApuracao);
            apuracaoIndice.setValorAbertura(valorAbertura);
            apuracaoIndice.setValorMaximo(valorMaximo);
            apuracaoIndice.setValorMinimo(valorMinimo);
            apuracaoIndice.setValorFechamento(valorFechamento);
            apuracaoIndiceRepository.save(apuracaoIndice);

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
