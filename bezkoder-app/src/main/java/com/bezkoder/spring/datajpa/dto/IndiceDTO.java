package com.bezkoder.spring.datajpa.dto;

public class IndiceDTO {
    private Long codIndice;
    private String descricao;
    private String periodicidade;
    private String tipoIndice;

    public IndiceDTO() {
    }

    public IndiceDTO(Long codIndice, String descricao, String periodicidade, String tipoIndice) {
        this.codIndice = codIndice;
        this.descricao = descricao;
        this.periodicidade = periodicidade;
        this.tipoIndice = tipoIndice;
    }

    public Long getCodIndice() {
        return codIndice;
    }

    public void setCodIndice(Long codIndice) {
        this.codIndice = codIndice;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(String periodicidade) {
        this.periodicidade = periodicidade;
    }

    public String getTipoIndice() {
        return tipoIndice;
    }

    public void setTipoIndice(String tipoIndice) {
        this.tipoIndice = tipoIndice;
    }
}
