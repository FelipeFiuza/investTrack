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
@Table(name = "apuracao_indice")
public class ApuracaoIndice {

    @Id
    @Column(name = "data_apuracao")
    private LocalDateTime dataApuracao;

    @Id
    @Column(name = "cod_indice")
    private Long codIndice;

    @ManyToOne
    @JoinColumn(name = "cod_indice", insertable = false, updatable = false)
    private Indice indice;

    @Column(name = "valor_abertura")
    private BigDecimal valorAbertura;

    @Column(name = "valor_maximo")
    private BigDecimal valorMaximo;

    @Column(name = "valor_minimo")
    private BigDecimal valorMinimo;

    @Column(name = "valor_fechamento")
    private BigDecimal valorFechamento;

    public ApuracaoIndice() {
    }

    public ApuracaoIndice(LocalDateTime dataApuracao, Indice indice, BigDecimal valorAbertura, BigDecimal valorFechamento, BigDecimal valorMaximo, BigDecimal valorMinimo) {
        this.dataApuracao = dataApuracao;
        this.indice = indice;
        this.codIndice = indice.getCodIndice();
        this.valorAbertura = valorAbertura;
        this.valorFechamento = valorFechamento;
        this.valorMaximo = valorMaximo;
        this.valorMinimo = valorMinimo;
    }

    public LocalDateTime getDataApuracao() {
        return dataApuracao;
    }

    public void setDataApuracao(LocalDateTime dataApuracao) {
        this.dataApuracao = dataApuracao;
    }

    public Long getCodIndice() {
        return codIndice;
    }

    public Indice getIndice() {
        return indice;
    }

    public void setIndice(Indice indice) {
        this.indice = indice;
        this.codIndice = indice.getCodIndice();
    }

    public BigDecimal getValorAbertura() {
        return valorAbertura;
    }

    public void setValorAbertura(BigDecimal valorAbertura) {
        this.valorAbertura = valorAbertura;
    }

    public BigDecimal getValorMaximo() {
        return valorMaximo;
    }

    public void setValorMaximo(BigDecimal valorMaximo) {
        this.valorMaximo = valorMaximo;
    }

    public BigDecimal getValorMinimo() {
        return valorMinimo;
    }

    public void setValorMinimo(BigDecimal valorMinimo) {
        this.valorMinimo = valorMinimo;
    }

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }

}