import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { transacaoApi } from '../services/api';
import AppLayout from '../components/AppLayout';
import { TransacaoCreateDTO, TipoInvestimento } from '../types';
import './TransacaoFormPage.css';

const TransacaoFormPage: React.FC = () => {
  const { id } = useParams();
  const isEdit = Boolean(id);

  const [formData, setFormData] = useState<TransacaoCreateDTO>({
    dataTransacao: new Date().toISOString().slice(0, 16),
    dataVencimento: '',
    instituicao: '',
    valor: 0,
    quantidade: 0,
    codInvestimento: 0,
    // Expected values: "buy" or "sell"
    tipoTransacao: 'buy',
    idUsuario: 1 // This should come from the logged-in user
  });
  
  const [tiposInvestimento, setTiposInvestimento] = useState<TipoInvestimento[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const toDateTimeLocal = (value?: string) => {
    if (!value) return '';
    // API may return `YYYY-MM-DDTHH:mm:ss` - input expects `YYYY-MM-DDTHH:mm`.
    return value.slice(0, 16);
  };

  useEffect(() => {
    const loadTiposInvestimento = async () => {
      try {
        // For now, we'll use mock data since the API might not be ready
        const mockTipos: TipoInvestimento[] = [
          { codInvestimento: 1, descricao: 'CDB', incideIof: 'N', incideIr: 'S' },
          { codInvestimento: 2, descricao: 'LCI', incideIof: 'N', incideIr: 'N' },
          { codInvestimento: 3, descricao: 'LCA', incideIof: 'N', incideIr: 'N' },
          { codInvestimento: 4, descricao: 'Tesouro Selic', incideIof: 'S', incideIr: 'S' },
          { codInvestimento: 5, descricao: 'Tesouro IPCA+', incideIof: 'S', incideIr: 'S' }
        ];
        setTiposInvestimento(mockTipos);
      } catch (error) {
        console.error('Error loading investment types:', error);
      }
    };

    loadTiposInvestimento();
  }, []);

  useEffect(() => {
    const loadTransaction = async () => {
      if (!isEdit || !id) return;

      setLoading(true);
      setError('');

      try {
        const res = await transacaoApi.getById(Number(id));
        const t = res.data;

        setFormData({
          dataTransacao: toDateTimeLocal(t.dataTransacao),
          dataVencimento: toDateTimeLocal(t.dataVencimento),
          instituicao: t.instituicao,
          valor: t.valor,
          quantidade: t.quantidade,
          codInvestimento: t.codInvestimento ?? 0,
          tipoTransacao: t.tipoTransacao ?? 'buy',
          idUsuario: t.idUsuario ?? 1,
        });

        // Ensure the current codInvestimento exists in the select options
        if (t.codInvestimento) {
          setTiposInvestimento((prev) => {
            const exists = prev.some((x) => x.codInvestimento === t.codInvestimento);
            if (exists) return prev;
            return [
              ...prev,
              {
                codInvestimento: t.codInvestimento,
                incideIof: 'N',
                incideIr: 'N',
                descricao: t.descricaoInvestimento || 'Unknown',
              } as unknown as TipoInvestimento,
            ];
          });
        }
      } catch (e) {
        setError('Failed to load transaction for editing.');
      } finally {
        setLoading(false);
      }
    };

    loadTransaction();
  }, [isEdit, id]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'valor' || name === 'quantidade' || name === 'codInvestimento' 
        ? Number(value) 
        : value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // Validate required fields
      if (
        !formData.instituicao ||
        !formData.valor ||
        !formData.quantidade ||
        !formData.codInvestimento ||
        !formData.tipoTransacao
      ) {
        setError('Please fill in all required fields');
        setLoading(false);
        return;
      }

      if (isEdit && id) {
        await transacaoApi.update(Number(id), formData as any);
      } else {
        await transacaoApi.create(formData as any);
      }

      navigate('/transactions');
    } catch (error) {
      setError(isEdit ? 'Failed to update transaction. Please try again.' : 'Failed to create transaction. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/transactions');
  };

  return (
    <AppLayout>
      <div className="transacao-form-container">
        <div className="transacao-form-card">
          <div className="form-header">
            <h1>{isEdit ? 'Edit Transaction' : 'Add New Transaction'}</h1>
            <p>Record your investment transaction</p>
          </div>

          <form onSubmit={handleSubmit} className="transacao-form">
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="dataTransacao" className="form-label">
                  Transaction Date *
                </label>
                <input
                  type="datetime-local"
                  id="dataTransacao"
                  name="dataTransacao"
                  value={formData.dataTransacao}
                  onChange={handleInputChange}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="dataVencimento" className="form-label">
                  Maturity Date
                </label>
                <input
                  type="datetime-local"
                  id="dataVencimento"
                  name="dataVencimento"
                  value={formData.dataVencimento}
                  onChange={handleInputChange}
                  className="form-input"
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="instituicao" className="form-label">
                  Institution *
                </label>
                <input
                  type="text"
                  id="instituicao"
                  name="instituicao"
                  value={formData.instituicao}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder="e.g., Banco do Brasil, Nubank"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="codInvestimento" className="form-label">
                  Investment Type *
                </label>
                <select
                  id="codInvestimento"
                  name="codInvestimento"
                  value={formData.codInvestimento}
                  onChange={handleInputChange}
                  className="form-select"
                  required
                >
                  <option value={0}>Select investment type</option>
                  {tiposInvestimento.map(tipo => (
                    <option key={tipo.codInvestimento} value={tipo.codInvestimento}>
                      {tipo.descricao}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="tipoTransacao" className="form-label">
                  Transaction Type *
                </label>
                <select
                  id="tipoTransacao"
                  name="tipoTransacao"
                  value={formData.tipoTransacao}
                  onChange={handleInputChange}
                  className="form-select"
                  required
                >
                  <option value="buy">buy</option>
                  <option value="sell">sell</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="valor" className="form-label">
                  Value (R$) *
                </label>
                <input
                  type="number"
                  id="valor"
                  name="valor"
                  value={formData.valor}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder="0.00"
                  step="0.01"
                  min="0"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="quantidade" className="form-label">
                  Quantity *
                </label>
                <input
                  type="number"
                  id="quantidade"
                  name="quantidade"
                  value={formData.quantidade}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder="0"
                  step="0.01"
                  min="0"
                  required
                />
              </div>
            </div>

            {error && (
              <div className="error-message">
                {error}
              </div>
            )}

            <div className="form-actions">
              <button
                type="button"
                onClick={handleCancel}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
              >
                {loading
                  ? isEdit
                    ? 'Updating...'
                    : 'Creating...'
                  : isEdit
                    ? 'Update Transaction'
                    : 'Create Transaction'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </AppLayout>
  );
};

export default TransacaoFormPage;
