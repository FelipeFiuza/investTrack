package com.bezkoder.spring.datajpa.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bezkoder.spring.datajpa.dto.CotacaoDiaDTO;
import com.bezkoder.spring.datajpa.dto.PanelCreateDTO;
import com.bezkoder.spring.datajpa.dto.PanelDTO;
import com.bezkoder.spring.datajpa.dto.PanelPosicaoResponse;
import com.bezkoder.spring.datajpa.dto.PanelUpdateDTO;
import com.bezkoder.spring.datajpa.dto.InvestimentoSerieDTO;
import com.bezkoder.spring.datajpa.dto.TransacaoSerieDTO;
import com.bezkoder.spring.datajpa.model.Base;
import com.bezkoder.spring.datajpa.model.ExibicaoTransacoes;
import com.bezkoder.spring.datajpa.model.Metrica;
import com.bezkoder.spring.datajpa.model.Panel;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.model.Transacao;
import com.bezkoder.spring.datajpa.model.Usuario;
import com.bezkoder.spring.datajpa.repository.PanelRepository;
import com.bezkoder.spring.datajpa.repository.TransacaoRepository;
import com.bezkoder.spring.datajpa.repository.UsuarioRepository;

@Service
public class PanelService {

    private static final Logger log = LoggerFactory.getLogger(PanelService.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private PanelRepository panelRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void dropLegacyUsuarioUnique() {
        try {
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                    "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'panel' "
                            + "AND COLUMN_NAME = 'id_usuario' AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'");
            for (Map<String, Object> row : indexes) {
                Object name = row.get("INDEX_NAME");
                if (name == null) {
                    continue;
                }
                jdbcTemplate.execute("ALTER TABLE panel DROP INDEX `" + name.toString().replace("`", "") + "`");
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel remover indice unico de panel.id_usuario: {}", e.getMessage());
        }
    }

    @Transactional
    public List<PanelDTO> listByUsuario(Long idUsuario) {
        int totalTransacoes = (int) transacaoRepository.countByUsuario_IdUsuario(idUsuario);
        List<PanelDTO> result = new ArrayList<PanelDTO>();
        List<Panel> panels = panelRepository.findByUsuario_IdUsuarioOrderByIdPanelAsc(idUsuario);
        for (Panel panel : panels) {
            result.add(toDto(panel, totalTransacoes));
        }
        return result;
    }

    @Transactional
    public PanelDTO getById(Long id) {
        Panel panel = panelRepository.findById(id).orElse(null);
        if (panel == null) {
            return null;
        }
        Long idUsuario = panel.getUsuario() != null ? panel.getUsuario().getIdUsuario() : null;
        int totalTransacoes = idUsuario == null ? 0 : (int) transacaoRepository.countByUsuario_IdUsuario(idUsuario);
        return toDto(panel, totalTransacoes);
    }

    @Transactional
    public PanelDTO create(PanelCreateDTO dto) {
        if (dto == null || dto.getIdUsuario() == null) {
            throw new IllegalArgumentException("idUsuario e obrigatorio");
        }
        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario()).orElse(null);
        if (usuario == null) {
            return null;
        }
        Panel panel = new Panel();
        panel.setUsuario(usuario);
        panel.setDescricao("Novo Painel");
        panel.setBase(Base.BRUTO);
        panel.setMetrica(Metrica.VALOR);
        panel.setExibicaoTransacoes(ExibicaoTransacoes.INDIVIDUAIS);
        panel.setFiltroTransacoes(false);
        Panel saved = panelRepository.save(panel);
        int totalTransacoes = (int) transacaoRepository.countByUsuario_IdUsuario(dto.getIdUsuario());
        return toDto(saved, totalTransacoes);
    }

    @Transactional
    public void unlinkTransacao(Long idTransacao) {
        if (idTransacao == null) {
            return;
        }
        panelRepository.deleteLinksByTransacao(idTransacao);
    }

