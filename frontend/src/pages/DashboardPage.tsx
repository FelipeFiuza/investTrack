import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { apuracaoIndiceApi } from '../services/api';
import { ApuracaoIndice } from '../types';
import AppLayout from '../components/AppLayout';
import './DashboardPage.css';

const DashboardPage: React.FC = () => {
  const [chartData, setChartData] = useState<ApuracaoIndice[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // Mock data as provided by user
  const mockData = [
    {
      dataApuracao: "2024-01-08 00:00:00",
      valorFechamento: 1370.00
    },
    {
      dataApuracao: "2024-01-09 00:00:00",
      valorFechamento: 1360.00
    },
    {
      dataApuracao: "2024-01-10 00:00:00",
      valorFechamento: 1376.00
    },
    {
      dataApuracao: "2024-01-11 00:00:00",
      valorFechamento: 1358.00
    },
    {
      dataApuracao: "2024-01-12 00:00:00",
      valorFechamento: 1354.00
    }
  ];

  useEffect(() => {
    const loadData = async () => {
      try {
        // For now, use mock data. In a real app, you'd fetch from API
        setChartData(mockData as unknown as ApuracaoIndice[]);
        setLoading(false);
      } catch (error) {
        console.error('Error loading chart data:', error);
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const handleAddTransaction = () => {
    navigate('/transacao');
  };

  // Format data for the chart
  const formattedData = chartData.map(item => ({
    date: new Date(item.dataApuracao).toLocaleDateString('pt-BR', { 
      month: 'short', 
      day: 'numeric' 
    }),
    value: item.valorFechamento
  }));

  return (
    <AppLayout>
    <div className="dashboard-container">
      <main className="dashboard-main">
        <div className="dashboard-grid">
          <div className="chart-card">
            <div className="card-header">
              <h2 className="card-title">Investment Performance</h2>
              <p className="card-subtitle">Portfolio value over time</p>
            </div>
            
            <div className="chart-container">
              {loading ? (
                <div className="loading-spinner">
                  <div className="spinner"></div>
                  <p>Loading chart data...</p>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={400}>
                  <LineChart data={formattedData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis 
                      dataKey="date" 
                      stroke="#4a5568"
                      fontSize={12}
                    />
                    <YAxis 
                      stroke="#4a5568"
                      fontSize={12}
                      tickFormatter={(value) => `R$ ${value.toLocaleString()}`}
                    />
                    <Tooltip 
                      formatter={(value: number) => [`R$ ${value.toLocaleString()}`, 'Value']}
                      labelFormatter={(label) => `Date: ${label}`}
                      contentStyle={{
                        backgroundColor: 'white',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px',
                        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
                      }}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="value" 
                      stroke="#667eea" 
                      strokeWidth={3}
                      dot={{ fill: '#667eea', strokeWidth: 2, r: 6 }}
                      activeDot={{ r: 8, stroke: '#667eea', strokeWidth: 2 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon">📈</div>
              <div className="stat-content">
                <h3>Total Value</h3>
                <p className="stat-value">R$ 1,354.00</p>
                <p className="stat-change positive">+2.1% from last week</p>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon">💰</div>
              <div className="stat-content">
                <h3>Investments</h3>
                <p className="stat-value">5</p>
                <p className="stat-change">Active positions</p>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon">📊</div>
              <div className="stat-content">
                <h3>Performance</h3>
                <p className="stat-value">+12.5%</p>
                <p className="stat-change positive">This month</p>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Floating Action Button */}
      <button 
        className="fab"
        onClick={handleAddTransaction}
        title="Add new transaction"
      >
        +
      </button>
    </div>
    </AppLayout>
  );
};

export default DashboardPage;
