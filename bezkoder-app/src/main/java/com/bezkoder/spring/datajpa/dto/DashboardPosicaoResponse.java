package com.bezkoder.spring.datajpa.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DashboardPosicaoResponse {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate inicio;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fim;

    private List<InvestimentoSerieDTO> investimentos = new ArrayList<InvestimentoSerieDTO>();

    public DashboardPosicaoResponse() {
    }

    public DashboardPosicaoResponse(LocalDate inicio, LocalDate fim, List<InvestimentoSerieDTO> investimentos) {
        this.inicio = inicio;
        this.fim = fim;
        this.investimentos = investimentos;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public void setFim(LocalDate fim) {
        this.fim = fim;
    }

    public List<InvestimentoSerieDTO> getInvestimentos() {
        return investimentos;
    }

    public void setInvestimentos(List<InvestimentoSerieDTO> investimentos) {
        this.investimentos = investimentos;
    }
}
