package com.bezkoder.spring.datajpa.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "tipo_investimento")
public class TipoInvestimento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cod_investimento")
    private Long codInvestimento;

    @Column(name = "cod_indice", length = 20)
    private Long codIndice;

    @OneToOne
    @JoinColumn(name = "cod_indice", insertable = false, updatable = false)
    private Indice indice;

    @Column(name = "descricao", length = 200)
    private String descricao;

    @Column(name = "incide_iof", length = 1)
    private String incideIof;

    @Column(name = "incide_ir", length = 1)
    private String incideIr;

    public TipoInvestimento() {
    }

    public TipoInvestimento(Long codInvestimento, String descricao, String incideIof, String incideIr, Indice indice) {
        this.codInvestimento = codInvestimento;
        this.descricao = descricao;
        this.incideIof = incideIof;
        this.incideIr = incideIr;
        this.indice = indice;
        this.codIndice = indice.getCodIndice();
    }

    public Long getCodInvestimento() {
        return codInvestimento;
    }

    public void setCodInvestimento(Long codInvestimento) {
        this.codInvestimento = codInvestimento;
    }

    public Indice getIndice() {
        return indice;
    }

    public Long getCodIndice() {
        return codIndice;
    }

    public void setIndice(Indice indice) {
        this.indice = indice;
        this.codIndice = indice.getCodIndice();
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