    @Transactional
    public PanelDTO update(Long id, PanelUpdateDTO dto) {
        if (dto == null || dto.getBase() == null || dto.getMetrica() == null) {
            throw new IllegalArgumentException("base e metrica sao obrigatorias");
        }
        Panel panel = panelRepository.findById(id).orElse(null);
        if (panel == null) {
            return null;
        }
        if (dto.getDescricao() != null) {
            panel.setDescricao(dto.getDescricao().trim());
        }
        panel.setBase(dto.getBase());
        panel.setMetrica(dto.getMetrica());
        panel.setExibicaoTransacoes(dto.getExibicaoTransacoes() != null
                ? dto.getExibicaoTransacoes()
                : ExibicaoTransacoes.INDIVIDUAIS);
        panel.setFiltroTransacoes(dto.isFiltroTransacoes());
        panel.getTransacoes().clear();
        if (dto.isFiltroTransacoes()) {
            panel.getTransacoes().addAll(transacoesDoUsuario(panel, dto.getIdsTransacao()));
        }
        Panel saved = panelRepository.save(panel);
        Long idUsuario = saved.getUsuario() != null ? saved.getUsuario().getIdUsuario() : null;
        int totalTransacoes = idUsuario == null ? 0 : (int) transacaoRepository.countByUsuario_IdUsuario(idUsuario);
        return toDto(saved, totalTransacoes);
    }

    @Transactional
    public boolean delete(Long id) {
        Panel panel = panelRepository.findById(id).orElse(null);
        if (panel == null) {
            return false;
        }
        panel.getTransacoes().clear();
        panelRepository.saveAndFlush(panel);
        clearDashboardReferences(id);
        panelRepository.delete(panel);
        return true;
    }

    private void clearDashboardReferences(Long idPanel) {
        jdbcTemplate.update("UPDATE dashboard SET id_panel_esquerda_superior = NULL WHERE id_panel_esquerda_superior = ?", idPanel);
        jdbcTemplate.update("UPDATE dashboard SET id_panel_direita_superior = NULL WHERE id_panel_direita_superior = ?", idPanel);
        jdbcTemplate.update("UPDATE dashboard SET id_panel_esquerda_inferior = NULL WHERE id_panel_esquerda_inferior = ?", idPanel);
        jdbcTemplate.update("UPDATE dashboard SET id_panel_direita_inferior = NULL WHERE id_panel_direita_inferior = ?", idPanel);
    }

    public PanelPosicaoResponse getPosicoes(Long idUsuario, LocalDate inicio, LocalDate fim,
            List<Long> idsExcluidos) {
        if (fim == null) {
            fim = LocalDate.now();
        }
        if (inicio == null) {
            inicio = fim.minusMonths(24);
        }
        if (inicio.isAfter(fim)) {
            LocalDate swap = inicio;
            inicio = fim;
            fim = swap;
        }

        Set<Long> excluded = toExcludedSet(idsExcluidos);
        Map<Long, TipoInvestimento> ativos = new LinkedHashMap<Long, TipoInvestimento>();
        Map<Long, List<Transacao>> txsPorInvestimento = new HashMap<Long, List<Transacao>>();
        List<Transacao> transacoes = transacaoRepository.findByUsuario_IdUsuario(idUsuario);
        for (Transacao transacao : transacoes) {
            if (transacao.getIdTransacao() != null && excluded.contains(transacao.getIdTransacao())) {
                continue;
            }
            if (transacao.getInvestimento() == null || transacao.getInvestimento().getCodInvestimento() == null) {
                continue;
            }
            if (transacao.getDataTransacao() == null
                    || transacao.getDataTransacao().toLocalDate().isAfter(fim)) {
                continue;
            }
            Long codInvestimento = transacao.getInvestimento().getCodInvestimento();
            ativos.put(codInvestimento, transacao.getInvestimento());
            List<Transacao> doAtivo = txsPorInvestimento.get(codInvestimento);
            if (doAtivo == null) {
                doAtivo = new ArrayList<Transacao>();
                txsPorInvestimento.put(codInvestimento, doAtivo);
            }
            doAtivo.add(transacao);
        }

        Set<Long> codigosIndice = new LinkedHashSet<Long>();
        for (TipoInvestimento tipo : ativos.values()) {
            if (tipo.getCodIndice() != null) {
                codigosIndice.add(tipo.getCodIndice());
            }
        }
        Map<Long, List<CotacaoDiaDTO>> cotacoes = loadCotacoes(
                inicio, inicioCotacaoFixa(inicio, ativos, txsPorInvestimento), fim,
                new ArrayList<Long>(codigosIndice));

        List<InvestimentoSerieDTO> series = new ArrayList<InvestimentoSerieDTO>();
        for (Map.Entry<Long, TipoInvestimento> entry : ativos.entrySet()) {
            Long codInvestimento = entry.getKey();
            TipoInvestimento tipo = entry.getValue();
            String descricao = tipo != null && tipo.getDescricao() != null
                    ? tipo.getDescricao()
                    : ("Investimento " + codInvestimento);
            List<Transacao> doAtivo = txsPorInvestimento.get(codInvestimento);
            List<CotacaoDiaDTO> cotacoesDoAtivo = tipo != null && tipo.getCodIndice() != null
                    ? cotacoes.get(tipo.getCodIndice())
                    : null;
            if (cotacoesDoAtivo == null) {
                cotacoesDoAtivo = new ArrayList<CotacaoDiaDTO>();
            }
            series.add(new InvestimentoSerieDTO(
                    codInvestimento,
                    descricao,
                    toTransacoesSerie(doAtivo),
                    cotacoesDoAtivo));
        }

        return new PanelPosicaoResponse(inicio, fim, series);
    }

