import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/axios';
import { toast } from 'sonner';
import { 
  Activity, 
  ShieldCheck, 
  Upload, 
  UserPlus, 
  Database 
} from 'lucide-react';

export function Dashboard() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'metricas' | 'colaboradores' | 'importacao' | 'auditoria'>('metricas');

  // Form Colaborador
  const [nomeFunc, setNomeFunc] = useState('');
  const [cpfFunc, setCpfFunc] = useState('');
  const [cargoFunc, setCargoFunc] = useState('');
  const [emailFunc, setEmailFunc] = useState('');
  const [senhaFunc, setSenhaFunc] = useState('');
  const [roleFunc, setRoleFunc] = useState('RECEPCIONISTA');

  // Form Importação CSV
  const [tipoImportacao, setTipoImportacao] = useState<'medicos' | 'pacientes'>('medicos');
  const [arquivoCsv, setArquivoCsv] = useState<File | null>(null);
  const [resultadoImportacao, setResultadoImportacao] = useState<any>(null);

  // Queries
  const { data: consultas } = useQuery({
    queryKey: ['consultas-admin'],
    queryFn: async () => {
      const res = await api.get('/api/consultas');
      return res.data;
    }
  });

  const { data: auditoriaLogs, isLoading: loadingAuditoria } = useQuery({
    queryKey: ['auditoria-logs'],
    queryFn: async () => {
      const res = await api.get('/api/auditoria?limite=20');
      return res.data;
    },
    enabled: activeTab === 'auditoria'
  });

  const stats = {
    total: consultas?.length || 0,
    agendadas: consultas?.filter((c: any) => c.status === 'AGENDADA').length || 0,
    confirmadas: consultas?.filter((c: any) => c.status === 'CONFIRMADA').length || 0,
    realizadas: consultas?.filter((c: any) => c.status === 'REALIZADA').length || 0,
    canceladas: consultas?.filter((c: any) => c.status === 'CANCELADA').length || 0,
  };

  const cadastrarFuncionarioMutation = useMutation({
    mutationFn: async () => {
      await api.post('/admin/funcionarios', {
        nome: nomeFunc,
        cpf: cpfFunc,
        cargo: cargoFunc,
        username: emailFunc,
        password: senhaFunc,
        role: roleFunc
      });
    },
    onSuccess: () => {
      toast.success('Novo colaborador cadastrado com sucesso!');
      setNomeFunc('');
      setCpfFunc('');
      setCargoFunc('');
      setEmailFunc('');
      setSenhaFunc('');
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const importarCsvMutation = useMutation({
    mutationFn: async () => {
      if (!arquivoCsv) throw new Error('Selecione um arquivo CSV');
      const formData = new FormData();
      formData.append('arquivo', arquivoCsv);
      const res = await api.post(`/api/${tipoImportacao}/importar-csv`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      return res.data;
    },
    onSuccess: (data) => {
      setResultadoImportacao(data);
      toast.success(`Importação concluída! Sucesso: ${data.sucessos || 0}, Erros: ${data.falhas || 0}`);
      queryClient.invalidateQueries({ queryKey: [tipoImportacao] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  return (
    <div className="space-y-6">
      <div className="border-b pb-4">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <ShieldCheck className="text-blue-600" /> Painel Geral de Administração
        </h1>
        <p className="text-sm text-gray-500">
          Controle administrativo, auditoria, cadastro de equipe e importação em lote
        </p>
      </div>

      {/* Tabs */}
      <div className="flex border-b space-x-4">
        <button
          onClick={() => setActiveTab('metricas')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'metricas' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Activity size={16} /> Visão Geral & Métricas
        </button>
        <button
          onClick={() => setActiveTab('colaboradores')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'colaboradores' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <UserPlus size={16} /> Cadastrar Funcionário
        </button>
        <button
          onClick={() => setActiveTab('importacao')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'importacao' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Upload size={16} /> Importação CSV em Streaming
        </button>
        <button
          onClick={() => setActiveTab('auditoria')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'auditoria' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Database size={16} /> Logs de Auditoria
        </button>
      </div>

      {/* Tab: Métricas */}
      {activeTab === 'metricas' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
              <span className="text-sm font-medium text-gray-500">Total Consultas</span>
              <p className="text-3xl font-bold text-gray-900 mt-1">{stats.total}</p>
            </div>
            <div className="bg-white p-5 rounded-xl border border-yellow-200 bg-yellow-50/20 shadow-sm">
              <span className="text-sm font-medium text-yellow-700">Agendadas</span>
              <p className="text-3xl font-bold text-yellow-800 mt-1">{stats.agendadas}</p>
            </div>
            <div className="bg-white p-5 rounded-xl border border-blue-200 bg-blue-50/20 shadow-sm">
              <span className="text-sm font-medium text-blue-700">Confirmadas</span>
              <p className="text-3xl font-bold text-blue-800 mt-1">{stats.confirmadas}</p>
            </div>
            <div className="bg-white p-5 rounded-xl border border-green-200 bg-green-50/20 shadow-sm">
              <span className="text-sm font-medium text-green-700">Realizadas</span>
              <p className="text-3xl font-bold text-green-800 mt-1">{stats.realizadas}</p>
            </div>
            <div className="bg-white p-5 rounded-xl border border-red-200 bg-red-50/20 shadow-sm">
              <span className="text-sm font-medium text-red-700">Canceladas</span>
              <p className="text-3xl font-bold text-red-800 mt-1">{stats.canceladas}</p>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl border shadow-sm">
            <h3 className="font-bold text-gray-900 mb-2">Arquitetura Integrada do MedFlow</h3>
            <p className="text-sm text-gray-600 mb-4">
              O ecossistema está operando em microsserviços desacoplados via Spring Cloud Gateway (8080), MedFlow Core (8081) e Notification Service (8082) com tolerância a falhas via Resilience4j Circuit Breaker e observabilidade por Prometheus/Grafana.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-4 border-t text-sm">
              <div className="p-3 bg-gray-50 rounded-lg">
                <span className="text-xs text-gray-500">API Gateway</span>
                <p className="font-semibold text-gray-900">Porta 8080 (Edge Proxy)</p>
                <span className="text-xs text-emerald-600 font-medium">● Roteamento & CORS Centralizado</span>
              </div>
              <div className="p-3 bg-gray-50 rounded-lg">
                <span className="text-xs text-gray-500">MedFlow Core</span>
                <p className="font-semibold text-gray-900">Porta 8081 (Backend)</p>
                <span className="text-xs text-emerald-600 font-medium">● Lock Pessimista & Flyway</span>
              </div>
              <div className="p-3 bg-gray-50 rounded-lg">
                <span className="text-xs text-gray-500">Notification Service</span>
                <p className="font-semibold text-gray-900">Porta 8082 (RabbitMQ)</p>
                <span className="text-xs text-emerald-600 font-medium">● OpenFeign + Resilience4j Fallback</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab: Cadastrar Colaborador */}
      {activeTab === 'colaboradores' && (
        <div className="bg-white p-6 rounded-xl border shadow-sm max-w-2xl space-y-4">
          <h3 className="text-lg font-bold text-gray-900">Cadastrar Novo Colaborador</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Nome Completo *</label>
              <input
                type="text"
                placeholder="Nome do colaborador"
                value={nomeFunc}
                onChange={(e) => setNomeFunc(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">CPF *</label>
              <input
                type="text"
                placeholder="000.000.000-00"
                value={cpfFunc}
                onChange={(e) => setCpfFunc(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Cargo *</label>
              <input
                type="text"
                placeholder="Ex: Recepcionista, Enfermeiro(a)"
                value={cargoFunc}
                onChange={(e) => setCargoFunc(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Perfil de Acesso (Role) *</label>
              <select
                value={roleFunc}
                onChange={(e) => setRoleFunc(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              >
                <option value="RECEPCIONISTA">RECEPCIONISTA</option>
                <option value="TRIAGEM">TRIAGEM (Enfermagem)</option>
                <option value="FATURAMENTO">FATURAMENTO</option>
                <option value="ADMIN">ADMINISTRADOR</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">E-mail / Username *</label>
              <input
                type="email"
                placeholder="email@medflow.com"
                value={emailFunc}
                onChange={(e) => setEmailFunc(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Senha Inicial *</label>
              <input
                type="password"
                placeholder="Mínimo 6 caracteres"
                value={senhaFunc}
                onChange={(e) => setSenhaFunc(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>
          </div>
          <div className="flex justify-end pt-3">
            <button
              onClick={() => cadastrarFuncionarioMutation.mutate()}
              disabled={cadastrarFuncionarioMutation.isPending || !nomeFunc || !cpfFunc || !emailFunc || !senhaFunc}
              className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2.5 rounded-lg text-sm font-medium shadow disabled:opacity-50"
            >
              {cadastrarFuncionarioMutation.isPending ? 'Cadastrando...' : 'Cadastrar Colaborador'}
            </button>
          </div>
        </div>
      )}

      {/* Tab: Importação CSV em Streaming */}
      {activeTab === 'importacao' && (
        <div className="bg-white p-6 rounded-xl border shadow-sm max-w-2xl space-y-5">
          <div>
            <h3 className="text-lg font-bold text-gray-900">Importação em Lote via Arquivo CSV</h3>
            <p className="text-xs text-gray-500 mt-1">
              Processamento em streaming via GoF Template Method com isolamento de falhas linha a linha.
            </p>
          </div>

          <div className="flex gap-4">
            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 cursor-pointer">
              <input
                type="radio"
                name="tipoImportacao"
                checked={tipoImportacao === 'medicos'}
                onChange={() => setTipoImportacao('medicos')}
              />
              Importar Médicos (CRM, Nome, E-mail, Especialidades)
            </label>
            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 cursor-pointer">
              <input
                type="radio"
                name="tipoImportacao"
                checked={tipoImportacao === 'pacientes'}
                onChange={() => setTipoImportacao('pacientes')}
              />
              Importar Pacientes (Nome, CPF, Data Nascimento, E-mail)
            </label>
          </div>

          <div className="border-2 border-dashed border-gray-300 rounded-xl p-6 text-center">
            <Upload size={32} className="mx-auto text-gray-400 mb-2" />
            <input
              type="file"
              accept=".csv"
              onChange={(e) => setArquivoCsv(e.target.files?.[0] || null)}
              className="block mx-auto text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
            />
            {arquivoCsv && (
              <p className="text-xs text-emerald-600 font-semibold mt-2">
                Arquivo selecionado: {arquivoCsv.name} ({(arquivoCsv.size / 1024).toFixed(1)} KB)
              </p>
            )}
          </div>

          <div className="flex justify-end">
            <button
              onClick={() => importarCsvMutation.mutate()}
              disabled={importarCsvMutation.isPending || !arquivoCsv}
              className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2.5 rounded-lg text-sm font-medium shadow disabled:opacity-50"
            >
              {importarCsvMutation.isPending ? 'Processando Streaming...' : 'Iniciar Importação CSV'}
            </button>
          </div>

          {resultadoImportacao && (
            <div className="p-4 bg-gray-50 rounded-lg border space-y-3">
              <h4 className="font-bold text-sm text-gray-800">Resultado do Processamento:</h4>
              <div className="flex gap-4 text-xs font-semibold">
                <span className="text-emerald-700">✓ Sucessos: {resultadoImportacao.sucessos}</span>
                <span className="text-red-700">✗ Falhas: {resultadoImportacao.falhas}</span>
                <span className="text-gray-600">Total: {resultadoImportacao.totalLinhas}</span>
              </div>
              {resultadoImportacao.erros?.length > 0 && (
                <div className="mt-2 text-xs text-red-600 space-y-1 max-h-40 overflow-y-auto">
                  <p className="font-bold">Detalhes dos erros por linha:</p>
                  {resultadoImportacao.erros.map((err: any, idx: number) => (
                    <p key={idx}>Linha {err.linha}: {err.mensagem}</p>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Tab: Logs de Auditoria */}
      {activeTab === 'auditoria' && (
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          <div className="p-4 border-b bg-gray-50">
            <h3 className="font-bold text-gray-800 text-sm">Últimos Eventos Auditados do Sistema</h3>
          </div>
          {loadingAuditoria ? (
            <div className="p-8 text-center text-gray-500 animate-pulse">Carregando logs de auditoria...</div>
          ) : (!auditoriaLogs || auditoriaLogs.length === 0) ? (
            <div className="p-8 text-center text-gray-500">Nenhum evento registrado ainda.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 text-xs">
                <thead className="bg-gray-50 text-gray-500 uppercase">
                  <tr>
                    <th className="px-6 py-3 text-left">Data/Hora</th>
                    <th className="px-6 py-3 text-left">Usuário</th>
                    <th className="px-6 py-3 text-left">Ação Executada</th>
                    <th className="px-6 py-3 text-left">Detalhes</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {auditoriaLogs.map((log: any) => (
                    <tr key={log.id} className="hover:bg-gray-50">
                      <td className="px-6 py-3 whitespace-nowrap text-gray-500">
                        {new Date(log.dataHora).toLocaleString()}
                      </td>
                      <td className="px-6 py-3 whitespace-nowrap font-medium text-gray-900">
                        {log.usuarioEmail || log.usuarioUsername || 'Sistema'}
                      </td>
                      <td className="px-6 py-3 whitespace-nowrap text-blue-600 font-semibold">
                        {log.acao}
                      </td>
                      <td className="px-6 py-3 text-gray-600">
                        {log.detalhes || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
