import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/axios';
import { toast } from 'sonner';
import { Activity, Clock, HeartPulse, Thermometer, Gauge } from 'lucide-react';

interface Consulta {
  id: string;
  dataHora: string;
  status: string;
  pacienteId: string;
  pacienteNome?: string;
  medicoNome?: string;
}

export function Triagem() {
  const queryClient = useQueryClient();
  const [selectedConsultaId, setSelectedConsultaId] = useState<string>('');
  const [pressaoArterial, setPressaoArterial] = useState('120x80');
  const [temperatura, setTemperatura] = useState('36.5');
  const [batimentos, setBatimentos] = useState('75');
  const [sintomas, setSintomas] = useState('');
  const [prioridade, setPrioridade] = useState<'EMERGENCIA' | 'ALTA' | 'MEDIA' | 'BAIXA'>('MEDIA');

  const { data: consultas, isLoading } = useQuery<Consulta[]>({
    queryKey: ['consultas-hoje'],
    queryFn: async () => {
      const res = await api.get('/api/consultas/hoje');
      return res.data;
    }
  });

  const triagemMutation = useMutation({
    mutationFn: async (dados: any) => {
      const res = await api.post('/api/triagens', dados);
      return res.data;
    },
    onSuccess: () => {
      toast.success('Classificação de triagem (Manchester) registrada com sucesso!');
      setSelectedConsultaId('');
      setSintomas('');
      queryClient.invalidateQueries({ queryKey: ['consultas-hoje'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedConsultaId) {
      toast.error('Por favor, selecione uma consulta para realizar a triagem.');
      return;
    }

    triagemMutation.mutate({
      consultaId: selectedConsultaId,
      pressaoArterial,
      temperatura: parseFloat(temperatura) || 36.5,
      batimentos: parseInt(batimentos, 10) || 75,
      sintomas,
      prioridade
    });
  };

  return (
    <div className="space-y-6">
      <div className="border-b pb-4">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <Activity className="text-blue-600" /> Triagem Clínica & Acolhimento
        </h1>
        <p className="text-sm text-gray-500">
          Registro de sinais vitais e classificação de urgência pelo Protocolo de Manchester
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Formulário de Triagem */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
            <HeartPulse className="text-red-500" /> Nova Avaliação de Triagem
          </h2>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Selecione a Consulta / Paciente *
              </label>
              <select
                value={selectedConsultaId}
                onChange={(e) => setSelectedConsultaId(e.target.value)}
                required
                className="w-full border border-gray-300 rounded-lg p-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="">-- Escolha a consulta de hoje --</option>
                {consultas?.filter(c => c.status !== 'CANCELADA').map(c => (
                  <option key={c.id} value={c.id}>
                    {new Date(c.dataHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - {c.pacienteNome || `Paciente ID: ${c.pacienteId.substring(0, 8)}`} (Médico: {c.medicoNome || 'Dr(a)'} | Status: {c.status})
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                  <Gauge size={16} className="text-blue-500" /> Pressão Arterial
                </label>
                <input
                  type="text"
                  placeholder="Ex: 120x80"
                  value={pressaoArterial}
                  onChange={(e) => setPressaoArterial(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                  <Thermometer size={16} className="text-orange-500" /> Temperatura (°C)
                </label>
                <input
                  type="number"
                  step="0.1"
                  placeholder="36.5"
                  value={temperatura}
                  onChange={(e) => setTemperatura(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                  <HeartPulse size={16} className="text-red-500" /> Freq. Cardíaca (bpm)
                </label>
                <input
                  type="number"
                  placeholder="75"
                  value={batimentos}
                  onChange={(e) => setBatimentos(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Sintomas e Queixa Principal
              </label>
              <textarea
                rows={3}
                placeholder="Descreva os sintomas relatados pelo paciente, duração e intensidade da dor..."
                value={sintomas}
                onChange={(e) => setSintomas(e.target.value)}
                className="w-full border border-gray-300 rounded-lg p-2.5 text-sm focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Classificação de Risco (Manchester) *
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <button
                  type="button"
                  onClick={() => setPrioridade('EMERGENCIA')}
                  className={`p-3 rounded-lg border text-left transition ${
                    prioridade === 'EMERGENCIA' 
                      ? 'border-red-600 bg-red-600 text-white font-bold shadow' 
                      : 'border-red-200 bg-red-50 text-red-800 hover:bg-red-100'
                  }`}
                >
                  <p className="text-xs font-semibold uppercase">Vermelho</p>
                  <p className="text-sm font-bold">Emergência</p>
                  <p className="text-[10px] opacity-80 mt-1">Imediato</p>
                </button>

                <button
                  type="button"
                  onClick={() => setPrioridade('ALTA')}
                  className={`p-3 rounded-lg border text-left transition ${
                    prioridade === 'ALTA' 
                      ? 'border-amber-500 bg-amber-500 text-white font-bold shadow' 
                      : 'border-amber-200 bg-amber-50 text-amber-800 hover:bg-amber-100'
                  }`}
                >
                  <p className="text-xs font-semibold uppercase">Amarelo</p>
                  <p className="text-sm font-bold">Muito Urgente</p>
                  <p className="text-[10px] opacity-80 mt-1">Até 60 min</p>
                </button>

                <button
                  type="button"
                  onClick={() => setPrioridade('MEDIA')}
                  className={`p-3 rounded-lg border text-left transition ${
                    prioridade === 'MEDIA' 
                      ? 'border-emerald-600 bg-emerald-600 text-white font-bold shadow' 
                      : 'border-emerald-200 bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
                  }`}
                >
                  <p className="text-xs font-semibold uppercase">Verde</p>
                  <p className="text-sm font-bold">Pouco Urgente</p>
                  <p className="text-[10px] opacity-80 mt-1">Até 120 min</p>
                </button>

                <button
                  type="button"
                  onClick={() => setPrioridade('BAIXA')}
                  className={`p-3 rounded-lg border text-left transition ${
                    prioridade === 'BAIXA' 
                      ? 'border-blue-600 bg-blue-600 text-white font-bold shadow' 
                      : 'border-blue-200 bg-blue-50 text-blue-800 hover:bg-blue-100'
                  }`}
                >
                  <p className="text-xs font-semibold uppercase">Azul</p>
                  <p className="text-sm font-bold">Não Urgente</p>
                  <p className="text-[10px] opacity-80 mt-1">Até 240 min</p>
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={triagemMutation.isPending}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 px-4 rounded-lg shadow transition disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {triagemMutation.isPending ? 'Gravando Triagem...' : 'Salvar Triagem Clínica'}
            </button>
          </form>
        </div>

        {/* Fila de Consultas do Dia */}
        <div className="bg-white rounded-xl shadow-sm border p-6 space-y-4">
          <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2 border-b pb-3">
            <Clock size={18} className="text-blue-600" /> Pacientes na Clínica Hoje
          </h2>

          {isLoading ? (
            <p className="text-sm text-gray-400">Carregando consultas...</p>
          ) : consultas?.length === 0 ? (
            <p className="text-sm text-gray-500 text-center py-6">Nenhum paciente agendado para hoje.</p>
          ) : (
            <div className="space-y-3 max-h-[500px] overflow-y-auto pr-1">
              {consultas?.map((c) => (
                <div 
                  key={c.id} 
                  onClick={() => setSelectedConsultaId(c.id)}
                  className={`p-3 rounded-lg border cursor-pointer transition ${
                    selectedConsultaId === c.id 
                      ? 'border-blue-500 bg-blue-50/50' 
                      : 'border-gray-200 hover:bg-gray-50'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <span className="font-semibold text-sm text-gray-900">
                      {c.pacienteNome || 'Paciente'}
                    </span>
                    <span className="text-xs bg-gray-100 text-gray-700 px-2 py-0.5 rounded font-medium">
                      {new Date(c.dataHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">Médico: {c.medicoNome || 'Corpo Clínico'}</p>
                  <div className="mt-2 flex items-center justify-between text-xs">
                    <span className="text-gray-400">Status: {c.status}</span>
                    <span className="text-blue-600 font-medium">Clique para triar</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