    private Set<Transacao> transacoesDoUsuario(Panel panel, List<Long> ids) {
        Set<Transacao> selected = new HashSet<Transacao>();
        if (ids == null || ids.isEmpty() || panel.getUsuario() == null) {
            return selected;
        }
        Long idUsuario = panel.getUsuario().getIdUsuario();
        Set<Long> wanted = new HashSet<Long>();
        for (Long idTransacao : ids) {
            if (idTransacao != null) {
                wanted.add(idTransacao);
            }
        }
        List<Transacao> owned = transacaoRepository.findByUsuario_IdUsuario(idUsuario);
        for (Transacao transacao : owned) {
            if (transacao.getIdTransacao() != null && wanted.contains(transacao.getIdTransacao())) {
                selected.add(transacao);
            }
        }
        return selected;
    }

    private PanelDTO toDto(Panel panel, int totalTransacoesUsuario) {
        PanelDTO dto = new PanelDTO();
        dto.setIdPanel(panel.getIdPanel());
        dto.setIdUsuario(panel.getUsuario() != null ? panel.getUsuario().getIdUsuario() : null);
        dto.setDescricao(panel.getDescricao());
        dto.setBase(panel.getBase());
        dto.setMetrica(panel.getMetrica());
        dto.setExibicaoTransacoes(panel.getExibicaoTransacoes() != null
                ? panel.getExibicaoTransacoes()
                : ExibicaoTransacoes.INDIVIDUAIS);
        dto.setFiltroTransacoes(panel.isFiltroTransacoes());
        List<Long> ids = new ArrayList<Long>();
        if (panel.getTransacoes() != null) {
            for (Transacao transacao : panel.getTransacoes()) {
                if (transacao.getIdTransacao() != null) {
                    ids.add(transacao.getIdTransacao());
                }
            }
        }
        dto.setIdsTransacao(ids);
        dto.setQuantidadeTransacoes(panel.isFiltroTransacoes() ? ids.size() : totalTransacoesUsuario);
        return dto;
    }

    private List<TransacaoSerieDTO> toTransacoesSerie(List<Transacao> transacoes) {
        List<Transacao> ordenadas = new ArrayList<Transacao>();
        if (transacoes != null) {
            ordenadas.addAll(transacoes);
        }
        Collections.sort(ordenadas, new Comparator<Transacao>() {
            @Override
            public int compare(Transacao a, Transacao b) {
                int porData = a.getDataTransacao().compareTo(b.getDataTransacao());
                if (porData != 0) {
                    return porData;
                }
                long idA = a.getIdTransacao() == null ? 0L : a.getIdTransacao().longValue();
                long idB = b.getIdTransacao() == null ? 0L : b.getIdTransacao().longValue();
                return Long.compare(idA, idB);
            }
        });
        List<TransacaoSerieDTO> dtos = new ArrayList<TransacaoSerieDTO>();
        for (Transacao transacao : ordenadas) {
            TransacaoSerieDTO dto = new TransacaoSerieDTO();
            dto.setIdTransacao(transacao.getIdTransacao());
            dto.setDataTransacao(transacao.getDataTransacao());
            dto.setTipoTransacao(transacao.getTipoTransacao());
            dto.setQuantidade(transacao.getQuantidade());
            dto.setValorTotal(transacao.getValorTotal());
            dto.setValorUnitario(transacao.getValorUnitario());
            dto.setTaxasImpostos(transacao.getTaxasImpostos());
            dtos.add(dto);
        }
        return dtos;
    }

