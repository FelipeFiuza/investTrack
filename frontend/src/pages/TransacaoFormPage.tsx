import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { transacaoApi, tipoInvestimentoApi } from '../services/api';
import { TransacaoCreateDTO, TipoInvestimento } from '../types';
import './TransacaoFormPage.css';

const TransacaoFormPage: React.FC = () => {
  const [formData, setFormData] = useState<TransacaoCreateDTO>({
    dataTransacao: new Date().toISOString().slice(0, 16),
    dataVencimento: '',
    instituicao: '',
    valor: 0,
    quantidade: 0,
    codInvestimento: 0,
    idUsuario: 1 // This should come from the logged-in user
  });
  
  const [tiposInvestimento, setTiposInvestimento] = useState<TipoInvestimento[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

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
      if (!formData.instituicao || !formData.valor || !formData.quantidade || !formData.codInvestimento) {
        setError('Please fill in all required fields');
        setLoading(false);
        return;
      }

      // For now, just log the data since the API might not be ready
      console.log('Transaction data:', formData);
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Navigate back to dashboard
      navigate('/dashboard');
    } catch (error) {
      setError('Failed to create transaction. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/dashboard');
  };

  return (
    <div className="transacao-form-container">
      <div className="transacao-form-card">
        <div className="form-header">
          <h1>Add New Transaction</h1>
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
              {loading ? 'Creating...' : 'Create Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default TransacaoFormPage;
