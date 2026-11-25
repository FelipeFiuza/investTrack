package com.bezkoder.spring.datajpa.dto;

import java.util.ArrayList;
import java.util.List;

public class InvestimentoSerieDTO {

    private Long codInvestimento;
    private String descricao;
    private List<TransacaoSerieDTO> transacoes = new ArrayList<TransacaoSerieDTO>();
    private List<CotacaoDiaDTO> cotacoes = new ArrayList<CotacaoDiaDTO>();

    public InvestimentoSerieDTO() {
    }

    public InvestimentoSerieDTO(Long codInvestimento, String descricao, List<TransacaoSerieDTO> transacoes,
            List<CotacaoDiaDTO> cotacoes) {
        this.codInvestimento = codInvestimento;
        this.descricao = descricao;
        this.transacoes = transacoes;
        this.cotacoes = cotacoes;
    }

    public Long getCodInvestimento() {
        return codInvestimento;
    }

    public void setCodInvestimento(Long codInvestimento) {
        this.codInvestimento = codInvestimento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<TransacaoSerieDTO> getTransacoes() {
        return transacoes;
    }

    public void setTransacoes(List<TransacaoSerieDTO> transacoes) {
        this.transacoes = transacoes;
    }

    public List<CotacaoDiaDTO> getCotacoes() {
        return cotacoes;
    }

    public void setCotacoes(List<CotacaoDiaDTO> cotacoes) {
        this.cotacoes = cotacoes;
    }
}