    private LocalDate inicioCotacaoFixa(LocalDate inicio, Map<Long, TipoInvestimento> ativos,
            Map<Long, List<Transacao>> txsPorInvestimento) {
        LocalDate inicioFixa = inicio;
        for (Map.Entry<Long, List<Transacao>> entry : txsPorInvestimento.entrySet()) {
            if (!rendimentoCdi(ativos.get(entry.getKey()))) {
                continue;
            }
            for (Transacao transacao : entry.getValue()) {
                if (transacao.getDataTransacao() == null) {
                    continue;
                }
                LocalDate dia = transacao.getDataTransacao().toLocalDate();
                if (dia.isBefore(inicioFixa)) {
                    inicioFixa = dia;
                }
            }
        }
        return inicioFixa;
    }

    private boolean rendimentoCdi(TipoInvestimento tipo) {
        if (tipo == null || tipo.getDescricao() == null) {
            return false;
        }
        String nome = tipo.getDescricao().trim().toUpperCase();
        return nome.startsWith("LCA ") || nome.startsWith("LCI ") || nome.startsWith("CDB ");
    }

    private Map<Long, List<CotacaoDiaDTO>> loadCotacoes(LocalDate inicio, LocalDate inicioFixa, LocalDate fim,
            List<Long> codigosIndice) {
        Map<Long, Map<LocalDate, BigDecimal>> fechamentos = queryUltimoFechamento("apuracao_indice_fixa", inicioFixa,
                codigosIndice);
        mergeFechamentos(fechamentos, queryUltimoFechamento("apuracao_indice", inicio, codigosIndice));
        mergeFechamentos(fechamentos, queryFechamentos("apuracao_indice_fixa", inicioFixa, fim, codigosIndice));
        Map<Long, Map<LocalDate, BigDecimal>> oficiais = queryFechamentos("apuracao_indice", inicio, fim, codigosIndice);
        mergeFechamentos(fechamentos, oficiais);

        Map<Long, List<CotacaoDiaDTO>> porIndice = new HashMap<Long, List<CotacaoDiaDTO>>();
        for (Map.Entry<Long, Map<LocalDate, BigDecimal>> entry : fechamentos.entrySet()) {
            List<LocalDate> dias = new ArrayList<LocalDate>(entry.getValue().keySet());
            Collections.sort(dias);
            List<CotacaoDiaDTO> cotacoes = new ArrayList<CotacaoDiaDTO>();
            for (LocalDate dia : dias) {
                CotacaoDiaDTO dto = new CotacaoDiaDTO();
                dto.setDay(dia);
                dto.setValorFechamento(entry.getValue().get(dia));
                cotacoes.add(dto);
            }
            porIndice.put(entry.getKey(), cotacoes);
        }
        return porIndice;
    }

    private void mergeFechamentos(Map<Long, Map<LocalDate, BigDecimal>> destino,
            Map<Long, Map<LocalDate, BigDecimal>> origem) {
        for (Map.Entry<Long, Map<LocalDate, BigDecimal>> entry : origem.entrySet()) {
            Map<LocalDate, BigDecimal> atual = destino.get(entry.getKey());
            if (atual == null) {
                destino.put(entry.getKey(), entry.getValue());
            } else {
                atual.putAll(entry.getValue());
            }
        }
    }

