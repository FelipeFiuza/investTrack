package com.bezkoder.spring.datajpa.service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.datajpa.dto.DashboardPosicaoResponse;
import com.bezkoder.spring.datajpa.dto.InvestimentoSerieDTO;
import com.bezkoder.spring.datajpa.dto.PosicaoDiariaDTO;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.model.Transacao;
import com.bezkoder.spring.datajpa.repository.TransacaoRepository;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DashboardPosicaoResponse getPosicoes(Long idUsuario, LocalDate inicio, LocalDate fim) {
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

        Map<Long, TipoInvestimento> ativos = findInvestimentosAte(idUsuario, fim);
        List<InvestimentoSerieDTO> series = new ArrayList<InvestimentoSerieDTO>();

        for (Map.Entry<Long, TipoInvestimento> entry : ativos.entrySet()) {
            Long codInvestimento = entry.getKey();
            TipoInvestimento tipo = entry.getValue();
            try {
                List<PosicaoDiariaDTO> posicoes = queryPosicaoDiaria(idUsuario, codInvestimento, inicio, fim);
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

        return new DashboardPosicaoResponse(inicio, fim, series);
    }

    private Map<Long, TipoInvestimento> findInvestimentosAte(Long idUsuario, LocalDate dataFim) {
        Map<Long, TipoInvestimento> byCod = new LinkedHashMap<Long, TipoInvestimento>();
        List<Transacao> transacoes = transacaoRepository.findByUsuario_IdUsuario(idUsuario);
        for (Transacao transacao : transacoes) {
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
            final LocalDate inicio, final LocalDate fim) {
        return jdbcTemplate.execute(new ConnectionCallback<List<PosicaoDiariaDTO>>() {
            @Override
            public List<PosicaoDiariaDTO> doInConnection(Connection con) throws SQLException {
                CallableStatement cs = con.prepareCall("{call get_posicao_diaria(?, ?, ?, ?)}");
                try {
                    cs.setLong(1, idUsuario);
                    cs.setLong(2, codInvestimento);
                    cs.setDate(3, java.sql.Date.valueOf(inicio));
                    cs.setDate(4, java.sql.Date.valueOf(fim));

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
}
