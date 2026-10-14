import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/axios';
import { toast } from 'sonner';
import { 
  DollarSign, 
  TrendingUp, 
  CheckCircle, 
  AlertCircle, 
  CreditCard,
  Calendar,
  FileSpreadsheet
} from 'lucide-react';

interface FaturamentoRelatorio {
  totalDia: number;
  totalMes: number;
  totalRecebido: number;
  pagamentosPendentes: number;
  pagamentosPagos: number;
  pagamentosCancelados: number;
}

interface ConsultaFinanceiro {
  id: string;
  dataHora: string;
  status: string;
  pacienteNome?: string;
  medicoNome?: string;
  pagamentoId?: string;
}

export function Faturamento() {
  const queryClient = useQueryClient();
  const [downloadingCsv, setDownloadingCsv] = useState(false);

  const { data: relatorio, isLoading: loadingRelatorio } = useQuery<FaturamentoRelatorio>({
    queryKey: ['faturamento-relatorio'],
    queryFn: async () => {
      const res = await api.get('/api/faturamento/relatorio');
      return res.data;
    }
  });

  const { data: consultas, isLoading: loadingConsultas } = useQuery<ConsultaFinanceiro[]>({
    queryKey: ['consultas-financeiro'],
    queryFn: async () => {
      const res = await api.get('/api/consultas');
      return res.data;
    }
  });

  const pagarMutation = useMutation({
    mutationFn: async (pagamentoId: string) => {
      await api.post(`/api/pagamentos/${pagamentoId}/pagar`);
    },
    onSuccess: () => {
      toast.success('Pagamento confirmado e quitado com sucesso!');
      queryClient.invalidateQueries({ queryKey: ['faturamento-relatorio'] });
      queryClient.invalidateQueries({ queryKey: ['consultas-financeiro'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const estornarMutation = useMutation({
    mutationFn: async (pagamentoId: string) => {
      await api.post(`/api/pagamentos/${pagamentoId}/estornar`);
    },
    onSuccess: () => {
      toast.success('Pagamento estornado com sucesso (retornado a PENDENTE)!');
      queryClient.invalidateQueries({ queryKey: ['faturamento-relatorio'] });
      queryClient.invalidateQueries({ queryKey: ['consultas-financeiro'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const handleDownloadCsv = async () => {
    setDownloadingCsv(true);
    try {
      const response = await api.get('/api/faturamento/relatorio-csv', {
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'text/csv;charset=utf-8;' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'relatorio-financeiro-medflow.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('Relatório CSV financeiro baixado com sucesso!');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDownloadingCsv(false);
    }
  };

  const formatCurrency = (val?: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <DollarSign className="text-emerald-600" /> Gestão Financeira & Faturamento
          </h1>
          <p className="text-sm text-gray-500">
            Acompanhamento em tempo real de receitas, conciliação e relatórios de fluxo de caixa
          </p>
        </div>
        <button
          onClick={handleDownloadCsv}
          disabled={downloadingCsv}
          className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium shadow-sm transition disabled:opacity-50"
        >
          <FileSpreadsheet size={16} />
          {downloadingCsv ? 'Exportando CSV...' : 'Exportar Relatório CSV'}
        </button>
      </div>

      {/* Cards de Métricas */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-center text-gray-500 mb-2">
            <span className="text-sm font-medium">Receita Hoje</span>
            <TrendingUp size={18} className="text-emerald-500" />
          </div>
          <p className="text-2xl font-bold text-gray-900">
            {loadingRelatorio ? '...' : formatCurrency(relatorio?.totalDia)}
          </p>
          <span className="text-xs text-gray-400">Total liquidado no dia</span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-center text-gray-500 mb-2">
            <span className="text-sm font-medium">Receita Mês</span>
            <Calendar size={18} className="text-blue-500" />
          </div>
          <p className="text-2xl font-bold text-gray-900">
            {loadingRelatorio ? '...' : formatCurrency(relatorio?.totalMes)}
          </p>
          <span className="text-xs text-gray-400">Total acumulado no mês</span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-center text-gray-500 mb-2">
            <span className="text-sm font-medium">Pagamentos Concluídos</span>
            <CheckCircle size={18} className="text-emerald-500" />
          </div>
          <p className="text-2xl font-bold text-emerald-600">
            {loadingRelatorio ? '...' : relatorio?.pagamentosPagos || 0}
          </p>
          <span className="text-xs text-gray-400">Consultas com pagamento PAGO</span>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-center text-gray-500 mb-2">
            <span className="text-sm font-medium">Pagamentos Pendentes</span>
            <AlertCircle size={18} className="text-amber-500" />
          </div>
          <p className="text-2xl font-bold text-amber-600">
            {loadingRelatorio ? '...' : relatorio?.pagamentosPendentes || 0}
          </p>
          <span className="text-xs text-gray-400">Aguardando confirmação</span>
        </div>
      </div>

      {/* Tabela de Transações */}
      <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
        <div className="p-4 border-b bg-gray-50 flex justify-between items-center">
          <h2 className="text-base font-bold text-gray-800 flex items-center gap-2">
            <CreditCard size={18} className="text-blue-600" /> Transações e Pagamentos de Consultas
          </h2>
          <span className="text-xs text-gray-500">Valor padrão da consulta: R$ 150,00</span>
        </div>

        {loadingConsultas ? (
          <div className="p-8 text-center text-gray-500 animate-pulse">Carregando transações financeiras...</div>
        ) : (!consultas || consultas.length === 0) ? (
          <div className="p-8 text-center text-gray-500">Nenhuma consulta cadastrada no sistema.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-xs text-gray-500 uppercase">
                <tr>
                  <th className="px-6 py-3 text-left">Data/Hora</th>
                  <th className="px-6 py-3 text-left">Paciente</th>
                  <th className="px-6 py-3 text-left">Médico</th>
                  <th className="px-6 py-3 text-left">Valor</th>
                  <th className="px-6 py-3 text-left">Status Consulta</th>
                  <th className="px-6 py-3 text-left">Ações Financeiras</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {consultas.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900 font-medium">
                      {new Date(c.dataHora).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-700">
                      {c.pacienteNome || 'Paciente'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-600">
                      {c.medicoNome || 'Médico'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap font-semibold text-gray-900">
                      R$ 150,00
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                        c.status === 'AGENDADA' ? 'bg-yellow-100 text-yellow-800' :
                        c.status === 'CONFIRMADA' ? 'bg-green-100 text-green-800' :
                        c.status === 'REALIZADA' ? 'bg-emerald-100 text-emerald-800' :
                        'bg-red-100 text-red-800'
                      }`}>
                        {c.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap space-x-2">
                      {c.pagamentoId ? (
                        <div className="inline-flex items-center gap-2">
                          <button
                            onClick={() => pagarMutation.mutate(c.pagamentoId!)}
                            disabled={pagarMutation.isPending}
                            className="px-2.5 py-1 text-xs font-semibold rounded bg-emerald-100 text-emerald-800 hover:bg-emerald-200 transition"
                          >
                            Quitar (Pagar)
                          </button>
                          <button
                            onClick={() => estornarMutation.mutate(c.pagamentoId!)}
                            disabled={estornarMutation.isPending}
                            className="px-2.5 py-1 text-xs font-semibold rounded bg-amber-100 text-amber-800 hover:bg-amber-200 transition"
                          >
                            Estornar
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400">Sem pagamento gerado</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
