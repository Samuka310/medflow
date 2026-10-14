import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { Stethoscope, Calendar, Clock, DollarSign, CheckCircle2, User, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';

export function Medicos() {
  const { role } = useAuth();
  const [selectedMedico, setSelectedMedico] = useState<string | null>(null);
  const [dataHora, setDataHora] = useState('');
  const [customPacienteId, setCustomPacienteId] = useState('');

  // Busca dados cadastrais do próprio paciente autenticado
  const { data: pacienteMe, isLoading: loadingPaciente } = useQuery({
    queryKey: ['paciente-me'],
    queryFn: async () => {
      if (role !== 'PACIENTE') return null;
      const res = await api.get('/api/pacientes/me');
      return res.data;
    },
    enabled: role === 'PACIENTE',
  });

  // Busca lista de médicos
  const { data: medicos, isLoading: loadingMedicos } = useQuery({
    queryKey: ['medicos'],
    queryFn: async () => {
      const res = await api.get('/api/medicos');
      return res.data;
    }
  });

  const agendarMutation = useMutation({
    mutationFn: async ({ medicoId, pacienteId, dataHora }: { medicoId: string; pacienteId: string; dataHora: string }) => {
      const res = await api.post('/api/consultas', {
        medicoId,
        pacienteId,
        dataHora: new Date(dataHora).toISOString().slice(0, 19),
      });
      return res.data;
    },
    onSuccess: () => {
      toast.success('Consulta agendada com sucesso! Aguardando confirmação.');
      setSelectedMedico(null);
      setDataHora('');
    },
    onError: (err: any) => {
      toast.error(getErrorMessage(err));
    }
  });

  const handleConfirmar = (medicoId: string) => {
    const pacienteId = role === 'PACIENTE' ? pacienteMe?.id : customPacienteId;
    if (!pacienteId) {
      toast.error('Identificador do paciente não encontrado. Por favor, forneça o ID do paciente.');
      return;
    }
    if (!dataHora) {
      toast.error('Selecione data e horário para a consulta.');
      return;
    }
    agendarMutation.mutate({ medicoId, pacienteId, dataHora });
  };

  if (loadingMedicos || (role === 'PACIENTE' && loadingPaciente)) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-48 bg-gray-200 rounded animate-pulse" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-56 bg-white rounded-xl p-6 shadow-sm border border-gray-100 animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight flex items-center gap-2">
            <Stethoscope className="text-blue-600" /> Corpo Clínico Disponível
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Escolha o médico especialista e agende seu horário de consulta.
          </p>
        </div>
        {role === 'PACIENTE' && pacienteMe && (
          <div className="bg-blue-50 border border-blue-200 text-blue-900 px-3.5 py-2 rounded-xl text-xs flex items-center gap-2">
            <User size={15} className="text-blue-600" />
            <span>Agendando como: <strong>{pacienteMe.nome}</strong> (CPF: {pacienteMe.cpf})</span>
          </div>
        )}
      </div>

      {medicos?.length === 0 ? (
        <div className="bg-white rounded-xl p-12 text-center border border-gray-200">
          <AlertCircle className="mx-auto text-gray-400 mb-3" size={40} />
          <h3 className="text-lg font-semibold text-gray-800">Nenhum médico disponível</h3>
          <p className="text-sm text-gray-500 mt-1">Nenhum profissional está com agenda aberta no momento.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {medicos?.map((medico: any) => {
            const isSelected = selectedMedico === medico.id;
            const medicoNome = medico.usuario?.nome || medico.usuario?.username || 'Médico';

            return (
              <div 
                key={medico.id} 
                className={`bg-white rounded-2xl p-6 shadow-sm border transition-all duration-200 flex flex-col justify-between ${
                  isSelected ? 'border-blue-500 ring-2 ring-blue-100' : 'border-gray-200 hover:shadow-md'
                }`}
              >
                <div>
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-bold text-lg text-gray-900">{medicoNome}</h3>
                      <p className="text-xs font-medium text-gray-500 mt-0.5">CRM: {medico.crm}</p>
                    </div>
                    <div className="w-10 h-10 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
                      <Stethoscope size={20} />
                    </div>
                  </div>

                  <div className="mt-4 space-y-2">
                    <div className="flex flex-wrap gap-1.5">
                      {medico.especialidades?.map((esp: any) => (
                        <span 
                          key={esp.id} 
                          className="bg-indigo-50 text-indigo-700 text-xs px-2.5 py-0.5 rounded-full font-semibold border border-indigo-100"
                        >
                          {esp.nome}
                        </span>
                      ))}
                    </div>

                    {medico.especialidades?.[0]?.valorConsulta && (
                      <p className="text-xs text-gray-600 flex items-center gap-1 pt-1">
                        <DollarSign size={14} className="text-emerald-600" />
                        <span>Valor estimado: </span>
                        <strong className="text-emerald-700">
                          {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(medico.especialidades[0].valorConsulta)}
                        </strong>
                      </p>
                    )}
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-gray-100">
                  {isSelected ? (
                    <div className="space-y-3 bg-slate-50 p-3.5 rounded-xl border border-slate-200">
                      <label className="block text-xs font-bold text-gray-700 flex items-center gap-1.5">
                        <Clock size={14} /> Selecionar Data e Horário
                      </label>
                      <input 
                        type="datetime-local" 
                        className="w-full text-xs p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white"
                        value={dataHora}
                        onChange={(e) => setDataHora(e.target.value)}
                      />

                      {role !== 'PACIENTE' && (
                        <div>
                          <label className="block text-xs font-bold text-gray-700 mb-1">ID do Paciente (UUID)</label>
                          <input 
                            type="text" 
                            className="w-full text-xs p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white"
                            placeholder="UUID do paciente cadastrado"
                            value={customPacienteId}
                            onChange={(e) => setCustomPacienteId(e.target.value)}
                          />
                        </div>
                      )}

                      <div className="flex gap-2 pt-1">
                        <button 
                          onClick={() => handleConfirmar(medico.id)}
                          disabled={agendarMutation.isPending}
                          className="flex-1 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold py-2 px-3 rounded-lg shadow-sm disabled:opacity-50 transition-colors flex items-center justify-center gap-1"
                        >
                          {agendarMutation.isPending ? 'Agendando...' : (
                            <>
                              <CheckCircle2 size={14} /> Confirmar
                            </>
                          )}
                        </button>
                        <button 
                          onClick={() => { setSelectedMedico(null); setDataHora(''); }}
                          className="bg-white hover:bg-gray-100 text-gray-700 border border-gray-300 text-xs font-semibold py-2 px-3 rounded-lg transition-colors"
                        >
                          Cancelar
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button 
                      onClick={() => setSelectedMedico(medico.id)}
                      className="w-full flex items-center justify-center gap-1.5 bg-blue-50 hover:bg-blue-600 text-blue-700 hover:text-white py-2 px-4 rounded-xl text-xs font-bold transition-all border border-blue-200 hover:border-transparent"
                    >
                      <Calendar size={15} /> Agendar Consulta
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
