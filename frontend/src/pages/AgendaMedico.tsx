import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/axios';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { 
  Calendar, 
  Clock, 
  CheckCircle, 
  FileText, 
  AlertCircle, 
  User, 
  Activity, 
  X
} from 'lucide-react';

interface ConsultaAgenda {
  id: string;
  dataHora: string;
  status: 'AGENDADA' | 'CONFIRMADA' | 'REALIZADA' | 'CANCELADA';
  pacienteId: string;
  pacienteNome?: string;
  medicoId: string;
  medicoNome?: string;
  pagamentoId?: string;
}

interface TriagemInfo {
  pressaoArterial?: string;
  temperatura?: number;
  batimentos?: number;
  sintomas?: string;
  prioridade?: 'EMERGENCIA' | 'ALTA' | 'MEDIA' | 'BAIXA';
}

export function AgendaMedico() {
  const queryClient = useQueryClient();
  const [selectedConsulta, setSelectedConsulta] = useState<ConsultaAgenda | null>(null);
  const [triagemData, setTriagemData] = useState<TriagemInfo | null>(null);
  const [loadingTriagem, setLoadingTriagem] = useState(false);

  const { data: consultas, isLoading, error } = useQuery<ConsultaAgenda[]>({
    queryKey: ['agenda-medico'],
    queryFn: async () => {
      const res = await api.get('/api/consultas/agenda-hoje');
      return res.data;
    }
  });

  const realizarMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.put(`/api/consultas/${id}/realizar`);
    },
    onSuccess: () => {
      toast.success('Consulta concluída e marcada como REALIZADA!');
      queryClient.invalidateQueries({ queryKey: ['agenda-medico'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const cancelarMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.put(`/api/consultas/${id}/cancelar`);
    },
    onSuccess: () => {
      toast.success('Consulta cancelada com sucesso.');
      queryClient.invalidateQueries({ queryKey: ['agenda-medico'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const abrirTriagem = async (c: ConsultaAgenda) => {
    setSelectedConsulta(c);
    setLoadingTriagem(true);
    setTriagemData(null);
    try {
      const res = await api.get(`/api/triagens/consulta/${c.id}`);
      setTriagemData(res.data);
    } catch {
      setTriagemData(null);
    } finally {
      setLoadingTriagem(false);
    }
  };

  const renderPrioridadeBadge = (prioridade?: string) => {
    switch (prioridade) {
      case 'EMERGENCIA':
        return <span className="bg-red-600 text-white text-xs px-2.5 py-1 rounded-full font-bold flex items-center gap-1"><AlertCircle size={12}/> Vermelho (Emergência)</span>;
      case 'ALTA':
        return <span className="bg-amber-500 text-white text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1"><AlertCircle size={12}/> Amarelo (Alta Prioridade)</span>;
      case 'MEDIA':
        return <span className="bg-emerald-600 text-white text-xs px-2.5 py-1 rounded-full font-medium flex items-center gap-1"><Activity size={12}/> Verde (Média)</span>;
      case 'BAIXA':
        return <span className="bg-blue-600 text-white text-xs px-2.5 py-1 rounded-full font-medium">Azul (Não Urgente)</span>;
      default:
        return <span className="bg-gray-200 text-gray-700 text-xs px-2.5 py-1 rounded-full">Aguardando Triagem</span>;
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/4"></div>
        <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        <div className="space-y-3 mt-6">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-24 bg-white rounded-lg shadow-sm border border-gray-100 p-4"></div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 p-6 rounded-lg text-red-700 flex items-center gap-3">
        <AlertCircle size={24} />
        <div>
          <h3 className="font-semibold">Erro ao carregar agenda médica</h3>
          <p className="text-sm">{getErrorMessage(error)}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Calendar className="text-blue-600" /> Minha Fila de Atendimento do Dia
          </h1>
          <p className="text-sm text-gray-500">
            Fila priorizada clinicamente pelo Protocolo de Manchester (Casos mais graves no topo)
          </p>
        </div>
        <div className="text-sm bg-blue-50 text-blue-700 px-4 py-2 rounded-lg font-medium self-start sm:self-auto">
          {consultas?.length || 0} paciente(s) agendados hoje
        </div>
      </div>

      {(!consultas || consultas.length === 0) ? (
        <div className="bg-white rounded-xl p-12 text-center border border-dashed border-gray-300">
          <Calendar className="mx-auto h-12 w-12 text-gray-400 mb-3" />
          <h3 className="text-lg font-medium text-gray-900">Nenhum atendimento na fila de hoje</h3>
          <p className="text-gray-500 text-sm mt-1">
            As consultas agendadas e confirmadas para o seu CRM aparecerão aqui ordenadas por risco.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {consultas.map((c) => (
            <div 
              key={c.id} 
              className={`bg-white rounded-xl shadow-sm border p-5 transition-all hover:shadow-md ${
                c.status === 'REALIZADA' ? 'border-green-200 bg-green-50/20 opacity-80' : 
                c.status === 'CANCELADA' ? 'border-red-200 bg-red-50/20 opacity-60' : 'border-gray-200'
              }`}
            >
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-lg text-gray-900 flex items-center gap-2">
                      <User size={18} className="text-blue-600" />
                      {c.pacienteNome || `Paciente ID: ${c.pacienteId.substring(0, 8)}...`}
                    </span>
                    <span className="flex items-center gap-1 text-sm text-gray-600 bg-gray-100 px-2.5 py-0.5 rounded">
                      <Clock size={14} />
                      {new Date(c.dataHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${
                      c.status === 'AGENDADA' ? 'bg-yellow-100 text-yellow-800' :
                      c.status === 'CONFIRMADA' ? 'bg-green-100 text-green-800' :
                      c.status === 'REALIZADA' ? 'bg-emerald-100 text-emerald-800' :
                      'bg-red-100 text-red-800'
                    }`}>
                      {c.status}
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => abrirTriagem(c)}
                      className="text-xs text-blue-600 hover:text-blue-800 font-medium underline flex items-center gap-1"
                    >
                      <Activity size={14} /> Ver Sinais Vitais & Triagem
                    </button>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <Link
                    to={`/prontuario/${c.id}`}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 text-sm font-medium rounded-lg text-blue-700 bg-blue-50 hover:bg-blue-100 border border-blue-200 transition"
                  >
                    <FileText size={16} /> Prontuário & Prescrições
                  </Link>

                  {c.status === 'CONFIRMADA' && (
                    <button
                      onClick={() => realizarMutation.mutate(c.id)}
                      disabled={realizarMutation.isPending}
                      className="inline-flex items-center gap-1.5 px-3.5 py-2 text-sm font-medium rounded-lg text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm transition disabled:opacity-50"
                    >
                      <CheckCircle size={16} /> Realizar Atendimento
                    </button>
                  )}

                  {c.status !== 'REALIZADA' && c.status !== 'CANCELADA' && (
                    <button
                      onClick={() => {
                        if (confirm('Deseja realmente cancelar esta consulta? A política de reembolso será aplicada.')) {
                          cancelarMutation.mutate(c.id);
                        }
                      }}
                      disabled={cancelarMutation.isPending}
                      className="inline-flex items-center gap-1 px-3 py-2 text-sm font-medium rounded-lg text-red-600 hover:bg-red-50 transition"
                    >
                      <X size={16} /> Cancelar
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal de Detalhes da Triagem */}
      {selectedConsulta && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-xl space-y-4">
            <div className="flex justify-between items-center border-b pb-3">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Activity className="text-blue-600" /> Triagem Clínica de Manchester
              </h3>
              <button 
                onClick={() => setSelectedConsulta(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={20} />
              </button>
            </div>

            {loadingTriagem ? (
              <div className="text-center py-8 text-gray-500">Carregando dados da triagem...</div>
            ) : triagemData ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">Classificação de Risco:</span>
                  {renderPrioridadeBadge(triagemData.prioridade)}
                </div>
                <div className="grid grid-cols-3 gap-3 bg-gray-50 p-3 rounded-lg text-center">
                  <div>
                    <span className="text-xs text-gray-500">Pressão</span>
                    <p className="font-bold text-gray-800">{triagemData.pressaoArterial || 'N/A'}</p>
                  </div>
                  <div>
                    <span className="text-xs text-gray-500">Temperatura</span>
                    <p className="font-bold text-gray-800">{triagemData.temperatura ? `${triagemData.temperatura}°C` : 'N/A'}</p>
                  </div>
                  <div>
                    <span className="text-xs text-gray-500">Freq. Cardíaca</span>
                    <p className="font-bold text-gray-800">{triagemData.batimentos ? `${triagemData.batimentos} bpm` : 'N/A'}</p>
                  </div>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-700">Queixas e Sintomas Registrados:</span>
                  <p className="text-sm text-gray-600 bg-gray-50 p-3 rounded-lg mt-1 border">
                    {triagemData.sintomas || 'Nenhuma observação informada.'}
                  </p>
                </div>
              </div>
            ) : (
              <div className="text-center py-6 text-gray-500 space-y-2">
                <AlertCircle className="mx-auto text-yellow-500" size={32} />
                <p>Nenhuma triagem foi registrada ainda para esta consulta.</p>
                <p className="text-xs text-gray-400">O enfermeiro ou técnico de triagem deve avaliar o paciente na recepção.</p>
              </div>
            )}

            <div className="pt-3 border-t flex justify-end">
              <button
                onClick={() => setSelectedConsulta(null)}
                className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-lg text-sm font-medium"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
