import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import PanelChart from '../components/PanelChart';
import { panelApi } from '../services/api';
import { Panel, PanelBase, PanelMetrica } from '../types';
import './PanelPage.css';

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

const BASE_LABEL: Record<PanelBase, string> = {
  BRUTO: 'Bruto',
  LIQUIDO: 'Líquido',
};

const METRICA_LABEL: Record<PanelMetrica, string> = {
  VALOR: 'Valor',
  RENTABILIDADE: 'Rentabilidade',
};

const PanelList: React.FC = () => {
  const navigate = useNavigate();
  const [panels, setPanels] = useState<Panel[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await panelApi.list(getUserId());
      setPanels(res.data || []);
    } catch {
      setError('Não foi possível carregar os painéis.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const createPanel = async () => {
    if (creating) return;
    setCreating(true);
    setError(null);
    try {
      await panelApi.create({
        idUsuario: getUserId(),
        descricao: 'Novo Painel',
      });
      await load();
    } catch {
      setError('Não foi possível criar o painel.');
      setLoading(false);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="panel-list-page">
      <header className="panel-list-header">
        <h1>Painéis</h1>
        <button type="button" className="btn btn-primary" onClick={createPanel} disabled={creating}>
          Novo painel
        </button>
      </header>
      <div className="panel-list-content">
        {loading ? (
          <p className="panel-list-empty">Carregando...</p>
        ) : error ? (
          <p className="panel-list-error">{error}</p>
        ) : panels.length === 0 ? (
          <p className="panel-list-empty">Nenhum painel ainda.</p>
        ) : (
          <div className="panel-list-table-wrapper">
            <table className="panel-list-table">
              <thead>
                <tr>
                  <th>Descrição</th>
                  <th>Base</th>
                  <th>Métrica</th>
                  <th>Transações</th>
                </tr>
              </thead>
              <tbody>
                {panels.map((panel) => (
                  <tr key={panel.idPanel} onClick={() => navigate(`/panel/${panel.idPanel}`)}>
                    <td>{panel.descricao || 'Novo Painel'}</td>
                    <td>{BASE_LABEL[panel.base] || panel.base}</td>
                    <td>{METRICA_LABEL[panel.metrica] || panel.metrica}</td>
                    <td>{panel.quantidadeTransacoes}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

const PanelPage: React.FC = () => {
  const { id } = useParams();
  const panelId = Number(id);

  return (
    <AppLayout>
      {id && Number.isFinite(panelId) ? <PanelChart panelId={panelId} mode="edit" /> : <PanelList />}
    </AppLayout>
  );
};

export default PanelPage;
