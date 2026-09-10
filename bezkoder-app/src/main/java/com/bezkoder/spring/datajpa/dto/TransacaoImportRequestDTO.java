package com.bezkoder.spring.datajpa.dto;

import java.util.List;

public class TransacaoImportRequestDTO {

    private Long idUsuario;
    private List<TransacaoImportItemDTO> transacoes;

    public TransacaoImportRequestDTO() {
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public List<TransacaoImportItemDTO> getTransacoes() {
        return transacoes;
    }

    public void setTransacoes(List<TransacaoImportItemDTO> transacoes) {
        this.transacoes = transacoes;
    }
}
