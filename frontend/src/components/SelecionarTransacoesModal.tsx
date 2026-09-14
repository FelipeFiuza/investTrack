import React, { useEffect } from 'react';
import { Transacao } from '../types';
import { isBuy } from '../utils/transactionSelection';
import './SelecionarTransacoesModal.css';

interface SelecionarTransacoesModalProps {
  open: boolean;
  transacoes: Transacao[];
  selectedIds: Set<number>;
  loading: boolean;
  error: string | null;
  onClose: () => void;
  onToggle: (id: number, checked: boolean) => void;
  onToggleAll: (checked: boolean) => void;
}

const currencyFmt = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

const formatDate = (value?: string): string => {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const SelecionarTransacoesModal: React.FC<SelecionarTransacoesModalProps> = ({
  open,
  transacoes,
  selectedIds,
  loading,
  error,
  onClose,
  onToggle,
  onToggleAll,
}) => {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  const selectable = transacoes.filter((t) => t.idTransacao != null);
  const selectedCount = selectable.filter((t) => selectedIds.has(t.idTransacao as number)).length;
  const allSelected = selectable.length > 0 && selectedCount === selectable.length;

  return (
    <div className="tx-modal-overlay" onClick={onClose} role="presentation">
      <div
        className="tx-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tx-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="tx-modal-header">
          <div>
            <h2 id="tx-modal-title">Selecionar transações</h2>
            <p>
              {selectedCount} de {selectable.length} consideradas no gráfico. Ao desmarcar uma
              compra, vendas do mesmo investimento que deixariam saldo negativo também são
              desmarcadas.
            </p>
          </div>
          <button type="button" className="tx-modal-close" onClick={onClose} aria-label="Fechar">
            ×
          </button>
        </header>

        <div className="tx-modal-body">
          {loading ? (
            <div className="tx-modal-status">Carregando transações...</div>
          ) : error ? (
            <div className="tx-modal-status tx-modal-error">{error}</div>
          ) : selectable.length === 0 ? (
            <div className="tx-modal-status">Nenhuma transação encontrada.</div>
          ) : (
            <table className="tx-modal-table">
              <thead>
                <tr>
                  <th className="tx-modal-check-col">
                    <input
                      type="checkbox"
                      checked={allSelected}
                      onChange={(e) => onToggleAll(e.target.checked)}
                      aria-label="Selecionar todas"
                    />
                  </th>
                  <th>Data</th>
                  <th>Tipo</th>
                  <th>Compra/Venda</th>
                  <th>Qtd</th>
                  <th>Valor</th>
                  <th>Instituição</th>
                </tr>
              </thead>
              <tbody>
                {selectable.map((t) => {
                  const id = t.idTransacao as number;
                  const checked = selectedIds.has(id);
                  const buy = isBuy(t.tipoTransacao);
                  return (
                    <tr key={id} className={checked ? '' : 'tx-modal-row-off'}>
                      <td>
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={(e) => onToggle(id, e.target.checked)}
                          aria-label={`Considerar transação ${id}`}
                        />
                      </td>
                      <td>{formatDate(t.dataTransacao)}</td>
                      <td>{t.descricaoInvestimento || '—'}</td>
                      <td>
                        <span className={`tx-modal-type ${buy ? 'tx-modal-type-buy' : 'tx-modal-type-sell'}`}>
                          {buy ? 'Compra' : 'Venda'}
                        </span>
                      </td>
                      <td>{Number(t.quantidade)}</td>
                      <td>{currencyFmt.format(Number(t.valor) / 100)}</td>
                      <td>{t.instituicao || '—'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <footer className="tx-modal-footer">
          <button type="button" className="btn btn-primary" onClick={onClose}>
            Fechar
          </button>
        </footer>
      </div>
    </div>
  );
};

export default SelecionarTransacoesModal;
