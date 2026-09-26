import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { api } from '../api/axios';

export function Medicos() {
  const [selectedMedico, setSelectedMedico] = useState<string | null>(null);
  const [dataHora, setDataHora] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const { data: medicos, isLoading } = useQuery({
    queryKey: ['medicos'],
    queryFn: async () => {
      const res = await api.get('/api/medicos');
      return res.data;
    }
  });

  const agendarMutation = useMutation({
    mutationFn: async (dados: any) => {
      const res = await api.post('/api/consultas', dados);
      return res.data;
    },
    onSuccess: () => {
      setSuccessMessage('Consulta agendada com sucesso!');
      setSelectedMedico(null);
      setDataHora('');
    }
  });

  if (isLoading) return <div>Carregando médicos...</div>;

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Médicos Disponíveis</h2>
      {successMessage && (
        <div className="bg-green-100 text-green-700 p-4 rounded-md">
          {successMessage}
        </div>
      )}
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {medicos?.map((medico: any) => (
          <div key={medico.id} className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
            <h3 className="font-bold text-lg">{medico.usuario.username}</h3>
            <p className="text-gray-600 mb-2">CRM: {medico.crm}</p>
            <p className="text-gray-500 mb-4">
              Especialidades: {medico.especialidades?.map((e: any) => e.nome).join(', ')}
            </p>
            
            {selectedMedico === medico.id ? (
              <div className="space-y-3 mt-4 border-t pt-4">
                <input 
                  type="datetime-local" 
                  className="border rounded p-2 w-full"
                  value={dataHora}
                  onChange={(e) => setDataHora(e.target.value)}
                />
                <div className="flex gap-2">
                  <button 
                    onClick={() => agendarMutation.mutate({ medicoId: medico.id, dataHora })}
                    disabled={!dataHora || agendarMutation.isPending}
                    className="bg-blue-600 text-white px-4 py-2 rounded flex-1 disabled:opacity-50"
                  >
                    Confirmar
                  </button>
                  <button 
                    onClick={() => setSelectedMedico(null)}
                    className="bg-gray-200 text-gray-800 px-4 py-2 rounded"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            ) : (
              <button 
                onClick={() => setSelectedMedico(medico.id)}
                className="text-blue-600 border border-blue-600 px-4 py-2 rounded hover:bg-blue-50"
              >
                Agendar Consulta
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
