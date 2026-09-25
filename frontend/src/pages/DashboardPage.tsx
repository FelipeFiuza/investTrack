import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import PanelChart from '../components/PanelChart';
import { panelApi } from '../services/api';
import { Panel } from '../types';
import './DashboardPage.css';

type SlotId = 'tl' | 'tr' | 'bl' | 'br';

const SLOTS: { id: SlotId; label: string }[] = [
  { id: 'tl', label: 'Esquerda superior' },
  { id: 'tr', label: 'Direita superior' },
  { id: 'bl', label: 'Esquerda inferior' },
  { id: 'br', label: 'Direita inferior' },
];

const emptySlots = (): Record<SlotId, number | null> => ({
  tl: null,
  tr: null,
  bl: null,
  br: null,
});

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

const loadSlots = (userId: number): Record<SlotId, number | null> => {
  try {
    const raw = localStorage.getItem(storageKey(userId));
    if (!raw) return emptySlots();
    const parsed = JSON.parse(raw) as Partial<Record<SlotId, number | null>>;
    return { ...emptySlots(), ...parsed };
  } catch {
    return emptySlots();
  }
};

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const userId = getUserId();
  const [slots, setSlots] = useState<Record<SlotId, number | null>>(() => loadSlots(userId));
  const [editing, setEditing] = useState<Record<SlotId, boolean>>({
    tl: false,
    tr: false,
    bl: false,
    br: false,
  });
  const [pickerSlot, setPickerSlot] = useState<SlotId | null>(null);
  const [panels, setPanels] = useState<Panel[]>([]);
  const [loadingPanels, setLoadingPanels] = useState(false);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const [novaDescricao, setNovaDescricao] = useState('');

  useEffect(() => {
    localStorage.setItem(storageKey(userId), JSON.stringify(slots));
  }, [slots, userId]);

  const openPicker = async (slot: SlotId) => {
    setPickerSlot(slot);
    setNovaDescricao('');
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

  const createAndAssign = async () => {
    if (!pickerSlot) return;
    setPickerError(null);
    try {
      const res = await panelApi.create({
        idUsuario: userId,
        descricao: novaDescricao.trim() || 'Novo panel',
      });
      setPanels((prev) => [...prev, res.data]);
      assignPanel(pickerSlot, res.data.idPanel);
    } catch {
      setPickerError('Não foi possível criar o panel.');
    }
  };

  return (
    <AppLayout>
      <div className="dashboard-board">
        {SLOTS.map((slot) => {
          const panelId = slots[slot.id];
          const isEditing = editing[slot.id];
          return (
            <section key={slot.id} className="dashboard-slot" aria-label={slot.label}>
              {panelId == null ? (
                <button
                  type="button"
                  className="dashboard-placeholder"
                  onClick={() => openPicker(slot.id)}
                  aria-label={`Adicionar panel em ${slot.label}`}
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
                        setEditing((prev) => ({ ...prev, [slot.id]: !prev[slot.id] }))
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
                      onClick={() => navigate(`/panel/${panelId}`)}
                      aria-label="Abrir panel"
                      title="Abrir panel"
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <rect x="4" y="5" width="16" height="14" rx="2" fill="none" stroke="currentColor" strokeWidth="2" />
                      </svg>
                    </button>
                  </div>
                  <PanelChart
                    panelId={panelId}
                    mode={isEditing ? 'edit' : 'display'}
                    embedded
                  />
                </>
              )}
            </section>
          );
        })}
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
            <label className="picker-create">
              <span>Novo panel</span>
              <input
                className="form-input"
                value={novaDescricao}
                onChange={(e) => setNovaDescricao(e.target.value)}
                placeholder="Descrição"
              />
            </label>
            {pickerError && <p className="picker-error">{pickerError}</p>}
            <div className="picker-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setPickerSlot(null)}>
                Cancelar
              </button>
              <button type="button" className="btn btn-primary" onClick={createAndAssign}>
                Criar e exibir
              </button>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default DashboardPage;
