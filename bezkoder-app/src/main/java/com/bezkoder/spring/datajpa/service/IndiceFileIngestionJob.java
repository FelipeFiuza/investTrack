package com.bezkoder.spring.datajpa.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.bezkoder.spring.datajpa.model.ApuracaoIndice;
import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;
import com.bezkoder.spring.datajpa.repository.TipoInvestimentoRepository;

@Service
public class IndiceFileIngestionJob implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IndiceFileIngestionJob.class);

    private static final int BATCH_SIZE = 2000;
    private static final int READER_BUFFER_SIZE = 64 * 1024;

    private static final String INSERT_APURACAO_SQL =
        "INSERT INTO apuracao_indice (" +
        "data_apuracao, cod_indice, valor_abertura, valor_maximo, valor_minimo, valor_fechamento, numero_distribuicao" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        "valor_abertura = VALUES(valor_abertura), " +
        "valor_maximo = VALUES(valor_maximo), " +
        "valor_minimo = VALUES(valor_minimo), " +
        "valor_fechamento = VALUES(valor_fechamento), " +
        "numero_distribuicao = VALUES(numero_distribuicao)";

    private static final List<String> FILE_PATHS = Arrays.asList(
        // "data/COTAHIST_A2021.TXT",
        // "data/COTAHIST_A2022.TXT",
        "data/COTAHIST_A2023.TXT",
        "data/COTAHIST_A2024.TXT",
        "data/COTAHIST_A2025.TXT",
        "data/COTAHIST_A2026.TXT"
    );

    @Autowired
    private IndiceRepository indiceRepository;

    @Autowired
    private TipoInvestimentoRepository tipoInvestimentoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Map<String, Indice> indiceByTicker = new HashMap<String, Indice>();
    private final Set<Long> tipoInvestimentoIndiceIds = new HashSet<Long>();
    private TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) throws Exception {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        loadCaches();
        long startedAt = System.currentTimeMillis();
        int totalInserted = 0;
        for (String filePath : FILE_PATHS) {
            totalInserted += scanAndIngest(filePath);
        }
        log.info("COTAHIST ingestion finished: {} apuracao_indice rows in {} ms",
            totalInserted, System.currentTimeMillis() - startedAt);
    }

    // Runs every 10 minutes
    //@Scheduled(cron = "0 */30 * * * *")
    public int scanAndIngest(String filePath) {
        int inserted = 0;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("COTAHIST file not found at {}", filePath);
                return 0;
            }

            List<ApuracaoIndice> batch = new ArrayList<ApuracaoIndice>(BATCH_SIZE);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1),
                    READER_BUFFER_SIZE)) {
                String line;
                log.info("Processing COTAHIST file at {}", filePath);
                long fileStartedAt = System.currentTimeMillis();
                while ((line = reader.readLine()) != null) {
                    ApuracaoIndice apuracao = parseLine(line);
                    if (apuracao == null) {
                        continue;
                    }
                    batch.add(apuracao);
                    if (batch.size() >= BATCH_SIZE) {
                        flushBatch(batch);
                        inserted += batch.size();
                        batch.clear();
                        log.info("Processed {} apuracao_indice from {} ({} ms)",
                            inserted, filePath, System.currentTimeMillis() - fileStartedAt);
                    }
                }
                if (!batch.isEmpty()) {
                    flushBatch(batch);
                    inserted += batch.size();
                    batch.clear();
                }
                log.info("Process finished COTAHIST file at {}: {} apuracao_indice in {} ms",
                    filePath, inserted, System.currentTimeMillis() - fileStartedAt);
            }
        } catch (Exception e) {
            log.error("Error processing COTAHIST file", e);
        }
        return inserted;
    }

    private void loadCaches() {
        indiceByTicker.clear();
        tipoInvestimentoIndiceIds.clear();
        for (Indice indice : indiceRepository.findAll()) {
            if (indice.getDescricao() != null) {
                indiceByTicker.put(indice.getDescricao(), indice);
            }
        }
        for (TipoInvestimento tipo : tipoInvestimentoRepository.findAll()) {
            if (tipo.getCodIndice() != null) {
                tipoInvestimentoIndiceIds.add(tipo.getCodIndice());
            }
        }
        log.info("Loaded {} indices and {} tipo_investimento into cache",
            indiceByTicker.size(), tipoInvestimentoIndiceIds.size());
    }

    private ApuracaoIndice parseLine(String line) {
        try {
            if (line.length() < 27) {
                return null; // too short
            }

            // positions are 1-based in the request; convert to 0-based substring indices
            // chars 24-27 inclusive => substring(23, 27)
            String typeCode = safeSubstring(line, 24, 27).trim();
            if (!"010".equals(typeCode) && !"020".equals(typeCode)) {
                return null;
            }

            String specificationCode = safeSubstring(line, 39, 49).trim();
            if (!specificationCode.startsWith("ON") && !specificationCode.startsWith("PN")) {
                return null;
            }

            // ticker at chars 12-17 inclusive => substring(11, 17)
            String ticker = safeSubstring(line, 12, 24).trim();
            if (ticker.isEmpty()) {
                return null;
            }

            Indice savedIndice = getOrCreateIndice(ticker);

            String descricao = safeSubstring(line, 27, 39).trim();
            ensureTipoInvestimento(savedIndice, ticker, descricao);

            String year = safeSubstring(line, 2, 6).trim();
            String month = safeSubstring(line, 6, 8).trim();
            String day = safeSubstring(line, 8, 10).trim();
            LocalDateTime dataApuracao = LocalDateTime.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day), 0, 0);

            BigDecimal valorAbertura = new BigDecimal(safeSubstring(line, 56, 69).trim());
            BigDecimal valorMaximo = new BigDecimal(safeSubstring(line, 69, 82).trim());
            BigDecimal valorMinimo = new BigDecimal(safeSubstring(line, 82, 95).trim());
            BigDecimal valorFechamento = new BigDecimal(safeSubstring(line, 108, 121).trim());
            Integer numeroDistribuicao = Integer.parseInt(safeSubstring(line, 242, 245).trim());

            ApuracaoIndice apuracaoIndice = new ApuracaoIndice();
            apuracaoIndice.setIndice(savedIndice);
            apuracaoIndice.setDataApuracao(dataApuracao);
            apuracaoIndice.setValorAbertura(valorAbertura);
            apuracaoIndice.setValorMaximo(valorMaximo);
            apuracaoIndice.setValorMinimo(valorMinimo);
            apuracaoIndice.setValorFechamento(valorFechamento);
            apuracaoIndice.setNumeroDistribuicao(numeroDistribuicao);
            return apuracaoIndice;
        } catch (Exception ex) {
            log.warn("Failed to process line: {}", line, ex);
            return null;
        }
    }

    private Indice getOrCreateIndice(String ticker) {
        Indice cached = indiceByTicker.get(ticker);
        if (cached != null) {
            return cached;
        }
        Indice indice = new Indice();
        indice.setDescricao(ticker);
        indice.setTipoIndice("F");
        indice.setPeriodicidade("D");
        Indice saved = indiceRepository.save(indice);
        indiceByTicker.put(ticker, saved);
        return saved;
    }

    private void ensureTipoInvestimento(Indice savedIndice, String ticker, String descricao) {
        if (tipoInvestimentoIndiceIds.contains(savedIndice.getCodIndice())) {
            return;
        }
        TipoInvestimento tipoInvestimento = new TipoInvestimento();
        tipoInvestimento.setIndice(savedIndice);
        tipoInvestimento.setDescricao(ticker + " - " + descricao);
        tipoInvestimento.setIncideIof("N");
        tipoInvestimento.setIncideIr("A");
        tipoInvestimentoRepository.save(tipoInvestimento);
        tipoInvestimentoIndiceIds.add(savedIndice.getCodIndice());
    }

    private void flushBatch(final List<ApuracaoIndice> batch) {
        final List<Object[]> args = new ArrayList<Object[]>(batch.size());
        for (ApuracaoIndice apuracao : batch) {
            args.add(new Object[] {
                Timestamp.valueOf(apuracao.getDataApuracao()),
                apuracao.getCodIndice(),
                apuracao.getValorAbertura(),
                apuracao.getValorMaximo(),
                apuracao.getValorMinimo(),
                apuracao.getValorFechamento(),
                apuracao.getNumeroDistribuicao()
            });
        }
        transactionTemplate.execute(status -> {
            jdbcTemplate.batchUpdate(INSERT_APURACAO_SQL, args);
            return null;
        });
    }

    private String safeSubstring(String s, int begin, int endExclusive) {
        int len = s.length();
        int start = Math.max(0, Math.min(begin, len));
        int end = Math.max(start, Math.min(endExclusive, len));
        return s.substring(start, end);
    }
}
