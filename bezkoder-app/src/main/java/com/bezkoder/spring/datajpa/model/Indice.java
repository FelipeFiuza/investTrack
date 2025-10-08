package com.bezkoder.spring.datajpa.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "indice")
public class Indice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cod_indice", length = 20)
    private Long codIndice;

    @Column(name = "descricao", length = 40)
    private String descricao;

    @Column(name = "periodicidade", length = 1)
    private String periodicidade;

    @Column(name = "tipo_indice", length = 1)
    private String tipoIndice;

    public Indice(String descricao, String periodicidade, String tipoIndice) {
        this.descricao = descricao;
        this.periodicidade = periodicidade;
        this.tipoIndice = tipoIndice;
    }

    public Indice() {
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