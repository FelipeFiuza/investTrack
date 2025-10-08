package com.bezkoder.spring.datajpa.dto;

import java.math.BigDecimal;

public class ApuracaoIndiceUpdateDTO {
    private BigDecimal valorAbertura;
    private BigDecimal valorMaximo;
    private BigDecimal valorMinimo;
    private BigDecimal valorFechamento;

    public ApuracaoIndiceUpdateDTO() {}

    public ApuracaoIndiceUpdateDTO(BigDecimal valorAbertura, BigDecimal valorMaximo, BigDecimal valorMinimo,
                                   BigDecimal valorFechamento) {
        this.valorAbertura = valorAbertura;
        this.valorMaximo = valorMaximo;
        this.valorMinimo = valorMinimo;
        this.valorFechamento = valorFechamento;
    }

    public BigDecimal getValorAbertura() { return valorAbertura; }
    public void setValorAbertura(BigDecimal valorAbertura) { this.valorAbertura = valorAbertura; }

    public BigDecimal getValorMaximo() { return valorMaximo; }
    public void setValorMaximo(BigDecimal valorMaximo) { this.valorMaximo = valorMaximo; }

    public BigDecimal getValorMinimo() { return valorMinimo; }
    public void setValorMinimo(BigDecimal valorMinimo) { this.valorMinimo = valorMinimo; }

    public BigDecimal getValorFechamento() { return valorFechamento; }
    public void setValorFechamento(BigDecimal valorFechamento) { this.valorFechamento = valorFechamento; }
}
