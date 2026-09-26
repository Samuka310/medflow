import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/axios';
import { useAuth } from '../context/AuthContext';

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
    mutationFn: async ({ id, acao }: { id: string, acao: string }) => {
      await api.put(`/api/consultas/${id}/${acao}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['consultas'] });
    }
  });

  if (isLoading) return <div>Carregando consultas...</div>;

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Minhas Consultas</h2>
      
      <div className="bg-white rounded-lg shadow-sm overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Data/Hora</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                {role === 'MEDICO' ? 'Paciente' : 'Médico'}
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ações</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {consultas?.map((c: any) => (
              <tr key={c.id}>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {new Date(c.dataHora).toLocaleString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {role === 'MEDICO' ? c.pacienteNome : c.medicoNome}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                    ${c.status === 'AGENDADA' ? 'bg-yellow-100 text-yellow-800' : 
                      c.status === 'CONFIRMADA' ? 'bg-green-100 text-green-800' : 
                      c.status === 'CANCELADA' ? 'bg-red-100 text-red-800' : 'bg-gray-100 text-gray-800'}`}>
                    {c.status}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                  {c.status === 'AGENDADA' && role === 'MEDICO' && (
                    <button 
                      onClick={() => acaoMutation.mutate({ id: c.id, acao: 'confirmar' })}
                      className="text-green-600 hover:text-green-900"
                    >
                      Confirmar
                    </button>
                  )}
                  {(c.status === 'AGENDADA' || c.status === 'CONFIRMADA') && (
                    <button 
                      onClick={() => acaoMutation.mutate({ id: c.id, acao: 'cancelar' })}
                      className="text-red-600 hover:text-red-900"
                    >
                      Cancelar
                    </button>
                  )}
                  {c.status === 'CONFIRMADA' && role === 'MEDICO' && (
                    <button 
                      onClick={() => acaoMutation.mutate({ id: c.id, acao: 'realizar' })}
                      className="text-blue-600 hover:text-blue-900"
                    >
                      Realizar
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {consultas?.length === 0 && (
              <tr>
                <td colSpan={4} className="px-6 py-4 text-center text-gray-500">
                  Nenhuma consulta encontrada.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
