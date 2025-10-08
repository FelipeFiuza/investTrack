package com.bezkoder.spring.datajpa.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class ApuracaoIndiceId implements Serializable {
    private Indice indice;
    private LocalDateTime dataApuracao;

    public ApuracaoIndiceId() {}

    public ApuracaoIndiceId(Indice indice, LocalDateTime dataApuracao) {
        this.indice = indice;
        this.dataApuracao = dataApuracao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApuracaoIndiceId)) return false;
        ApuracaoIndiceId that = (ApuracaoIndiceId) o;
        return Objects.equals(indice, that.indice) &&
               Objects.equals(dataApuracao, that.dataApuracao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this);
    }
}