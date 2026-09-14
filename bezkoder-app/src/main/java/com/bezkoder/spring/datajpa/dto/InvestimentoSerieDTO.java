package com.bezkoder.spring.datajpa.dto;

import java.util.ArrayList;
import java.util.List;

public class InvestimentoSerieDTO {

    private Long codInvestimento;
    private String descricao;
    private List<PosicaoDiariaDTO> posicoes = new ArrayList<PosicaoDiariaDTO>();

    public InvestimentoSerieDTO() {
    }

    public InvestimentoSerieDTO(Long codInvestimento, String descricao, List<PosicaoDiariaDTO> posicoes) {
        this.codInvestimento = codInvestimento;
        this.descricao = descricao;
        this.posicoes = posicoes;
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

    public List<PosicaoDiariaDTO> getPosicoes() {
        return posicoes;
    }

    public void setPosicoes(List<PosicaoDiariaDTO> posicoes) {
        this.posicoes = posicoes;
    }
}
