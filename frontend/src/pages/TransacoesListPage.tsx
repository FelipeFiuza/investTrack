import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import { transacaoApi } from '../services/api';
import { Transacao } from '../types';
import './TransacoesListPage.css';

const ID_USUARIO = 1;

const TransacoesListPage: React.FC = () => {
  const [transacoes, setTransacoes] = useState<Transacao[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const loadTransacoes = async () => {
      try {
        const res = await transacaoApi.getAll(ID_USUARIO);
        setTransacoes(res.data);
      } catch (err) {
        setError('Failed to load transactions.');
      } finally {
        setLoading(false);
      }
    };
    loadTransacoes();
  }, []);

  const handleDelete = async (idTransacao?: number) => {
    if (!idTransacao) return;
    const ok = window.confirm('Delete this transaction?');
    if (!ok) return;

    setLoading(true);
    setError(null);
    try {
      await transacaoApi.delete(idTransacao);
      const res = await transacaoApi.getAll(ID_USUARIO);
      setTransacoes(res.data);
    } catch (err) {
      setError('Failed to delete transaction.');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (value: string | undefined) => {
    if (!value) return '—';
    return new Date(value).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value/100);
  };

  const formatBuySell = (value?: string) => {
    if (!value) return '—';
    const v = value.toLowerCase();
    if (v === 'buy') return 'Buy';
    if (v === 'sell') return 'Sell';
    return value;
  };

  return (
    <AppLayout>
      <div className="transacoes-list-page">
        <header className="transacoes-list-header">
          <h1>Transações</h1>
          <button
            className="btn btn-primary"
            onClick={() => navigate('/transacao')}
          >
            Adicionar transação
          </button>
        </header>

        <div className="transacoes-list-content">
          {loading ? (
            <div className="loading-spinner">
              <div className="spinner"></div>
              <p>Carregando transações...</p>
            </div>
          ) : error ? (
            <p className="transacoes-list-error">{error}</p>
          ) : transacoes.length === 0 ? (
            <p className="transacoes-list-empty">Nenhuma transação encontrada para este usuário.</p>
          ) : (
            <div className="transacoes-table-wrapper">
              <table className="transacoes-table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Vencimento</th>
                    <th>Instituição</th>
                    <th>Tipo</th>
                    <th>Compra/Venda</th>
                    <th>Quantidade</th>
                    <th>Valor</th>
                    <th className="transacoes-actions-col">Editar</th>
                  </tr>
                </thead>
                <tbody>
                  {transacoes.map((t) => {
                    if (!t.idTransacao) return null;
                    return (
                      <tr key={t.idTransacao}>
                        <td>{formatDate(t.dataTransacao)}</td>
                        <td>{formatDate(t.dataVencimento)}</td>
                        <td>{t.instituicao || '—'}</td>
                        <td>{t.descricaoInvestimento || '—'}</td>
                        <td>{formatBuySell(t.tipoTransacao)}</td>
                        <td>{Number(t.quantidade)}</td>
                        <td>{formatCurrency(Number(t.valor))}</td>
                        <td className="transacoes-actions-col">
                          <div className="transacoes-actions">
                            <button
                              type="button"
                              className="transacao-action-btn transacao-action-edit"
                              aria-label="Edit transaction"
                              onClick={() => navigate(`/transacao/${t.idTransacao}`)}
                            >
                              ✏️
                            </button>
                            <button
                              type="button"
                              className="transacao-action-btn transacao-action-delete"
                              aria-label="Delete transaction"
                              onClick={() => handleDelete(t.idTransacao)}
                            >
                              🗑️
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </AppLayout>
  );
};

export default TransacoesListPage;
