import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import PanelChart from '../components/PanelChart';
import { panelApi } from '../services/api';

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

const PanelPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) return;
    let cancelled = false;
    const openDefault = async () => {
      try {
        const res = await panelApi.list(getUserId());
        const panels = res.data || [];
        const target =
          panels[0] ||
          (await panelApi.create({ idUsuario: getUserId(), descricao: 'Novo panel' })).data;
        if (!cancelled) {
          navigate(`/panel/${target.idPanel}`, { replace: true });
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível abrir o panel.');
        }
      }
    };
    openDefault();
    return () => {
      cancelled = true;
    };
  }, [id, navigate]);

  const panelId = Number(id);

  return (
    <AppLayout>
      {id && Number.isFinite(panelId) ? (
        <PanelChart panelId={panelId} mode="edit" />
      ) : (
        <div className="panel-container">
          <main className="panel-main">
            <p>{error || 'Carregando panel...'}</p>
          </main>
        </div>
      )}
    </AppLayout>
  );
};

export default PanelPage;
