package com.bezkoder.spring.datajpa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class CotacaoDiaDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate day;
    private BigDecimal valorFechamento;

    public CotacaoDiaDTO() {
    }

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }
}
