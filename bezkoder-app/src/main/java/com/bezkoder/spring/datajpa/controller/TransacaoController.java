package com.bezkoder.spring.datajpa.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bezkoder.spring.datajpa.dto.TransacaoCreateUpdateDTO;
import com.bezkoder.spring.datajpa.dto.TransacaoDTO;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;
import com.bezkoder.spring.datajpa.model.Usuario;
import com.bezkoder.spring.datajpa.model.Transacao;
import com.bezkoder.spring.datajpa.repository.TipoInvestimentoRepository;
import com.bezkoder.spring.datajpa.repository.UsuarioRepository;
import com.bezkoder.spring.datajpa.repository.TransacaoRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoInvestimentoRepository tipoInvestimentoRepository;

    @GetMapping
    public ResponseEntity<List<TransacaoDTO>> getAll(@RequestParam(defaultValue = "1") Long idUsuario) {
        try {
            List<Transacao> list = transacaoRepository.findByUsuario_IdUsuario(idUsuario);
            List<TransacaoDTO> dtos = list.stream().map(this::toDTO).collect(Collectors.toList());
            return new ResponseEntity<>(dtos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoDTO> getById(@PathVariable("id") Long id) {
        Optional<Transacao> data = transacaoRepository.findById(id);

        return data.map(this::toDTO)
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<TransacaoDTO> create(@RequestBody TransacaoCreateUpdateDTO dto) {
        try {
            Transacao transacao = new Transacao();
            transacao.setDataTransacao(dto.getDataTransacao());
            transacao.setDataVencimento(dto.getDataVencimento());
            transacao.setInstituicao(dto.getInstituicao());
            transacao.setTipoTransacao(dto.getTipoTransacao());
            transacao.setValorTotal(dto.getValor());
            transacao.setQuantidade(dto.getQuantidade());

            // These relationships may be partially read-only depending on the entity mapping.
            // Still, we set them when provided so the FK can update if allowed.
            if (dto.getCodInvestimento() != null) {
                TipoInvestimento investimento = tipoInvestimentoRepository.findById(dto.getCodInvestimento()).orElse(null);
                transacao.setInvestimento(investimento);
            }
            if (dto.getIdUsuario() != null) {
                Usuario usuario = usuarioRepository.findById(dto.getIdUsuario()).orElse(null);
                transacao.setUsuario(usuario);
            }

            Transacao saved = transacaoRepository.save(transacao);
            return new ResponseEntity<>(toDTO(saved), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransacaoDTO> update(@PathVariable("id") Long id, @RequestBody TransacaoCreateUpdateDTO dto) {
        try {
            Optional<Transacao> data = transacaoRepository.findById(id);
            if (!data.isPresent()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Transacao transacao = data.get();
            transacao.setDataTransacao(dto.getDataTransacao());
            transacao.setDataVencimento(dto.getDataVencimento());
            transacao.setInstituicao(dto.getInstituicao());
            transacao.setTipoTransacao(dto.getTipoTransacao());
            transacao.setValorTotal(dto.getValor());
            transacao.setQuantidade(dto.getQuantidade());

            if (dto.getCodInvestimento() != null) {
                TipoInvestimento investimento = tipoInvestimentoRepository.findById(dto.getCodInvestimento()).orElse(null);
                transacao.setInvestimento(investimento);
            }
            if (dto.getIdUsuario() != null) {
                Usuario usuario = usuarioRepository.findById(dto.getIdUsuario()).orElse(null);
                transacao.setUsuario(usuario);
            }

            Transacao saved = transacaoRepository.save(transacao);
            return new ResponseEntity<>(toDTO(saved), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable("id") Long id) {
        try {
            if (!transacaoRepository.existsById(id)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            transacaoRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private TransacaoDTO toDTO(Transacao t) {
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
        return dto;
    }
}
