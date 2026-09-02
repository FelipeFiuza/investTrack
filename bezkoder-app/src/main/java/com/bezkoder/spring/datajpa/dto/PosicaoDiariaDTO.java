package com.bezkoder.spring.datajpa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class PosicaoDiariaDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate day;
    private BigDecimal currentQty;
    private BigDecimal currentAverageCost;
    private BigDecimal boughtQty;
    private BigDecimal boughtPrice;
    private BigDecimal boughtTaxes;
    private BigDecimal soldQty;
    private BigDecimal soldPrice;
    private BigDecimal soldTaxes;
    private BigDecimal valorFechamento;
    private BigDecimal grossTotalAmount;
    private BigDecimal netTotalAmount;
    private BigDecimal grossVariation;
    private BigDecimal netVariation;

    public PosicaoDiariaDTO() {
    }

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public BigDecimal getCurrentQty() {
        return currentQty;
    }

    public void setCurrentQty(BigDecimal currentQty) {
        this.currentQty = currentQty;
    }

    public BigDecimal getCurrentAverageCost() {
        return currentAverageCost;
    }

    public void setCurrentAverageCost(BigDecimal currentAverageCost) {
        this.currentAverageCost = currentAverageCost;
    }

    public BigDecimal getBoughtQty() {
        return boughtQty;
    }

    public void setBoughtQty(BigDecimal boughtQty) {
        this.boughtQty = boughtQty;
    }

    public BigDecimal getBoughtPrice() {
        return boughtPrice;
    }

    public void setBoughtPrice(BigDecimal boughtPrice) {
        this.boughtPrice = boughtPrice;
    }

    public BigDecimal getBoughtTaxes() {
        return boughtTaxes;
    }

    public void setBoughtTaxes(BigDecimal boughtTaxes) {
        this.boughtTaxes = boughtTaxes;
    }

    public BigDecimal getSoldQty() {
        return soldQty;
    }

    public void setSoldQty(BigDecimal soldQty) {
        this.soldQty = soldQty;
    }

    public BigDecimal getSoldPrice() {
        return soldPrice;
    }

    public void setSoldPrice(BigDecimal soldPrice) {
        this.soldPrice = soldPrice;
    }

    public BigDecimal getSoldTaxes() {
        return soldTaxes;
    }

    public void setSoldTaxes(BigDecimal soldTaxes) {
        this.soldTaxes = soldTaxes;
    }

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }

    public BigDecimal getGrossTotalAmount() {
        return grossTotalAmount;
    }

    public void setGrossTotalAmount(BigDecimal grossTotalAmount) {
        this.grossTotalAmount = grossTotalAmount;
    }

    public BigDecimal getNetTotalAmount() {
        return netTotalAmount;
    }

    public void setNetTotalAmount(BigDecimal netTotalAmount) {
        this.netTotalAmount = netTotalAmount;
    }

    public BigDecimal getGrossVariation() {
        return grossVariation;
    }

    public void setGrossVariation(BigDecimal grossVariation) {
        this.grossVariation = grossVariation;
    }

    public BigDecimal getNetVariation() {
        return netVariation;
    }

    public void setNetVariation(BigDecimal netVariation) {
        this.netVariation = netVariation;
    }
}
