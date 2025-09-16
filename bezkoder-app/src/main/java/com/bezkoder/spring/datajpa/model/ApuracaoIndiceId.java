package com.bezkoder.spring.datajpa.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class ApuracaoIndiceId implements Serializable {
    private String codIndice;
    private LocalDateTime dataApuracao;

    public ApuracaoIndiceId() {}

    public ApuracaoIndiceId(String codIndice, LocalDateTime dataApuracao) {
        this.codIndice = codIndice;
        this.dataApuracao = dataApuracao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApuracaoIndiceId)) return false;
        ApuracaoIndiceId that = (ApuracaoIndiceId) o;
        return Objects.equals(codIndice, that.codIndice) &&
               Objects.equals(dataApuracao, that.dataApuracao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codIndice, dataApuracao);
    }
}