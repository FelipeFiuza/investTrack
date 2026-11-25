package com.bezkoder.spring.datajpa.service;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;
import com.bezkoder.spring.datajpa.repository.TipoInvestimentoRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Service
@Order(0)
public class ApuracaoIndiceFixaIngestionJob implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ApuracaoIndiceFixaIngestionJob.class);

    static final LocalDate DATA_INICIAL = LocalDate.of(2021, 1, 1);
    private static final DateTimeFormatter BCB_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TIPO_RENDA_FIXA = "R";
    private static final String PERIODICIDADE_DIARIA = "D";
    private static final String PERIODICIDADE_MENSAL = "M";
    private static final int BATCH_SIZE = 500;
    private static final int VALOR_SCALE = 8;

    private static final String INSERT_SQL =
        "INSERT INTO apuracao_indice_fixa (data_apuracao, cod_indice, valor_fechamento) " +
        "VALUES (?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE valor_fechamento = VALUES(valor_fechamento)";

    private static final List<SerieFixa> SERIES = Arrays.asList(
        new SerieFixa("CDI", 12, PERIODICIDADE_DIARIA),
        new SerieFixa("IPCA", 433, PERIODICIDADE_MENSAL),
        new SerieFixa("IGP-M", 189, PERIODICIDADE_MENSAL),
        new SerieFixa("TR", 226, PERIODICIDADE_DIARIA),
        new SerieFixa("Selic", 11, PERIODICIDADE_DIARIA),
        new SerieFixa("INPC", 188, PERIODICIDADE_MENSAL)
    );

    private static final List<TipoCdi> TIPOS_CDI = Arrays.asList(
        new TipoCdi("LCA CDI (Ex: 100% CDI)", "S", "N"),
        new TipoCdi("LCI CDI (Ex: 100% CDI)", "S", "N"),
        new TipoCdi("CDB CDI (Ex: 100% CDI)", "S", "S")
    );

    @Autowired
    private IndiceRepository indiceRepository;

    @Autowired
    private TipoInvestimentoRepository tipoInvestimentoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private RestTemplate restTemplate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public void run(String... args) {
        sincronizar();
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "America/Sao_Paulo")
    public void sincronizarAgendado() {
        sincronizar();
    }

    public void sincronizar() {
        if (!running.compareAndSet(false, true)) {
            log.info("Sincronizacao de indices de renda fixa ja em andamento");
            return;
        }
        long startedAt = System.currentTimeMillis();
        try {
            garantirEscalaValor();
            LocalDate hoje = LocalDate.now();
            for (SerieFixa serie : SERIES) {
                try {
                    completarSerie(serie, hoje);
                } catch (Exception e) {
                    log.error("Falha ao sincronizar {}", serie.descricao, e);
                }
            }
            garantirTiposCdi();
            log.info("Sincronizacao de indices de renda fixa concluida em {} ms",
                System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            log.error("Falha na sincronizacao de indices de renda fixa", e);
        } finally {
            running.set(false);
        }
    }

    private void garantirEscalaValor() {
        Number scale = jdbcTemplate.queryForObject(
            "SELECT NUMERIC_SCALE FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'apuracao_indice_fixa' " +
            "AND COLUMN_NAME = 'valor_fechamento'",
            Number.class);
        if (scale != null && scale.intValue() >= VALOR_SCALE) {
            return;
        }
        jdbcTemplate.execute(
            "ALTER TABLE apuracao_indice_fixa MODIFY valor_fechamento DECIMAL(19," + VALOR_SCALE + ") NULL");
        log.info("Coluna apuracao_indice_fixa.valor_fechamento ajustada para DECIMAL(19,{})", VALOR_SCALE);
    }

    private void completarSerie(SerieFixa serie, LocalDate hoje) {
        Indice indice = ensureIndice(serie);
        LocalDate min = dataLimite(indice.getCodIndice(), true);
        LocalDate max = dataLimite(indice.getCodIndice(), false);
        int gravados = 0;

        boolean interrompido = false;
        if (min == null || max == null) {
            log.info("{} sem apuracoes; buscando de {} a {}", serie.descricao, DATA_INICIAL, hoje);
            Carga carga = preencherParaFrente(indice, serie, DATA_INICIAL, hoje);
            gravados += carga.gravados;
            interrompido = carga.interrompida;
        } else {
            log.info("{} ja possui apuracoes de {} a {}", serie.descricao, min, max);
            if (min.isAfter(DATA_INICIAL)) {
                Carga carga = preencherParaTras(indice, serie, DATA_INICIAL, min.minusDays(1));
                gravados += carga.gravados;
                interrompido = carga.interrompida;
            }
            if (max.isBefore(hoje)) {
                Carga carga = preencherParaFrente(indice, serie, max.plusDays(1), hoje);
                gravados += carga.gravados;
                interrompido = interrompido || carga.interrompida;
            }
        }

        if (interrompido) {
            log.warn("{} ficou incompleto; {} apuracoes gravadas antes da interrupcao", serie.descricao, gravados);
        } else if (gravados == 0) {
            log.info("{} sem periodos faltantes ate {}", serie.descricao, hoje);
        } else {
            log.info("{}: {} apuracoes gravadas", serie.descricao, gravados);
        }
    }

    private Carga preencherParaFrente(Indice indice, SerieFixa serie, LocalDate inicio, LocalDate fim) {
        Carga carga = new Carga();
        for (Periodo periodo : dividirPorAno(inicio, fim)) {
            List<Object[]> linhas = buscarLinhas(indice, serie, periodo);
            if (linhas == null) {
                carga.interrompida = true;
                log.warn("{} interrompido em {} a {}; o restante sera buscado na proxima execucao",
                    serie.descricao, periodo.inicio, periodo.fim);
                break;
            }
            carga.gravados += persistir(linhas);
        }
        return carga;
    }

    private Carga preencherParaTras(Indice indice, SerieFixa serie, LocalDate inicio, LocalDate fim) {
        List<Periodo> periodos = dividirPorAno(inicio, fim);
        Carga carga = new Carga();
        for (int i = periodos.size() - 1; i >= 0; i--) {
            Periodo periodo = periodos.get(i);
            List<Object[]> linhas = buscarLinhas(indice, serie, periodo);
            if (linhas == null) {
                carga.interrompida = true;
                log.warn("{} interrompido no preenchimento anterior a {}; o restante sera buscado na proxima execucao",
                    serie.descricao, periodo.fim.plusDays(1));
                break;
            }
            carga.gravados += persistir(linhas);
        }
        return carga;
    }

    private List<Object[]> buscarLinhas(Indice indice, SerieFixa serie, Periodo periodo) {
        if (periodo.inicio.isAfter(periodo.fim)) {
            return Collections.emptyList();
        }
        List<BcbPonto> pontos;
        try {
            pontos = buscarSerie(serie.codigoSgs, periodo.inicio, periodo.fim);
        } catch (RestClientException e) {
            log.error("Falha ao consultar SGS {} ({}) de {} a {}",
                serie.codigoSgs, serie.descricao, periodo.inicio, periodo.fim, e);
            return null;
        }
        List<Object[]> linhas = new ArrayList<Object[]>(pontos.size());
        for (BcbPonto ponto : pontos) {
            Object[] linha = paraLinha(indice.getCodIndice(), ponto);
            if (linha != null) {
                linhas.add(linha);
            }
        }
        log.info("{} SGS {}: {} a {} retornou {} pontos",
            serie.descricao, serie.codigoSgs, periodo.inicio, periodo.fim, linhas.size());
        return linhas;
    }

    private List<BcbPonto> buscarSerie(int codigoSgs, LocalDate inicio, LocalDate fim) {
        URI uri = UriComponentsBuilder
            .fromHttpUrl("https://api.bcb.gov.br/dados/serie/bcdata.sgs." + codigoSgs + "/dados")
            .queryParam("formato", "json")
            .queryParam("dataInicial", inicio.format(BCB_DATE))
            .queryParam("dataFinal", fim.format(BCB_DATE))
            .build()
            .encode()
            .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "investTrack/1.0");

        try {
            List<BcbPonto> body = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                new ParameterizedTypeReference<List<BcbPonto>>() {
                }).getBody();
            return body == null ? Collections.<BcbPonto>emptyList() : body;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    private Object[] paraLinha(Long codIndice, BcbPonto ponto) {
        if (ponto.getData() == null || ponto.getValor() == null) {
            return null;
        }
        LocalDate data;
        try {
            data = LocalDate.parse(ponto.getData().trim(), BCB_DATE);
        } catch (DateTimeParseException e) {
            log.warn("Data invalida recebida do BCB: {}", ponto.getData());
            return null;
        }
        if (data.isBefore(DATA_INICIAL)) {
            return null;
        }
        BigDecimal valor = parseValor(ponto.getValor());
        if (valor == null) {
            return null;
        }
        return new Object[] { Timestamp.valueOf(data.atStartOfDay()), codIndice, valor };
    }

    private BigDecimal parseValor(String valor) {
        String normalizado = valor.trim().replace(',', '.');
        if (normalizado.isEmpty() || "-".equals(normalizado)) {
            return null;
        }
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            log.warn("Valor invalido recebido do BCB: {}", valor);
            return null;
        }
    }

    private int persistir(List<Object[]> linhas) {
        if (linhas.isEmpty()) {
            return 0;
        }
        int gravados = 0;
        for (int offset = 0; offset < linhas.size(); offset += BATCH_SIZE) {
            int fim = Math.min(offset + BATCH_SIZE, linhas.size());
            final List<Object[]> lote = linhas.subList(offset, fim);
            transactionTemplate.execute(status -> {
                jdbcTemplate.batchUpdate(INSERT_SQL, lote);
                return null;
            });
            gravados += lote.size();
        }
        return gravados;
    }

    private LocalDate dataLimite(Long codIndice, boolean minima) {
        String funcao = minima ? "MIN" : "MAX";
        List<Timestamp> limites = jdbcTemplate.query(
            "SELECT " + funcao + "(data_apuracao) FROM apuracao_indice_fixa WHERE cod_indice = ?",
            (rs, rowNum) -> rs.getTimestamp(1),
            codIndice);
        if (limites.isEmpty() || limites.get(0) == null) {
            return null;
        }
        return limites.get(0).toLocalDateTime().toLocalDate();
    }

    private void garantirTiposCdi() {
        List<Indice> cdis = indiceRepository.findByDescricaoAndTipoIndice("CDI", TIPO_RENDA_FIXA);
        if (cdis.isEmpty()) {
            log.warn("Indice CDI nao encontrado; tipos de investimento nao foram criados");
            return;
        }
        Indice cdi = cdis.get(0);
        for (TipoCdi tipo : TIPOS_CDI) {
            ensureTipoInvestimento(cdi, tipo);
        }
    }

    private void ensureTipoInvestimento(Indice cdi, TipoCdi tipo) {
        List<TipoInvestimento> existentes = tipoInvestimentoRepository.findByDescricao(tipo.descricao);
        if (!existentes.isEmpty()) {
            return;
        }
        TipoInvestimento investimento = new TipoInvestimento();
        investimento.setDescricao(tipo.descricao);
        investimento.setIncideIof(tipo.incideIof);
        investimento.setIncideIr(tipo.incideIr);
        investimento.setIndice(cdi);
        TipoInvestimento saved = tipoInvestimentoRepository.save(investimento);
        log.info("Tipo de investimento criado: {} (cod {}, IOF {}, IR {})",
            saved.getDescricao(), saved.getCodInvestimento(), tipo.incideIof, tipo.incideIr);
    }

    private Indice ensureIndice(SerieFixa serie) {
        List<Indice> existentes = indiceRepository.findByDescricaoAndTipoIndice(serie.descricao, TIPO_RENDA_FIXA);
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        Indice indice = new Indice(serie.descricao, serie.periodicidade, TIPO_RENDA_FIXA);
        Indice saved = indiceRepository.save(indice);
        log.info("Indice de renda fixa criado: {} (cod {}, SGS {}, periodicidade {})",
            saved.getDescricao(), saved.getCodIndice(), serie.codigoSgs, serie.periodicidade);
        return saved;
    }

    static List<Periodo> dividirPorAno(LocalDate inicio, LocalDate fim) {
        List<Periodo> fatias = new ArrayList<Periodo>();
        if (inicio.isAfter(fim)) {
            return fatias;
        }
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            LocalDate fimDoAno = LocalDate.of(cursor.getYear(), 12, 31);
            if (fimDoAno.isAfter(fim)) {
                fimDoAno = fim;
            }
            fatias.add(new Periodo(cursor, fimDoAno));
            cursor = fimDoAno.plusDays(1);
        }
        return fatias;
    }

    private static final class Carga {
        private int gravados;
        private boolean interrompida;
    }

    private static final class TipoCdi {
        private final String descricao;
        private final String incideIof;
        private final String incideIr;

        private TipoCdi(String descricao, String incideIof, String incideIr) {
            this.descricao = descricao;
            this.incideIof = incideIof;
            this.incideIr = incideIr;
        }
    }

    private static final class SerieFixa {
        private final String descricao;
        private final int codigoSgs;
        private final String periodicidade;

        private SerieFixa(String descricao, int codigoSgs, String periodicidade) {
            this.descricao = descricao;
            this.codigoSgs = codigoSgs;
            this.periodicidade = periodicidade;
        }
    }

    static final class Periodo {
        private final LocalDate inicio;
        private final LocalDate fim;

        private Periodo(LocalDate inicio, LocalDate fim) {
            this.inicio = inicio;
            this.fim = fim;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BcbPonto {
        private String data;
        private String valor;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }
    }
}
