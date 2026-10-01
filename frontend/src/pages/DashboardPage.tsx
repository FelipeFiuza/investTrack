import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import PanelChart from '../components/PanelChart';
import { dashboardApi, panelApi } from '../services/api';
import { Dashboard, Panel } from '../types';
import { buildDashboardRegions, emptySlots, SlotId, SlotMap } from '../utils/dashboardLayout';
import './DashboardPage.css';

const getUserId = (): number => {
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return 1;
    const user = JSON.parse(raw);
    return user.idUsuario || 1;
  } catch {
    return 1;
  }
};

const storageKey = (userId: number): string => `investtrack-dashboard-${userId}`;

const loadLocalSlots = (userId: number): SlotMap => {
  try {
    const raw = localStorage.getItem(storageKey(userId));
    if (!raw) return emptySlots();
    const parsed = JSON.parse(raw) as Partial<SlotMap>;
    return { ...emptySlots(), ...parsed };
  } catch {
    return emptySlots();
  }
};

const slotsFromDashboard = (dashboard: Dashboard): SlotMap => ({
  tl: dashboard.idPanelEsquerdaSuperior,
  tr: dashboard.idPanelDireitaSuperior,
  bl: dashboard.idPanelEsquerdaInferior,
  br: dashboard.idPanelDireitaInferior,
});

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const userId = getUserId();
  const [dashboardId, setDashboardId] = useState<number | null>(null);
  const [descricao, setDescricao] = useState('Dashboard');
  const [slots, setSlots] = useState<SlotMap>(emptySlots);
  const [editing, setEditing] = useState<Record<string, boolean>>({});
  const [pickerSlot, setPickerSlot] = useState<SlotId | null>(null);
  const [panels, setPanels] = useState<Panel[]>([]);
  const [loadingPanels, setLoadingPanels] = useState(false);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const [creatingPanel, setCreatingPanel] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const readyToSave = useRef(false);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      readyToSave.current = false;
      setLoadError(null);
      try {
        const res = await dashboardApi.list(userId);
        const existing = (res.data || [])[0];
        let dashboard = existing;
        if (!dashboard) {
          const local = loadLocalSlots(userId);
          const payload = {
            idUsuario: userId,
            descricao: 'Dashboard',
            idPanelEsquerdaSuperior: local.tl,
            idPanelDireitaSuperior: local.tr,
            idPanelEsquerdaInferior: local.bl,
            idPanelDireitaInferior: local.br,
          };
          try {
            dashboard = (await dashboardApi.create(payload)).data;
          } catch {
            dashboard = (
              await dashboardApi.create({
                ...payload,
                idPanelEsquerdaSuperior: null,
                idPanelDireitaSuperior: null,
                idPanelEsquerdaInferior: null,
                idPanelDireitaInferior: null,
              })
            ).data;
          }
        }
        if (cancelled) return;
        localStorage.removeItem(storageKey(userId));
        setDashboardId(dashboard.idDashboard);
        setDescricao(dashboard.descricao || 'Dashboard');
        setSlots(slotsFromDashboard(dashboard));
        readyToSave.current = true;
      } catch {
        if (!cancelled) {
          setLoadError('Não foi possível carregar o dashboard.');
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, [userId]);

  useEffect(() => {
    if (!readyToSave.current || dashboardId == null) return;
    const handle = window.setTimeout(() => {
      dashboardApi
        .update(dashboardId, {
          idUsuario: userId,
          descricao,
          idPanelEsquerdaSuperior: slots.tl,
          idPanelDireitaSuperior: slots.tr,
          idPanelEsquerdaInferior: slots.bl,
          idPanelDireitaInferior: slots.br,
        })
        .catch(() => setLoadError('Não foi possível salvar o dashboard.'));
    }, 300);
    return () => window.clearTimeout(handle);
  }, [dashboardId, descricao, slots, userId]);

  const openPicker = async (slot: SlotId) => {
    setPickerSlot(slot);
    setPickerError(null);
    setLoadingPanels(true);
    try {
      const res = await panelApi.list(userId);
      setPanels(res.data || []);
    } catch {
      setPickerError('Não foi possível carregar os panels.');
      setPanels([]);
    } finally {
      setLoadingPanels(false);
    }
  };

  const assignPanel = (slot: SlotId, panelId: number) => {
    setSlots((prev) => ({ ...prev, [slot]: panelId }));
    setEditing((prev) => ({ ...prev, [slot]: false }));
    setPickerSlot(null);
  };

  const closeRegion = (regionSlots: SlotId[]) => {
    setSlots((prev) => {
      const next = { ...prev };
      regionSlots.forEach((slot) => {
        next[slot] = null;
      });
      return next;
    });
    setEditing((prev) => {
      const next = { ...prev };
      regionSlots.forEach((slot) => {
        next[slot] = false;
      });
      return next;
    });
  };

  const createAndAssign = async () => {
    if (!pickerSlot || creatingPanel) return;
    setPickerError(null);
    setCreatingPanel(true);
    try {
      const res = await panelApi.create({
        idUsuario: userId,
        descricao: 'Novo Painel',
      });
      setPanels((prev) => [...prev, res.data]);
      assignPanel(pickerSlot, res.data.idPanel);
    } catch {
      setPickerError('Não foi possível criar o panel.');
    } finally {
      setCreatingPanel(false);
    }
  };

  const regions = buildDashboardRegions(slots);

  return (
    <AppLayout>
      <div className="dashboard-page">
        <div className="dashboard-toolbar">
          <input
            className="dashboard-title"
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            aria-label="Descrição do dashboard"
          />
          {loadError && <p className="dashboard-error">{loadError}</p>}
        </div>
        <div className="dashboard-board">
          {regions.map((region) => {
            const anchor = region.slots[0];
            const isEditing = Boolean(editing[anchor]);
            return (
              <section
                key={region.id}
                className="dashboard-slot"
                style={{ gridColumn: region.gridColumn, gridRow: region.gridRow }}
                aria-label={region.label}
              >
                {region.panelId == null ? (
                  <button
                    type="button"
                    className="dashboard-placeholder"
                    onClick={() => openPicker(anchor)}
                    aria-label={`Adicionar panel em ${region.label}`}
                  >
                    +
                  </button>
                ) : (
                  <>
                    <div className="slot-actions">
                      <button
                        type="button"
                        className={`slot-action${isEditing ? ' active' : ''}`}
                        onClick={() =>
                          setEditing((prev) => ({ ...prev, [anchor]: !prev[anchor] }))
                        }
                        aria-label="Editar panel"
                        title="Editar"
                      >
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path
                            d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"
                            fill="currentColor"
                          />
                        </svg>
                      </button>
                      <button
                        type="button"
                        className="slot-action"
                        onClick={() => navigate(`/panel/${region.panelId}`)}
                        aria-label="Abrir panel"
                        title="Abrir panel"
                      >
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <rect
                            x="4"
                            y="5"
                            width="16"
                            height="14"
                            rx="2"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                          />
                        </svg>
                      </button>
                      <button
                        type="button"
                        className="slot-action"
                        onClick={() => closeRegion(region.slots)}
                        aria-label="Fechar panel"
                        title="Fechar"
                      >
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path
                            d="M18.3 5.71a1 1 0 0 0-1.41 0L12 10.59 7.11 5.7A1 1 0 0 0 5.7 7.11L10.59 12 5.7 16.89a1 1 0 1 0 1.41 1.41L12 13.41l4.89 4.89a1 1 0 0 0 1.41-1.41L13.41 12l4.89-4.89a1 1 0 0 0 0-1.4z"
                            fill="currentColor"
                          />
                        </svg>
                      </button>
                    </div>
                    <PanelChart
                      panelId={region.panelId}
                      mode={isEditing ? 'edit' : 'display'}
                      embedded
                    />
                  </>
                )}
              </section>
            );
          })}
        </div>
      </div>

      {pickerSlot && (
        <div className="picker-backdrop" onClick={() => setPickerSlot(null)}>
          <div className="picker-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Escolher panel</h2>
            {loadingPanels ? (
              <p>Carregando...</p>
            ) : (
              <ul className="picker-list">
                {panels.length === 0 && <li className="picker-empty">Nenhum panel ainda.</li>}
                {panels.map((panel) => (
                  <li key={panel.idPanel}>
                    <button type="button" onClick={() => assignPanel(pickerSlot, panel.idPanel)}>
                      {panel.descricao || 'Panel'}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {pickerError && <p className="picker-error">{pickerError}</p>}
            <div className="picker-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setPickerSlot(null)}>
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={createAndAssign}
                disabled={creatingPanel}
              >
                Novo Painel
              </button>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default DashboardPage;
