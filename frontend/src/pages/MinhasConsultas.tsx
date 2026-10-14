import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api, getErrorMessage } from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { 
  Calendar, 
  Clock, 
  User, 
  FileText, 
  CheckCircle, 
  XCircle, 
  Play, 
  AlertCircle,
  Plus
} from 'lucide-react';
import { toast } from 'sonner';

export function MinhasConsultas() {
  const { role } = useAuth();
  const queryClient = useQueryClient();

  const { data: consultas, isLoading } = useQuery({
    queryKey: ['consultas'],
    queryFn: async () => {
      const res = await api.get('/api/consultas');
      return res.data;
    }
  });

  const acaoMutation = useMutation({
    mutationFn: async ({ id, acao }: { id: string; acao: string }) => {
      await api.put(`/api/consultas/${id}/${acao}`);
    },
    onSuccess: (_, variables) => {
      const msg = variables.acao === 'confirmar' ? 'Consulta confirmada!' :
                  variables.acao === 'cancelar' ? 'Consulta cancelada.' :
                  variables.acao === 'realizar' ? 'Consulta realizada com sucesso!' : 'Ação concluída com sucesso.';
      toast.success(msg);
      queryClient.invalidateQueries({ queryKey: ['consultas'] });
    },
    onError: (err: any) => {
      toast.error(getErrorMessage(err));
    }
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'AGENDADA':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'CONFIRMADA':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'REALIZADA':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'CANCELADA':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-56 bg-gray-200 rounded animate-pulse" />
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 space-y-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-12 bg-gray-100 rounded-lg animate-pulse" />
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
            <Calendar className="text-blue-600" />
            {role === 'MEDICO' ? 'Histórico de Atendimentos' : 'Minhas Consultas'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            {role === 'MEDICO' 
              ? 'Acompanhe as consultas realizadas, agendadas e confirmadas com seus pacientes.'
              : 'Gerencie suas consultas médicas, acompanhe confirmações e acesse prescrições.'}
          </p>
        </div>
        {role === 'PACIENTE' && (
          <Link
            to="/medicos"
            className="inline-flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-sm transition-all"
          >
            <Plus size={16} /> Nova Consulta
          </Link>
        )}
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-3.5 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                  Data & Horário
                </th>
                <th className="px-6 py-3.5 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                  {role === 'MEDICO' ? 'Paciente' : 'Médico'}
                </th>
                <th className="px-6 py-3.5 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3.5 text-right text-xs font-bold text-gray-600 uppercase tracking-wider">
                  Ações & Documentos
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {consultas?.map((c: any) => {
                const dataFormatada = new Date(c.dataHora).toLocaleString('pt-BR', {
                  day: '2-digit',
                  month: '2-digit',
                  year: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit'
                });

                return (
                  <tr key={c.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                      <div className="flex items-center gap-2">
                        <Clock size={16} className="text-gray-400" />
                        <span>{dataFormatada}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      <div className="flex items-center gap-2">
                        <User size={16} className="text-gray-400" />
                        <span className="font-semibold">
                          {role === 'MEDICO' 
                            ? (c.pacienteNome || 'Paciente') 
                            : (c.medicoNome || 'Dr(a). Médico')}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`px-2.5 py-1 inline-flex text-xs leading-5 font-bold rounded-full border ${getStatusBadge(c.status)}`}>
                        {c.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-right space-x-2">
                      {/* Confirmar */}
                      {c.status === 'AGENDADA' && (role === 'MEDICO' || role === 'RECEPCIONISTA' || role === 'ADMIN') && (
                        <button 
                          onClick={() => acaoMutation.mutate({ id: c.id, acao: 'confirmar' })}
                          disabled={acaoMutation.isPending}
                          className="inline-flex items-center gap-1 text-emerald-600 hover:text-emerald-800 bg-emerald-50 hover:bg-emerald-100 px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-colors"
                        >
                          <CheckCircle size={14} /> Confirmar
                        </button>
                      )}

                      {/* Cancelar */}
                      {(c.status === 'AGENDADA' || c.status === 'CONFIRMADA') && (
                        <button 
                          onClick={() => acaoMutation.mutate({ id: c.id, acao: 'cancelar' })}
                          disabled={acaoMutation.isPending}
                          className="inline-flex items-center gap-1 text-rose-600 hover:text-rose-800 bg-rose-50 hover:bg-rose-100 px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-colors"
                        >
                          <XCircle size={14} /> Cancelar
                        </button>
                      )}

                      {/* Realizar (Médico) */}
                      {c.status === 'CONFIRMADA' && (role === 'MEDICO' || role === 'ADMIN') && (
                        <button 
                          onClick={() => acaoMutation.mutate({ id: c.id, acao: 'realizar' })}
                          disabled={acaoMutation.isPending}
                          className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-colors"
                        >
                          <Play size={14} /> Realizar
                        </button>
                      )}

                      {/* Ver Prontuário & Documentos quando REALIZADA */}
                      {c.status === 'REALIZADA' && (
                        <Link 
                          to={`/prontuario/${c.id}`}
                          className="inline-flex items-center gap-1 text-indigo-600 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-colors"
                        >
                          <FileText size={14} /> Prontuário & PDFs
                        </Link>
                      )}
                    </td>
                  </tr>
                );
              })}
              {consultas?.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-6 py-12 text-center">
                    <AlertCircle className="mx-auto text-gray-300 mb-2" size={36} />
                    <p className="text-sm font-semibold text-gray-600">Nenhuma consulta encontrada</p>
                    <p className="text-xs text-gray-400 mt-1">
                      {role === 'PACIENTE' 
                        ? 'Você ainda não possui consultas agendadas.' 
                        : 'Não há registros cadastrados para exibição.'}
                    </p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
