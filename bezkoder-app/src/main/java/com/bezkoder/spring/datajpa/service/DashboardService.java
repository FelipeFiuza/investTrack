package com.bezkoder.spring.datajpa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bezkoder.spring.datajpa.dto.DashboardDTO;
import com.bezkoder.spring.datajpa.dto.DashboardWriteDTO;
import com.bezkoder.spring.datajpa.model.Dashboard;
import com.bezkoder.spring.datajpa.model.Panel;
import com.bezkoder.spring.datajpa.model.Usuario;
import com.bezkoder.spring.datajpa.repository.DashboardRepository;
import com.bezkoder.spring.datajpa.repository.PanelRepository;
import com.bezkoder.spring.datajpa.repository.UsuarioRepository;

@Service
public class DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PanelRepository panelRepository;

    @Transactional
    public List<DashboardDTO> listByUsuario(Long idUsuario) {
        List<DashboardDTO> result = new ArrayList<DashboardDTO>();
        List<Dashboard> dashboards = dashboardRepository.findByUsuario_IdUsuarioOrderByIdDashboardAsc(idUsuario);
        for (Dashboard dashboard : dashboards) {
            result.add(toDto(dashboard));
        }
        return result;
    }

    @Transactional
    public DashboardDTO getById(Long id) {
        Dashboard dashboard = dashboardRepository.findById(id).orElse(null);
        return dashboard == null ? null : toDto(dashboard);
    }

    @Transactional
    public DashboardDTO create(DashboardWriteDTO dto) {
        if (dto == null || dto.getIdUsuario() == null) {
            throw new IllegalArgumentException("idUsuario e obrigatorio");
        }
        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario()).orElse(null);
        if (usuario == null) {
            return null;
        }
        Dashboard dashboard = new Dashboard();
        dashboard.setUsuario(usuario);
        apply(dashboard, dto, usuario.getIdUsuario());
        return toDto(dashboardRepository.save(dashboard));
    }

    @Transactional
    public DashboardDTO update(Long id, DashboardWriteDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dashboard e obrigatorio");
        }
        Dashboard dashboard = dashboardRepository.findById(id).orElse(null);
        if (dashboard == null || dashboard.getUsuario() == null) {
            return null;
        }
        apply(dashboard, dto, dashboard.getUsuario().getIdUsuario());
        return toDto(dashboardRepository.save(dashboard));
    }

    private void apply(Dashboard dashboard, DashboardWriteDTO dto, Long idUsuario) {
        dashboard.setDescricao(normalizeDescricao(dto.getDescricao()));
        dashboard.setPanelEsquerdaSuperior(resolvePanel(dto.getIdPanelEsquerdaSuperior(), idUsuario));
        dashboard.setPanelDireitaSuperior(resolvePanel(dto.getIdPanelDireitaSuperior(), idUsuario));
        dashboard.setPanelEsquerdaInferior(resolvePanel(dto.getIdPanelEsquerdaInferior(), idUsuario));
        dashboard.setPanelDireitaInferior(resolvePanel(dto.getIdPanelDireitaInferior(), idUsuario));
    }

    private Panel resolvePanel(Long idPanel, Long idUsuario) {
        if (idPanel == null) {
            return null;
        }
        Panel panel = panelRepository.findById(idPanel).orElse(null);
        if (panel == null || panel.getUsuario() == null
                || !idUsuario.equals(panel.getUsuario().getIdUsuario())) {
            throw new IllegalArgumentException("panel invalido: " + idPanel);
        }
        return panel;
    }

    private String normalizeDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            return "Dashboard";
        }
        return descricao.trim();
    }

    private DashboardDTO toDto(Dashboard dashboard) {
        DashboardDTO dto = new DashboardDTO();
        dto.setIdDashboard(dashboard.getIdDashboard());
        dto.setIdUsuario(dashboard.getUsuario() != null ? dashboard.getUsuario().getIdUsuario() : null);
        dto.setDescricao(dashboard.getDescricao());
        dto.setIdPanelEsquerdaSuperior(idOf(dashboard.getPanelEsquerdaSuperior()));
        dto.setIdPanelDireitaSuperior(idOf(dashboard.getPanelDireitaSuperior()));
        dto.setIdPanelEsquerdaInferior(idOf(dashboard.getPanelEsquerdaInferior()));
        dto.setIdPanelDireitaInferior(idOf(dashboard.getPanelDireitaInferior()));
        return dto;
    }

    private Long idOf(Panel panel) {
        return panel == null ? null : panel.getIdPanel();
    }
}
