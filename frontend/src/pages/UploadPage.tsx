import React, { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as XLSX from 'xlsx';
import AppLayout from '../components/AppLayout';
import { transacaoApi } from '../services/api';
import { B3ImportPreviewRow, parseB3Rows } from '../utils/b3Import';
import './UploadPage.css';

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

const toCents = (reais: number): number => Math.round(reais * 100);

const formatDate = (iso: string): string => {
  const [date] = iso.split('T');
  const [y, m, d] = date.split('-');
  return `${d}/${m}/${y}`;
};

const formatCurrency = (value: number): string =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

const UploadPage: React.FC = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [fileName, setFileName] = useState('');
  const [rows, setRows] = useState<B3ImportPreviewRow[]>([]);
  const [ignoredCount, setIgnoredCount] = useState(0);
  const [error, setError] = useState('');
  const [parsing, setParsing] = useState(false);
  const [saving, setSaving] = useState(false);

  const selectedRows = rows.filter((r) => r.selected);

  const resetPreview = () => {
    setRows([]);
    setIgnoredCount(0);
    setFileName('');
    setError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setParsing(true);
    setError('');
    setRows([]);
    setIgnoredCount(0);
    setFileName(file.name);

    try {
      if (!file.name.toLowerCase().endsWith('.xlsx')) {
        throw new Error('Please select a .xlsx file exported by B3.');
      }

      const buffer = await file.arrayBuffer();
      const workbook = XLSX.read(buffer, { type: 'array', cellDates: true });
      let parsed: ReturnType<typeof parseB3Rows> | null = null;

      for (const sheetName of workbook.SheetNames) {
        const sheet = workbook.Sheets[sheetName];
        const sheetRows = XLSX.utils.sheet_to_json(sheet, {
          header: 1,
          raw: true,
          defval: '',
        }) as unknown[][];
        try {
          parsed = parseB3Rows(sheetRows);
          break;
        } catch {
          // try next sheet
        }
      }

      if (!parsed) {
        throw new Error('Could not find the B3 header row (Entrada/Saída, Movimentação, Produto).');
      }

      setRows(parsed.transacoes);
      setIgnoredCount(parsed.ignoradas);
      if (parsed.transacoes.length === 0) {
        setError('No buy/sell rows found. Only "Transferência - Liquidação" with Crédito or Débito is imported.');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to read the spreadsheet.');
      setFileName('');
    } finally {
      setParsing(false);
    }
  };

  const toggleRow = (rowNumber: number) => {
    setRows((prev) =>
      prev.map((row) => (row.rowNumber === rowNumber ? { ...row, selected: !row.selected } : row))
    );
  };

  const toggleAll = (selected: boolean) => {
    setRows((prev) => prev.map((row) => ({ ...row, selected })));
  };

  const handleConfirm = async () => {
    if (selectedRows.length === 0) return;

    setSaving(true);
    setError('');
    try {
      await transacaoApi.importBatch({
        idUsuario: getUserId(),
        transacoes: selectedRows.map((row) => ({
          dataTransacao: row.dataTransacao,
          instituicao: row.instituicao,
          tipoTransacao: row.tipoTransacao,
          valor: toCents(row.valorTotal),
          valorUnitario: toCents(row.valorUnitario),
          quantidade: row.quantidade,
          ticker: row.ticker,
          produto: row.produto,
        })),
      });
      navigate('/transactions');
    } catch {
      setError('Failed to save the selected transactions. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppLayout>
      <div className="upload-page">
        <header className="upload-header">
          <h1>Upload</h1>
          <p>Import stock trades from a B3 .xlsx statement.</p>
        </header>

        <section className="upload-card">
          <label className="upload-dropzone">
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
              onChange={handleFileChange}
              disabled={parsing || saving}
            />
            <span className="upload-dropzone-title">
              {parsing ? 'Reading file...' : 'Choose a B3 .xlsx file'}
            </span>
            <span className="upload-dropzone-hint">
              {fileName || 'Only rows with Movimentação = Transferência - Liquidação are imported.'}
            </span>
          </label>
        </section>

        {error && <div className="upload-error">{error}</div>}

        {rows.length > 0 && (
          <section className="upload-preview">
            <div className="upload-preview-header">
              <div>
                <h2>Confirm transactions</h2>
                <p>
                  {selectedRows.length} of {rows.length} selected
                  {ignoredCount > 0 ? ` · ${ignoredCount} other rows ignored` : ''}
                </p>
              </div>
              <div className="upload-preview-actions">
                <button type="button" className="btn btn-secondary" onClick={resetPreview} disabled={saving}>
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleConfirm}
                  disabled={saving || selectedRows.length === 0}
                >
                  {saving ? 'Saving...' : `Confirm ${selectedRows.length} transaction${selectedRows.length === 1 ? '' : 's'}`}
                </button>
              </div>
            </div>

            <div className="upload-table-wrapper">
              <table className="upload-table">
                <thead>
                  <tr>
                    <th className="upload-check-col">
                      <input
                        type="checkbox"
                        checked={rows.length > 0 && selectedRows.length === rows.length}
                        onChange={(e) => toggleAll(e.target.checked)}
                        aria-label="Select all"
                      />
                    </th>
                    <th>Date</th>
                    <th>Buy/Sell</th>
                    <th>Product</th>
                    <th>Institution</th>
                    <th>Qty</th>
                    <th>Unit price</th>
                    <th>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.rowNumber} className={row.selected ? '' : 'upload-row-off'}>
                      <td>
                        <input
                          type="checkbox"
                          checked={row.selected}
                          onChange={() => toggleRow(row.rowNumber)}
                          aria-label={`Select row ${row.rowNumber}`}
                        />
                      </td>
                      <td>{formatDate(row.dataTransacao)}</td>
                      <td>
                        <span className={`upload-type upload-type-${row.tipoTransacao}`}>
                          {row.tipoTransacao === 'buy' ? 'Buy' : 'Sell'}
                        </span>
                      </td>
                      <td>{row.produto}</td>
                      <td>{row.instituicao || '—'}</td>
                      <td>{row.quantidade}</td>
                      <td>{formatCurrency(row.valorUnitario)}</td>
                      <td>{formatCurrency(row.valorTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}
      </div>
    </AppLayout>
  );
};

export default UploadPage;
