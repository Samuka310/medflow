import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/axios';
import { toast } from 'sonner';
import { 
  Building2, 
  Calendar, 
  CheckCircle, 
  UserCheck, 
  CalendarClock, 
  FileSpreadsheet, 
  Plus, 
  X
} from 'lucide-react';

interface ConsultaRecepcao {
  id: string;
  dataHora: string;
  status: string;
  pacienteId: string;
  pacienteNome?: string;
  medicoId: string;
  medicoNome?: string;
  pagamentoId?: string;
}

export function Recepcao() {
  const queryClient = useQueryClient();
  const [remarcarModalConsulta, setRemarcarModalConsulta] = useState<ConsultaRecepcao | null>(null);
  const [novaDataHora, setNovaDataHora] = useState('');
  const [novoAgendamentoOpen, setNovoAgendamentoOpen] = useState(false);
  const [downloadingCsv, setDownloadingCsv] = useState(false);

  // Form novo agendamento
  const [selectedMedicoId, setSelectedMedicoId] = useState('');
  const [selectedPacienteId, setSelectedPacienteId] = useState('');
  const [agendamentoDataHora, setAgendamentoDataHora] = useState('');

  const { data: consultas, isLoading } = useQuery<ConsultaRecepcao[]>({
    queryKey: ['consultas-recepcao-hoje'],
    queryFn: async () => {
      const res = await api.get('/api/consultas/hoje');
      return res.data;
    }
  });

  const { data: medicos } = useQuery({
    queryKey: ['medicos-lista'],
    queryFn: async () => {
      const res = await api.get('/api/medicos');
      return res.data;
    }
  });

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => {
      const res = await api.get('/api/pacientes');
      return res.data;
    }
  });

  const checkinMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.put(`/api/consultas/${id}/checkin`);
    },
    onSuccess: () => {
      toast.success('Check-in presencial registrado! Paciente encaminhado para triagem.');
      queryClient.invalidateQueries({ queryKey: ['consultas-recepcao-hoje'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const confirmarMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.put(`/api/consultas/${id}/confirmar`);
    },
    onSuccess: () => {
      toast.success('Consulta confirmada com sucesso!');
      queryClient.invalidateQueries({ queryKey: ['consultas-recepcao-hoje'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const remarcarMutation = useMutation({
    mutationFn: async ({ id, dataHora }: { id: string; dataHora: string }) => {
      await api.put(`/api/consultas/${id}/remarcar`, { dataHora });
    },
    onSuccess: () => {
      toast.success('Consulta remarcada com sucesso!');
      setRemarcarModalConsulta(null);
      setNovaDataHora('');
      queryClient.invalidateQueries({ queryKey: ['consultas-recepcao-hoje'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const criarConsultaMutation = useMutation({
    mutationFn: async () => {
      await api.post('/api/consultas', {
        medicoId: selectedMedicoId,
        pacienteId: selectedPacienteId,
        dataHora: agendamentoDataHora
      });
    },
    onSuccess: () => {
      toast.success('Nova consulta agendada com sucesso!');
      setNovoAgendamentoOpen(false);
      setSelectedMedicoId('');
      setSelectedPacienteId('');
      setAgendamentoDataHora('');
      queryClient.invalidateQueries({ queryKey: ['consultas-recepcao-hoje'] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const handleDownloadCsv = async () => {
    setDownloadingCsv(true);
    try {
      const res = await api.get('/api/consultas/relatorio-csv', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv;charset=utf-8;' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'relatorio-consultas-medflow.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('Download do relatório CSV concluído!');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDownloadingCsv(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Building2 className="text-blue-600" /> Recepção & Acolhimento
          </h1>
          <p className="text-sm text-gray-500">
            Controle de chegada presencial (Check-in), agendamentos e agenda geral do dia
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setNovoAgendamentoOpen(true)}
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium shadow-sm transition"
          >
            <Plus size={16} /> Novo Agendamento
          </button>
          <button
            onClick={handleDownloadCsv}
            disabled={downloadingCsv}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg text-sm font-medium transition"
          >
            <FileSpreadsheet size={16} /> {downloadingCsv ? 'Baixando...' : 'CSV'}
          </button>
        </div>
      </div>

      {/* Lista de Consultas de Hoje */}
      <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
        <div className="p-4 border-b bg-gray-50 flex justify-between items-center">
          <h2 className="text-base font-bold text-gray-800 flex items-center gap-2">
            <Calendar className="text-blue-600" size={18} /> Agenda da Recepção de Hoje
          </h2>
          <span className="text-xs bg-blue-100 text-blue-800 font-semibold px-2.5 py-0.5 rounded-full">
            {consultas?.length || 0} agendamento(s)
          </span>
        </div>

        {isLoading ? (
          <div className="p-8 text-center text-gray-500 animate-pulse">Carregando consultas da recepção...</div>
        ) : (!consultas || consultas.length === 0) ? (
          <div className="p-8 text-center text-gray-500">Nenhum paciente agendado para o dia de hoje.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-xs text-gray-500 uppercase">
                <tr>
                  <th className="px-6 py-3 text-left">Horário</th>
                  <th className="px-6 py-3 text-left">Paciente</th>
                  <th className="px-6 py-3 text-left">Médico</th>
                  <th className="px-6 py-3 text-left">Status</th>
                  <th className="px-6 py-3 text-left">Ações Recepção</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {consultas.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap font-medium text-gray-900">
                      {new Date(c.dataHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-800 font-semibold">
                      {c.pacienteNome || 'Paciente'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-600">
                      {c.medicoNome || 'Corpo Clínico'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                        c.status === 'AGENDADA' ? 'bg-yellow-100 text-yellow-800' :
                        c.status === 'CONFIRMADA' ? 'bg-green-100 text-green-800' :
                        c.status === 'REALIZADA' ? 'bg-emerald-100 text-emerald-800' :
                        'bg-red-100 text-red-800'
                      }`}>
                        {c.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap space-x-2">
                      {c.status === 'AGENDADA' && (
                        <>
                          <button
                            onClick={() => checkinMutation.mutate(c.id)}
                            disabled={checkinMutation.isPending}
                            className="inline-flex items-center gap-1 px-3 py-1 bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200 rounded text-xs font-semibold transition"
                          >
                            <UserCheck size={14} /> Check-in (Chegou)
                          </button>
                          <button
                            onClick={() => confirmarMutation.mutate(c.id)}
                            disabled={confirmarMutation.isPending}
                            className="inline-flex items-center gap-1 px-3 py-1 bg-green-50 text-green-700 hover:bg-green-100 border border-green-200 rounded text-xs font-semibold transition"
                          >
                            <CheckCircle size={14} /> Confirmar
                          </button>
                        </>
                      )}
                      {c.status !== 'REALIZADA' && c.status !== 'CANCELADA' && (
                        <button
                          onClick={() => {
                            setRemarcarModalConsulta(c);
                            setNovaDataHora(c.dataHora.substring(0, 16));
                          }}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-gray-600 hover:bg-gray-100 rounded text-xs font-medium border"
                        >
                          <CalendarClock size={14} /> Remarcar
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal: Remarcar Consulta */}
      {remarcarModalConsulta && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex justify-between items-center border-b pb-3">
              <h3 className="text-lg font-bold text-gray-900">Remarcar Consulta</h3>
              <button onClick={() => setRemarcarModalConsulta(null)} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>
            <p className="text-sm text-gray-600">
              Selecione o novo horário para o paciente <strong>{remarcarModalConsulta.pacienteNome}</strong>:
            </p>
            <div>
              <input
                type="datetime-local"
                value={novaDataHora}
                onChange={(e) => setNovaDataHora(e.target.value)}
                className="w-full border rounded-lg p-2.5 text-sm"
              />
            </div>
            <div className="flex justify-end gap-2 pt-3 border-t">
              <button
                onClick={() => setRemarcarModalConsulta(null)}
                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium"
              >
                Cancelar
              </button>
              <button
                onClick={() => remarcarMutation.mutate({ id: remarcarModalConsulta.id, dataHora: novaDataHora })}
                disabled={remarcarMutation.isPending || !novaDataHora}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium disabled:opacity-50"
              >
                {remarcarMutation.isPending ? 'Salvando...' : 'Confirmar Remarcação'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Novo Agendamento */}
      {novoAgendamentoOpen && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-xl space-y-4">
            <div className="flex justify-between items-center border-b pb-3">
              <h3 className="text-lg font-bold text-gray-900">Agendar Nova Consulta</h3>
              <button onClick={() => setNovoAgendamentoOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Paciente *</label>
                <select
                  value={selectedPacienteId}
                  onChange={(e) => setSelectedPacienteId(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                >
                  <option value="">-- Selecione o Paciente --</option>
                  {pacientes?.map((p: any) => (
                    <option key={p.id} value={p.id}>
                      {p.nome || p.usuario?.nome || `CPF: ${p.cpf}`}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Médico *</label>
                <select
                  value={selectedMedicoId}
                  onChange={(e) => setSelectedMedicoId(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                >
                  <option value="">-- Selecione o Médico --</option>
                  {medicos?.map((m: any) => (
                    <option key={m.id} value={m.id}>
                      {m.usuario?.nome || m.usuario?.username} (CRM: {m.crm})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Data e Hora *</label>
                <input
                  type="datetime-local"
                  value={agendamentoDataHora}
                  onChange={(e) => setAgendamentoDataHora(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t">
              <button
                onClick={() => setNovoAgendamentoOpen(false)}
                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium"
              >
                Cancelar
              </button>
              <button
                onClick={() => criarConsultaMutation.mutate()}
                disabled={criarConsultaMutation.isPending || !selectedMedicoId || !selectedPacienteId || !agendamentoDataHora}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium disabled:opacity-50"
              >
                {criarConsultaMutation.isPending ? 'Agendando...' : 'Criar Agendamento'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
