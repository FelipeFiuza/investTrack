package com.bezkoder.spring.datajpa.dto;

public class IndiceUpdateDTO {
    private String descricao;
    private String periodicidade;
    private String tipoIndice;

    public IndiceUpdateDTO() {
    }

    public IndiceUpdateDTO(String descricao, String periodicidade, String tipoIndice) {
        this.descricao = descricao;
        this.periodicidade = periodicidade;
        this.tipoIndice = tipoIndice;
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
