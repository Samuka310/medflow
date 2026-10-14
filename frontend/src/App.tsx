import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Medicos } from './pages/Medicos';
import { MinhasConsultas } from './pages/MinhasConsultas';
import { Dashboard } from './pages/Dashboard';
import { AgendaMedico } from './pages/AgendaMedico';
import { Triagem } from './pages/Triagem';
import { Faturamento } from './pages/Faturamento';
import { Recepcao } from './pages/Recepcao';
import { ProntuarioView } from './pages/ProntuarioView';
import { 
  Calendar, 
  Activity, 
  DollarSign, 
  Users, 
  LayoutDashboard, 
  FileText, 
  ShieldCheck, 
  ArrowRight 
} from 'lucide-react';

function HomePortal() {
  const { role, username } = useAuth();

  const cardsByRole: Record<string, Array<{ title: string; desc: string; to: string; icon: any; color: string }>> = {
    ADMIN: [
      { title: 'Dashboard Administrativo', desc: 'Métricas da clínica, gestão de colaboradores e importação em lote CSV.', to: '/admin', icon: LayoutDashboard, color: 'text-purple-600 bg-purple-50 hover:bg-purple-100' },
      { title: 'Recepção & Agendamento', desc: 'Check-in presencial, confirmações e cadastro rápido de consultas.', to: '/recepcao', icon: Users, color: 'text-emerald-600 bg-emerald-50 hover:bg-emerald-100' },
      { title: 'Triagem Manchester', desc: 'Classificação de risco com aferição de sinais vitais e prioridade clínica.', to: '/triagem', icon: Activity, color: 'text-amber-600 bg-amber-50 hover:bg-amber-100' },
      { title: 'Faturamento & Guias TISS', desc: 'Liquidação de pagamentos, estornos e exportação em streaming CSV.', to: '/faturamento', icon: DollarSign, color: 'text-rose-600 bg-rose-50 hover:bg-rose-100' },
    ],
    MEDICO: [
      { title: 'Fila de Atendimento Manchester', desc: 'Atenda pacientes priorizados por gravidade clínica com chamada direta.', to: '/agenda', icon: Calendar, color: 'text-blue-600 bg-blue-50 hover:bg-blue-100' },
      { title: 'Histórico de Consultas', desc: 'Consulte registros anteriores, atestados e receitas emitidas.', to: '/minhas-consultas', icon: FileText, color: 'text-indigo-600 bg-indigo-50 hover:bg-indigo-100' },
    ],
    RECEPCIONISTA: [
      { title: 'Recepção & Agendamento', desc: 'Gestão de agendamentos, check-in, cancelamentos e reagendamentos.', to: '/recepcao', icon: Users, color: 'text-emerald-600 bg-emerald-50 hover:bg-emerald-100' },
    ],
    TRIAGEM: [
      { title: 'Triagem Manchester', desc: 'Classificação de risco por cores com registro de sinais vitais.', to: '/triagem', icon: Activity, color: 'text-amber-600 bg-amber-50 hover:bg-amber-100' },
    ],
    FATURAMENTO: [
      { title: 'Gestão Financeira & Faturamento', desc: 'Acompanhamento de receita, liquidação de consultas e exportação financeira.', to: '/faturamento', icon: DollarSign, color: 'text-rose-600 bg-rose-50 hover:bg-rose-100' },
    ],
    PACIENTE: [
      { title: 'Agendar Consulta', desc: 'Consulte médicos especialistas e reserve seu horário de atendimento.', to: '/medicos', icon: Calendar, color: 'text-teal-600 bg-teal-50 hover:bg-teal-100' },
      { title: 'Minhas Consultas & Prontuário', desc: 'Acompanhe status, confirmações e faça download de receitas e atestados em PDF.', to: '/minhas-consultas', icon: FileText, color: 'text-blue-600 bg-blue-50 hover:bg-blue-100' },
    ],
  };

  const cards = (role && cardsByRole[role]) || [];

  return (
    <div className="space-y-8 py-4">
      <div className="bg-gradient-to-r from-blue-700 via-blue-600 to-indigo-700 rounded-2xl p-8 text-white shadow-lg">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="px-3 py-1 rounded-full text-xs font-semibold bg-white/20 text-white backdrop-blur-sm border border-white/20 flex items-center gap-1.5">
                <ShieldCheck size={14} /> Perfil: {role}
              </span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight">
              Olá, {username || 'Usuário'}!
            </h1>
            <p className="mt-2 text-blue-100 max-w-2xl text-sm leading-relaxed">
              Bem-vindo ao <strong>MedFlow</strong>, o ecossistema integrado de gestão clínica e hospitalar com classificação Manchester e arquitetura reativa em microsserviços.
            </p>
          </div>
        </div>
      </div>

      <div>
        <h2 className="text-lg font-bold text-gray-800 mb-4 flex items-center gap-2">
          <span>Acesso Rápido às Funcionalidades</span>
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {cards.map((c, idx) => {
            const Icon = c.icon;
            return (
              <Link
                key={idx}
                to={c.to}
                className="group p-6 bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md hover:border-blue-300 transition-all duration-200 flex flex-col justify-between"
              >
                <div>
                  <div className={`w-12 h-12 rounded-lg flex items-center justify-center mb-4 transition-transform group-hover:scale-105 ${c.color}`}>
                    <Icon size={24} />
                  </div>
                  <h3 className="font-bold text-gray-900 group-hover:text-blue-600 text-base mb-1 transition-colors">
                    {c.title}
                  </h3>
                  <p className="text-xs text-gray-500 leading-relaxed">
                    {c.desc}
                  </p>
                </div>
                <div className="mt-4 pt-3 border-t border-gray-100 flex items-center text-xs font-semibold text-blue-600 group-hover:translate-x-1 transition-transform">
                  <span>Acessar</span>
                  <ArrowRight size={14} className="ml-1" />
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          <Route element={<Layout />}>
            {/* Home Portal */}
            <Route path="/" element={
              <ProtectedRoute>
                <HomePortal />
              </ProtectedRoute>
            } />

            {/* Rotas de Paciente & Recepção */}
            <Route path="/medicos" element={
              <ProtectedRoute allowedRoles={['PACIENTE', 'RECEPCIONISTA', 'ADMIN']}>
                <Medicos />
              </ProtectedRoute>
            } />

            {/* Histórico / Consultas */}
            <Route path="/minhas-consultas" element={
              <ProtectedRoute allowedRoles={['PACIENTE', 'MEDICO', 'RECEPCIONISTA', 'ADMIN']}>
                <MinhasConsultas />
              </ProtectedRoute>
            } />

            {/* Fila de Atendimento do Médico (Manchester) */}
            <Route path="/agenda" element={
              <ProtectedRoute allowedRoles={['MEDICO', 'ADMIN']}>
                <AgendaMedico />
              </ProtectedRoute>
            } />

            {/* Triagem Protocolo Manchester */}
            <Route path="/triagem" element={
              <ProtectedRoute allowedRoles={['TRIAGEM', 'ADMIN']}>
                <Triagem />
              </ProtectedRoute>
            } />

            {/* Gestão Financeira & Faturamento */}
            <Route path="/faturamento" element={
              <ProtectedRoute allowedRoles={['FATURAMENTO', 'ADMIN']}>
                <Faturamento />
              </ProtectedRoute>
            } />

            {/* Recepção, Check-in & Agendamento */}
            <Route path="/recepcao" element={
              <ProtectedRoute allowedRoles={['RECEPCIONISTA', 'ADMIN']}>
                <Recepcao />
              </ProtectedRoute>
            } />

            {/* Visualizador de Prontuário, Prescrições e Atestados */}
            <Route path="/prontuario/:consultaId" element={
              <ProtectedRoute allowedRoles={['MEDICO', 'PACIENTE', 'ADMIN']}>
                <ProntuarioView />
              </ProtectedRoute>
            } />

            {/* Dashboard Administrativo & Auditoria */}
            <Route path="/admin" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Dashboard />
              </ProtectedRoute>
            } />

            {/* Fallback para rota desconhecida */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
