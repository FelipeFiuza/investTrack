package com.bezkoder.spring.datajpa.model;

import javax.persistence.*;

@Entity
@Table(name = "Indice")
public class Indice {

    @Id
    @Column(name = "codIndice", length = 20)
    private String codIndice;

    @Column(name = "descricao", length = 40)
    private String descricao;

    @Column(name = "periodicidade", length = 1)
    private String periodicidade;

    @Column(name = "tipoIndice", length = 1)
    private String tipoIndice;

    // Getters e Setters
    public String getCodIndice() {
        return codIndice;
    }

    public void setCodIndice(String codIndice) {
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