import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/axios';
import { toast } from 'sonner';
import { 
  FileText, 
  Pill, 
  FileCheck, 
  ArrowLeft, 
  Download, 
  Plus, 
  Save, 
  User, 
  Calendar 
} from 'lucide-react';

interface ConsultaDetalhe {
  id: string;
  pacienteId: string;
  pacienteNome?: string;
  medicoId: string;
  medicoNome?: string;
  dataHora: string;
  status: string;
}

export function ProntuarioView() {
  const { consultaId } = useParams<{ consultaId: string }>();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'prontuario' | 'receitas' | 'atestados'>('prontuario');

  // Prontuário Form State
  const [diagnosticoCid, setDiagnosticoCid] = useState('');
  const [evolucaoClinica, setEvolucaoClinica] = useState('');
  const [condutaMedica, setCondutaMedica] = useState('');
  const [observacoes, setObservacoes] = useState('');

  // Receita Form State
  const [medicamento, setMedicamento] = useState('');
  const [dosagem, setDosagem] = useState('');
  const [diasValidade, setDiasValidade] = useState('30');

  // Atestado Form State
  const [descricaoAtestado, setDescricaoAtestado] = useState('');
  const [diasAfastamento, setDiasAfastamento] = useState('3');
  const [cidAtestado, setCidAtestado] = useState('');

  // Buscar dados da consulta
  const { data: consulta, isLoading: loadingConsulta } = useQuery<ConsultaDetalhe>({
    queryKey: ['consulta-detalhe', consultaId],
    queryFn: async () => {
      const res = await api.get('/api/consultas');
      const found = res.data.find((c: any) => c.id === consultaId);
      if (!found) throw new Error('Consulta não encontrada');
      return found;
    },
    enabled: !!consultaId
  });

  const pacienteId = consulta?.pacienteId;

  // Buscar dados clínicos completos do paciente (receitas, atestados, prontuários já existentes)
  const { data: dadosClinicos, isLoading: loadingClinico } = useQuery({
    queryKey: ['paciente-clinico', pacienteId],
    queryFn: async () => {
      const res = await api.get(`/api/pacientes/${pacienteId}/clinico`);
      return res.data;
    },
    enabled: !!pacienteId
  });

  // Mutações
  const salvarProntuarioMutation = useMutation({
    mutationFn: async () => {
      await api.post('/api/prontuarios', {
        consultaId,
        diagnosticoCid,
        evolucaoClinica,
        condutaMedica,
        observacoes
      });
    },
    onSuccess: () => {
      toast.success('Prontuário e evolução clínica salvos com sucesso!');
      queryClient.invalidateQueries({ queryKey: ['paciente-clinico', pacienteId] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const emitirReceitaMutation = useMutation({
    mutationFn: async () => {
      const validade = new Date();
      validade.setDate(validade.getDate() + parseInt(diasValidade, 10));
      await api.post(`/api/pacientes/${pacienteId}/receitas`, {
        consultaId,
        medicamento,
        dosagem,
        validadeData: validade.toISOString().split('T')[0]
      });
    },
    onSuccess: () => {
      toast.success('Receita médica prescrita com sucesso!');
      setMedicamento('');
      setDosagem('');
      queryClient.invalidateQueries({ queryKey: ['paciente-clinico', pacienteId] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const emitirAtestadoMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/api/pacientes/${pacienteId}/atestados`, {
        consultaId,
        diasAfastamento: parseInt(diasAfastamento, 10),
        descricao: descricaoAtestado,
        cidOpcional: cidAtestado
      });
    },
    onSuccess: () => {
      toast.success('Atestado médico emitido com sucesso!');
      setDescricaoAtestado('');
      queryClient.invalidateQueries({ queryKey: ['paciente-clinico', pacienteId] });
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
    }
  });

  const baixarPdf = async (tipo: 'receitas' | 'atestados', id: string, filename: string) => {
    try {
      const res = await api.get(`/api/pacientes/${tipo}/${id}/pdf`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${filename}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('Download do PDF iniciado!');
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  if (loadingConsulta || loadingClinico) {
    return <div className="p-8 text-center text-gray-500 animate-pulse">Carregando prontuário e dados clínicos...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 border-b pb-4">
        <div>
          <Link to="/agenda" className="text-xs text-blue-600 hover:text-blue-800 font-medium inline-flex items-center gap-1 mb-2">
            <ArrowLeft size={14} /> Voltar para a Agenda
          </Link>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <FileText className="text-blue-600" /> Prontuário & Atendimento Clínico
          </h1>
          <p className="text-sm text-gray-500 flex items-center gap-3 mt-1">
            <span className="flex items-center gap-1"><User size={14} /> {consulta?.pacienteNome || 'Paciente'}</span>
            <span>•</span>
            <span className="flex items-center gap-1"><Calendar size={14} /> {new Date(consulta?.dataHora || '').toLocaleString()}</span>
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b space-x-4">
        <button
          onClick={() => setActiveTab('prontuario')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'prontuario' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <FileText size={16} /> Prontuário Clínico
        </button>
        <button
          onClick={() => setActiveTab('receitas')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'receitas' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Pill size={16} /> Receitas Médicas ({dadosClinicos?.receitas?.length || 0})
        </button>
        <button
          onClick={() => setActiveTab('atestados')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'atestados' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <FileCheck size={16} /> Atestados Médicos ({dadosClinicos?.atestados?.length || 0})
        </button>
      </div>

      {/* Tab: Prontuário */}
      {activeTab === 'prontuario' && (
        <div className="bg-white rounded-xl shadow-sm border p-6 space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Diagnóstico / Hipótese Diagnóstica (CID) *
              </label>
              <input
                type="text"
                placeholder="Ex: J06.9 - Infecção aguda das vias aéreas superiores"
                value={diagnosticoCid}
                onChange={(e) => setDiagnosticoCid(e.target.value)}
                className="w-full border border-gray-300 rounded-lg p-2.5 text-sm focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Conduta Médica / Recomendações
              </label>
              <input
                type="text"
                placeholder="Ex: Repouso domiciliar, hidratação abundante"
                value={condutaMedica}
                onChange={(e) => setCondutaMedica(e.target.value)}
                className="w-full border border-gray-300 rounded-lg p-2.5 text-sm focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Evolução Clínica e Exame Físico *
            </label>
            <textarea
              rows={4}
              placeholder="Descreva a anamnese, estado geral do paciente, exame dos aparelhos e observações clínicas relevantes..."
              value={evolucaoClinica}
              onChange={(e) => setEvolucaoClinica(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-2.5 text-sm focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Observações Confidenciais do Atendimento
            </label>
            <textarea
              rows={2}
              placeholder="Anotações internas..."
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-2.5 text-sm focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex justify-end">
            <button
              onClick={() => salvarProntuarioMutation.mutate()}
              disabled={salvarProntuarioMutation.isPending || !diagnosticoCid || !evolucaoClinica}
              className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 px-6 rounded-lg shadow transition disabled:opacity-50 text-sm"
            >
              <Save size={16} />
              {salvarProntuarioMutation.isPending ? 'Salvando...' : 'Salvar Registro de Prontuário'}
            </button>
          </div>
        </div>
      )}

      {/* Tab: Receitas */}
      {activeTab === 'receitas' && (
        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow-sm border p-6 space-y-4">
            <h3 className="text-base font-bold text-gray-900 flex items-center gap-2 border-b pb-2">
              <Plus size={16} className="text-blue-600" /> Prescrever Nova Receita
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Medicamento *</label>
                <input
                  type="text"
                  placeholder="Ex: Paracetamol 750mg"
                  value={medicamento}
                  onChange={(e) => setMedicamento(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Posologia e Dosagem *</label>
                <input
                  type="text"
                  placeholder="Ex: 1 comp. de 8/8h se dor ou febre"
                  value={dosagem}
                  onChange={(e) => setDosagem(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Validade (dias)</label>
                <input
                  type="number"
                  value={diasValidade}
                  onChange={(e) => setDiasValidade(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
            </div>
            <div className="flex justify-end">
              <button
                onClick={() => emitirReceitaMutation.mutate()}
                disabled={emitirReceitaMutation.isPending || !medicamento || !dosagem}
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium shadow transition disabled:opacity-50"
              >
                Emitir Receita Médica
              </button>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border p-6 space-y-3">
            <h3 className="text-base font-bold text-gray-900 border-b pb-2">Receitas Prescritas</h3>
            {dadosClinicos?.receitas?.length === 0 ? (
              <p className="text-sm text-gray-500 py-4 text-center">Nenhuma receita prescrita ainda para este paciente.</p>
            ) : (
              <div className="divide-y">
                {dadosClinicos?.receitas?.map((r: any) => (
                  <div key={r.id} className="py-3 flex justify-between items-center">
                    <div>
                      <p className="font-semibold text-sm text-gray-900">{r.medicamento}</p>
                      <p className="text-xs text-gray-500">{r.dosagem} • Validade: {r.validadeData || '30 dias'}</p>
                    </div>
                    <button
                      onClick={() => baixarPdf('receitas', r.id, `receita-${r.medicamento}`)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded text-xs font-medium transition"
                    >
                      <Download size={14} /> Baixar PDF
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab: Atestados */}
      {activeTab === 'atestados' && (
        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow-sm border p-6 space-y-4">
            <h3 className="text-base font-bold text-gray-900 flex items-center gap-2 border-b pb-2">
              <Plus size={16} className="text-blue-600" /> Emitir Novo Atestado Médico
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Dias de Afastamento *</label>
                <input
                  type="number"
                  value={diasAfastamento}
                  onChange={(e) => setDiasAfastamento(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">CID (Opcional)</label>
                <input
                  type="text"
                  placeholder="Ex: J00"
                  value={cidAtestado}
                  onChange={(e) => setCidAtestado(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Descrição / Justificativa Médica *</label>
              <textarea
                rows={2}
                placeholder="Ex: Paciente esteve sob cuidados médicos nesta data necessitando de afastamento por motivo de saúde..."
                value={descricaoAtestado}
                onChange={(e) => setDescricaoAtestado(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>
            <div className="flex justify-end">
              <button
                onClick={() => emitirAtestadoMutation.mutate()}
                disabled={emitirAtestadoMutation.isPending || !descricaoAtestado}
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium shadow transition disabled:opacity-50"
              >
                Emitir Atestado Médico
              </button>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border p-6 space-y-3">
            <h3 className="text-base font-bold text-gray-900 border-b pb-2">Atestados Emitidos</h3>
            {dadosClinicos?.atestados?.length === 0 ? (
              <p className="text-sm text-gray-500 py-4 text-center">Nenhum atestado emitido para este paciente.</p>
            ) : (
              <div className="divide-y">
                {dadosClinicos?.atestados?.map((a: any) => (
                  <div key={a.id} className="py-3 flex justify-between items-center">
                    <div>
                      <p className="font-semibold text-sm text-gray-900">{a.diasAfastamento} dia(s) de repouso</p>
                      <p className="text-xs text-gray-500">{a.descricaoAfastamento || a.descricao || 'Atestado médico de dispensa'}</p>
                    </div>
                    <button
                      onClick={() => baixarPdf('atestados', a.id, `atestado-${a.diasAfastamento}dias`)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded text-xs font-medium transition"
                    >
                      <Download size={14} /> Baixar PDF
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
