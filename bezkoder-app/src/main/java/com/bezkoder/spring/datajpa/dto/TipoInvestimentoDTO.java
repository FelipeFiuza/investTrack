package com.bezkoder.spring.datajpa.dto;

public class TipoInvestimentoDTO {
    private Long codInvestimento;
    private String descricao;
    private String incideIof;
    private String incideIr;

    public TipoInvestimentoDTO() {
    }

    public TipoInvestimentoDTO(Long codInvestimento, String descricao, String incideIof, String incideIr) {
        this.codInvestimento = codInvestimento;
        this.descricao = descricao;
        this.incideIof = incideIof;
        this.incideIr = incideIr;
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

    public String getIncideIof() {
        return incideIof;
    }

    public void setIncideIof(String incideIof) {
        this.incideIof = incideIof;
    }

    public String getIncideIr() {
        return incideIr;
    }

    public void setIncideIr(String incideIr) {
        this.incideIr = incideIr;
    }
}