    private Map<Long, Map<LocalDate, BigDecimal>> queryUltimoFechamento(String table, LocalDate inicio,
            List<Long> codigosIndice) {
        if (codigosIndice == null || codigosIndice.isEmpty()) {
            return new HashMap<Long, Map<LocalDate, BigDecimal>>();
        }
        validarTabelaCotacao(table);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.cod_indice, DATE(t.data_apuracao) AS dia, MAX(t.valor_fechamento) AS valor_fechamento FROM ");
        sql.append(table);
        sql.append(" t INNER JOIN (SELECT cod_indice, MAX(DATE(data_apuracao)) AS dia FROM ");
        sql.append(table);
        sql.append(" WHERE data_apuracao < ? AND cod_indice IN (");
        appendInClause(sql, codigosIndice.size());
        sql.append(") GROUP BY cod_indice) u ON u.cod_indice = t.cod_indice AND DATE(t.data_apuracao) = u.dia ");
        sql.append("GROUP BY t.cod_indice, DATE(t.data_apuracao)");

        Object[] args = new Object[codigosIndice.size() + 1];
        args[0] = Timestamp.valueOf(inicio.atStartOfDay());
        for (int i = 0; i < codigosIndice.size(); i++) {
            args[i + 1] = codigosIndice.get(i);
        }
        return jdbcTemplate.query(sql.toString(), args, fechamentoExtractor());
    }

    private Map<Long, Map<LocalDate, BigDecimal>> queryFechamentos(String table, LocalDate inicio, LocalDate fim,
            List<Long> codigosIndice) {
        if (codigosIndice == null || codigosIndice.isEmpty()) {
            return new HashMap<Long, Map<LocalDate, BigDecimal>>();
        }
        validarTabelaCotacao(table);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cod_indice, DATE(data_apuracao) AS dia, MAX(valor_fechamento) AS valor_fechamento FROM ");
        sql.append(table);
        sql.append(" WHERE data_apuracao >= ? AND data_apuracao < ? AND cod_indice IN (");
        appendInClause(sql, codigosIndice.size());
        sql.append(") GROUP BY cod_indice, DATE(data_apuracao)");

        Object[] args = new Object[codigosIndice.size() + 2];
        args[0] = Timestamp.valueOf(inicio.atStartOfDay());
        args[1] = Timestamp.valueOf(fim.plusDays(1).atStartOfDay());
        for (int i = 0; i < codigosIndice.size(); i++) {
            args[i + 2] = codigosIndice.get(i);
        }

        return jdbcTemplate.query(sql.toString(), args, fechamentoExtractor());
    }

    private void validarTabelaCotacao(String table) {
        if (!"apuracao_indice".equals(table) && !"apuracao_indice_fixa".equals(table)) {
            throw new IllegalArgumentException("Tabela de cotacao invalida");
        }
    }

    private void appendInClause(StringBuilder sql, int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
        }
    }

    private ResultSetExtractor<Map<Long, Map<LocalDate, BigDecimal>>> fechamentoExtractor() {
        return new ResultSetExtractor<Map<Long, Map<LocalDate, BigDecimal>>>() {
            @Override
            public Map<Long, Map<LocalDate, BigDecimal>> extractData(ResultSet rs) throws SQLException {
                Map<Long, Map<LocalDate, BigDecimal>> map = new HashMap<Long, Map<LocalDate, BigDecimal>>();
                while (rs.next()) {
                    long codIndice = rs.getLong("cod_indice");
                    if (rs.wasNull()) {
                        continue;
                    }
                    java.sql.Date dia = rs.getDate("dia");
                    BigDecimal valor = rs.getBigDecimal("valor_fechamento");
                    if (dia == null || valor == null) {
                        continue;
                    }
                    Map<LocalDate, BigDecimal> porDia = map.get(Long.valueOf(codIndice));
                    if (porDia == null) {
                        porDia = new HashMap<LocalDate, BigDecimal>();
                        map.put(Long.valueOf(codIndice), porDia);
                    }
                    porDia.put(dia.toLocalDate(), valor);
                }
                return map;
            }
        };
    }

    private Set<Long> toExcludedSet(List<Long> idsExcluidos) {
        Set<Long> excluded = new HashSet<Long>();
        if (idsExcluidos == null) {
            return excluded;
        }
        for (Long id : idsExcluidos) {
            if (id != null) {
                excluded.add(id);
            }
        }
        return excluded;
    }
}
