package com.bezkoder.spring.datajpa.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bezkoder.spring.datajpa.dto.TransacaoDTO;
import com.bezkoder.spring.datajpa.dto.TransacaoImportItemDTO;
import com.bezkoder.spring.datajpa.dto.TransacaoImportRequestDTO;
import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.model.Transacao;
import com.bezkoder.spring.datajpa.model.Usuario;
import com.bezkoder.spring.datajpa.repository.IndiceRepository;
import com.bezkoder.spring.datajpa.repository.TipoInvestimentoRepository;
import com.bezkoder.spring.datajpa.repository.TransacaoRepository;
import com.bezkoder.spring.datajpa.repository.UsuarioRepository;

@Service
public class TransacaoImportService {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoInvestimentoRepository tipoInvestimentoRepository;

    @Autowired
    private IndiceRepository indiceRepository;

    @Transactional
    public List<TransacaoDTO> importTransacoes(TransacaoImportRequestDTO request) {
        if (request == null || request.getTransacoes() == null || request.getTransacoes().isEmpty()) {
            throw new IllegalArgumentException("No transactions to import");
        }

        Long idUsuario = request.getIdUsuario() != null ? request.getIdUsuario() : 1L;
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + idUsuario));

        List<Transacao> saved = new ArrayList<Transacao>();
        for (TransacaoImportItemDTO item : request.getTransacoes()) {
            if (item == null) {
                continue;
            }
            Transacao transacao = new Transacao();
            transacao.setUsuario(usuario);
            transacao.setDataTransacao(item.getDataTransacao());
            transacao.setInstituicao(item.getInstituicao());
            transacao.setTipoTransacao(item.getTipoTransacao());
            transacao.setQuantidade(item.getQuantidade());
            transacao.setValorTotal(item.getValor());
            transacao.setValorUnitario(resolveValorUnitario(item));
            transacao.setInvestimento(resolveInvestimento(item));
            saved.add(transacaoRepository.save(transacao));
        }
        return toDTOs(saved);
    }

    private BigDecimal resolveValorUnitario(TransacaoImportItemDTO item) {
        if (item.getValorUnitario() != null) {
            return item.getValorUnitario();
        }
        if (item.getValor() != null && item.getQuantidade() != null
                && item.getQuantidade().compareTo(BigDecimal.ZERO) != 0) {
            return item.getValor().divide(item.getQuantidade(), 6, RoundingMode.HALF_UP);
        }
        return item.getValor();
    }

    private TipoInvestimento resolveInvestimento(TransacaoImportItemDTO item) {
        String ticker = normalizeTicker(item.getTicker(), item.getProduto());
        if (ticker.isEmpty()) {
            throw new IllegalArgumentException("Missing ticker for product: " + item.getProduto());
        }

        List<TipoInvestimento> byTicker = tipoInvestimentoRepository.findByIndice_DescricaoIgnoreCase(ticker);
        if (byTicker != null && !byTicker.isEmpty()) {
            return byTicker.get(0);
        }

        Indice indice = indiceRepository.findByDescricao(ticker);
        if (indice == null) {
            indice = new Indice();
            indice.setDescricao(ticker);
            indice.setTipoIndice("F");
            indice.setPeriodicidade("D");
            indice = indiceRepository.save(indice);
        }

        TipoInvestimento existing = tipoInvestimentoRepository.findByIndice(indice);
        if (existing != null) {
            return existing;
        }

        TipoInvestimento tipo = new TipoInvestimento();
        tipo.setIndice(indice);
        String descricao = item.getProduto() != null && !item.getProduto().trim().isEmpty()
            ? item.getProduto().trim()
            : ticker;
        tipo.setDescricao(descricao);
        tipo.setIncideIof("N");
        tipo.setIncideIr("A");
        return tipoInvestimentoRepository.save(tipo);
    }

    private String normalizeTicker(String ticker, String produto) {
        if (ticker != null && !ticker.trim().isEmpty()) {
            return ticker.trim().toUpperCase();
        }
        if (produto == null) {
            return "";
        }
        String trimmed = produto.trim();
        int dash = trimmed.indexOf(" - ");
        if (dash > 0) {
            return trimmed.substring(0, dash).trim().toUpperCase();
        }
        String[] parts = trimmed.split("\\s+");
        return parts.length > 0 ? parts[0].toUpperCase() : "";
    }

    private List<TransacaoDTO> toDTOs(List<Transacao> list) {
        List<TransacaoDTO> dtos = new ArrayList<TransacaoDTO>();
        for (Transacao t : list) {
            TransacaoDTO dto = new TransacaoDTO();
            dto.setIdTransacao(t.getIdTransacao());
            dto.setDataTransacao(t.getDataTransacao());
            dto.setDataVencimento(t.getDataVencimento());
            dto.setInstituicao(t.getInstituicao());
            dto.setTipoTransacao(t.getTipoTransacao());
            dto.setValor(t.getValorTotal());
            dto.setQuantidade(t.getQuantidade());
            if (t.getUsuario() != null) {
                dto.setIdUsuario(t.getUsuario().getIdUsuario());
            }
            if (t.getInvestimento() != null) {
                dto.setCodInvestimento(t.getInvestimento().getCodInvestimento());
                dto.setDescricaoInvestimento(t.getInvestimento().getDescricao());
            }
            dtos.add(dto);
        }
        return dtos;
    }
}
