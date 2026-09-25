package com.bezkoder.spring.datajpa.service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bezkoder.spring.datajpa.dto.PanelCreateDTO;
import com.bezkoder.spring.datajpa.dto.PanelDTO;
import com.bezkoder.spring.datajpa.dto.PanelPosicaoResponse;
import com.bezkoder.spring.datajpa.dto.PanelUpdateDTO;
import com.bezkoder.spring.datajpa.dto.InvestimentoSerieDTO;
import com.bezkoder.spring.datajpa.dto.PosicaoDiariaDTO;
import com.bezkoder.spring.datajpa.model.Base;
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
        List<PanelDTO> result = new ArrayList<PanelDTO>();
        List<Panel> panels = panelRepository.findByUsuario_IdUsuarioOrderByIdPanelAsc(idUsuario);
        for (Panel panel : panels) {
            result.add(toDto(panel));
        }
        return result;
    }

    @Transactional
    public PanelDTO getById(Long id) {
        Panel panel = panelRepository.findById(id).orElse(null);
        return panel == null ? null : toDto(panel);
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
        panel.setDescricao(normalizeDescricao(dto.getDescricao()));
        panel.setBase(Base.LIQUIDO);
        panel.setMetrica(Metrica.VALOR);
        panel.setFiltroTransacoes(false);
        return toDto(panelRepository.save(panel));
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
        panel.setFiltroTransacoes(dto.isFiltroTransacoes());
        panel.getTransacoes().clear();
        if (dto.isFiltroTransacoes()) {
            panel.getTransacoes().addAll(transacoesDoUsuario(panel, dto.getIdsTransacao()));
        }
        return toDto(panelRepository.save(panel));
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
        String excludedCsv = toCsv(excluded);
        Map<Long, TipoInvestimento> ativos = findInvestimentosAte(idUsuario, fim, excluded);
        List<InvestimentoSerieDTO> series = new ArrayList<InvestimentoSerieDTO>();

        for (Map.Entry<Long, TipoInvestimento> entry : ativos.entrySet()) {
            Long codInvestimento = entry.getKey();
            TipoInvestimento tipo = entry.getValue();
            try {
                List<PosicaoDiariaDTO> posicoes = queryPosicaoDiaria(idUsuario, codInvestimento, inicio, fim,
                        excludedCsv);
                if (posicoes == null || posicoes.isEmpty()) {
                    continue;
                }
                String descricao = tipo != null && tipo.getDescricao() != null
                    ? tipo.getDescricao()
                    : ("Investimento " + codInvestimento);
                series.add(new InvestimentoSerieDTO(codInvestimento, descricao, posicoes));
            } catch (Exception e) {
                log.error("Falha ao buscar posicao diaria. usuario={}, investimento={}", idUsuario, codInvestimento, e);
            }
        }

        return new PanelPosicaoResponse(inicio, fim, series);
    }

    private String normalizeDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            return "Novo panel";
        }
        return descricao.trim();
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

    private PanelDTO toDto(Panel panel) {
        PanelDTO dto = new PanelDTO();
        dto.setIdPanel(panel.getIdPanel());
        dto.setIdUsuario(panel.getUsuario() != null ? panel.getUsuario().getIdUsuario() : null);
        dto.setDescricao(panel.getDescricao());
        dto.setBase(panel.getBase());
        dto.setMetrica(panel.getMetrica());
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
        return dto;
    }

    private Map<Long, TipoInvestimento> findInvestimentosAte(Long idUsuario, LocalDate dataFim, Set<Long> excluded) {
        Map<Long, TipoInvestimento> byCod = new LinkedHashMap<Long, TipoInvestimento>();
        List<Transacao> transacoes = transacaoRepository.findByUsuario_IdUsuario(idUsuario);
        for (Transacao transacao : transacoes) {
            if (transacao.getIdTransacao() != null && excluded.contains(transacao.getIdTransacao())) {
                continue;
            }
            if (transacao.getInvestimento() == null || transacao.getInvestimento().getCodInvestimento() == null) {
                continue;
            }
            if (transacao.getDataTransacao() != null
                    && transacao.getDataTransacao().toLocalDate().isAfter(dataFim)) {
                continue;
            }
            byCod.put(transacao.getInvestimento().getCodInvestimento(), transacao.getInvestimento());
        }
        return byCod;
    }

    private List<PosicaoDiariaDTO> queryPosicaoDiaria(final Long idUsuario, final Long codInvestimento,
            final LocalDate inicio, final LocalDate fim, final String idsExcluidosCsv) {
        return jdbcTemplate.execute(new ConnectionCallback<List<PosicaoDiariaDTO>>() {
            @Override
            public List<PosicaoDiariaDTO> doInConnection(Connection con) throws SQLException {
                CallableStatement cs = con.prepareCall("{call get_posicao_diaria(?, ?, ?, ?, ?)}");
                try {
                    cs.setLong(1, idUsuario);
                    cs.setLong(2, codInvestimento);
                    cs.setDate(3, java.sql.Date.valueOf(inicio));
                    cs.setDate(4, java.sql.Date.valueOf(fim));
                    cs.setString(5, idsExcluidosCsv == null ? "" : idsExcluidosCsv);

                    boolean hasResult = cs.execute();
                    List<PosicaoDiariaDTO> rows = new ArrayList<PosicaoDiariaDTO>();
                    int rowNum = 0;
                    while (true) {
                        if (hasResult) {
                            ResultSet rs = cs.getResultSet();
                            try {
                                while (rs.next()) {
                                    rows.add(mapPosicao(rs, rowNum++));
                                }
                            } finally {
                                rs.close();
                            }
                            return rows;
                        }
                        if (cs.getUpdateCount() == -1) {
                            break;
                        }
                        hasResult = cs.getMoreResults();
                    }
                    return rows;
                } finally {
                    cs.close();
                }
            }
        });
    }

    private PosicaoDiariaDTO mapPosicao(ResultSet rs, int rowNum) throws SQLException {
        PosicaoDiariaDTO dto = new PosicaoDiariaDTO();
        java.sql.Date day = rs.getDate("day");
        dto.setDay(day != null ? day.toLocalDate() : null);
        dto.setCurrentQty(getDecimal(rs, "current_qty"));
        dto.setCurrentAverageCost(getDecimal(rs, "current_average_cost"));
        dto.setBoughtQty(getDecimal(rs, "bought_qty"));
        dto.setBoughtPrice(getDecimal(rs, "bought_price"));
        dto.setBoughtTaxes(getDecimal(rs, "bought_taxes"));
        dto.setSoldQty(getDecimal(rs, "sold_qty"));
        dto.setSoldPrice(getDecimal(rs, "sold_price"));
        dto.setSoldTaxes(getDecimal(rs, "sold_taxes"));
        dto.setValorFechamento(getDecimal(rs, "valor_fechamento"));
        dto.setGrossTotalAmount(getDecimal(rs, "gross_total_amount"));
        dto.setNetTotalAmount(getDecimal(rs, "net_total_amount"));
        dto.setGrossVariation(getDecimal(rs, "gross_variation"));
        dto.setNetVariation(getDecimal(rs, "net_variation"));
        return dto;
    }

    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return rs.wasNull() ? null : value;
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

    private String toCsv(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Long id : ids) {
            if (!first) {
                sb.append(',');
            }
            sb.append(id);
            first = false;
        }
        return sb.toString();
    }
}
