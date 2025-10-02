package com.bezkoder.spring.datajpa.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@IdClass(ApuracaoIndiceId.class)
@Table(name = "apuracao_indice_fixa")
public class ApuracaoIndiceFixa {

    @Id
    @Column(name = "data_apuracao")
    private LocalDateTime dataApuracao;

    @Id
    @ManyToOne
    @JoinColumn(name = "cod_indice", insertable = false, updatable = false)
    private Indice indice;

    @Column(name = "valor_fechamento")
    private BigDecimal valorFechamento;

    public ApuracaoIndiceFixa() {
    }

    public ApuracaoIndiceFixa(String codIndice, LocalDateTime dataApuracao, Indice indice, BigDecimal valorFechamento) {
        this.dataApuracao = dataApuracao;
        this.indice = indice;
        this.valorFechamento = valorFechamento;
    }

    public LocalDateTime getDataApuracao() {
        return dataApuracao;
    }

    public void setDataApuracao(LocalDateTime dataApuracao) {
        this.dataApuracao = dataApuracao;
    }

    public Indice getIndice() {
        return indice;
    }

    public void setIndice(Indice indice) {
        this.indice = indice;
    }

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }
}