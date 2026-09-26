import { useQuery } from '@tanstack/react-query';
import { api } from '../api/axios';

export function Dashboard() {
  const { data: consultas } = useQuery({
    queryKey: ['consultas-admin'],
    queryFn: async () => {
      const res = await api.get('/api/consultas');
      return res.data;
    }
  });

  const stats = {
    total: consultas?.length || 0,
    agendadas: consultas?.filter((c: any) => c.status === 'AGENDADA').length || 0,
    realizadas: consultas?.filter((c: any) => c.status === 'REALIZADA').length || 0,
    canceladas: consultas?.filter((c: any) => c.status === 'CANCELADA').length || 0,
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Painel de Administração</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white p-6 rounded-lg shadow-sm border-l-4 border-blue-500">
          <p className="text-gray-500 text-sm font-medium">Total de Consultas</p>
          <p className="text-3xl font-bold">{stats.total}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border-l-4 border-yellow-500">
          <p className="text-gray-500 text-sm font-medium">Agendadas</p>
          <p className="text-3xl font-bold">{stats.agendadas}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border-l-4 border-green-500">
          <p className="text-gray-500 text-sm font-medium">Realizadas</p>
          <p className="text-3xl font-bold">{stats.realizadas}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border-l-4 border-red-500">
          <p className="text-gray-500 text-sm font-medium">Canceladas</p>
          <p className="text-3xl font-bold">{stats.canceladas}</p>
        </div>
      </div>
      
      <div className="bg-white p-6 rounded-lg shadow-sm mt-8">
        <h3 className="text-lg font-bold mb-4">Acesso rápido</h3>
        <p className="text-gray-600">Este é um painel fictício. Em uma versão real, aqui teríamos gráficos detalhados de faturamento e ocupação da clínica.</p>
      </div>
    </div>
  );
}
