import { useState } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  LogOut, 
  User, 
  Stethoscope, 
  Calendar, 
  Activity, 
  DollarSign, 
  Users, 
  LayoutDashboard, 
  Menu, 
  X 
} from 'lucide-react';
import { Toaster } from 'sonner';

export function Layout() {
  const { logout, role, username } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  const linkClass = (path: string) => `
    inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium transition-colors border-b-2
    ${isActive(path)
      ? 'border-blue-600 text-blue-600 font-semibold'
      : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
    }
  `;

  const mobileLinkClass = (path: string) => `
    block px-3 py-2 rounded-md text-base font-medium transition-colors
    ${isActive(path)
      ? 'bg-blue-50 text-blue-700 font-semibold'
      : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
    }
  `;

  const getRoleBadge = (r: string | null) => {
    switch (r) {
      case 'ADMIN': return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'MEDICO': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'RECEPCIONISTA': return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'TRIAGEM': return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'FATURAMENTO': return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'PACIENTE': return 'bg-teal-100 text-teal-800 border-teal-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      <Toaster richColors position="top-right" closeButton />
      
      <header className="bg-white shadow-sm border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            {/* Logo e Links Desktop */}
            <div className="flex items-center">
              <Link to="/" className="flex items-center gap-2 text-xl font-bold text-blue-600 hover:opacity-90">
                <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center shadow">
                  <Stethoscope size={20} />
                </div>
                <span>MedFlow</span>
              </Link>

              <nav className="hidden md:ml-8 md:flex md:space-x-1 lg:space-x-4">
                {role === 'ADMIN' && (
                  <>
                    <Link to="/admin" className={linkClass('/admin')}>
                      <LayoutDashboard size={16} /> Dashboard
                    </Link>
                    <Link to="/recepcao" className={linkClass('/recepcao')}>
                      <Users size={16} /> Recepção
                    </Link>
                    <Link to="/triagem" className={linkClass('/triagem')}>
                      <Activity size={16} /> Triagem
                    </Link>
                    <Link to="/faturamento" className={linkClass('/faturamento')}>
                      <DollarSign size={16} /> Faturamento
                    </Link>
                  </>
                )}

                {role === 'MEDICO' && (
                  <>
                    <Link to="/agenda" className={linkClass('/agenda')}>
                      <Calendar size={16} /> Fila de Atendimento
                    </Link>
                    <Link to="/minhas-consultas" className={linkClass('/minhas-consultas')}>
                      <Users size={16} /> Histórico de Consultas
                    </Link>
                  </>
                )}

                {role === 'RECEPCIONISTA' && (
                  <Link to="/recepcao" className={linkClass('/recepcao')}>
                    <Users size={16} /> Recepção & Agendamento
                  </Link>
                )}

                {role === 'TRIAGEM' && (
                  <Link to="/triagem" className={linkClass('/triagem')}>
                    <Activity size={16} /> Triagem Manchester
                  </Link>
                )}

                {role === 'FATURAMENTO' && (
                  <Link to="/faturamento" className={linkClass('/faturamento')}>
                    <DollarSign size={16} /> Gestão Financeira
                  </Link>
                )}

                {role === 'PACIENTE' && (
                  <>
                    <Link to="/medicos" className={linkClass('/medicos')}>
                      <Calendar size={16} /> Agendar Consulta
                    </Link>
                    <Link to="/minhas-consultas" className={linkClass('/minhas-consultas')}>
                      <Users size={16} /> Minhas Consultas
                    </Link>
                  </>
                )}
              </nav>
            </div>

            {/* Usuário e Logout Desktop */}
            <div className="hidden md:flex items-center space-x-4">
              <div className="flex items-center gap-2 text-sm text-gray-700 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-200">
                <User size={15} className="text-gray-500" />
                <span className="font-medium text-gray-800">{username || 'Usuário'}</span>
                <span className={`text-xs px-2 py-0.5 rounded-full font-semibold border ${getRoleBadge(role)}`}>
                  {role}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="text-gray-500 hover:text-red-600 hover:bg-red-50 p-2 rounded-md transition-colors flex items-center gap-1.5 text-sm font-medium"
                title="Sair do sistema"
              >
                <LogOut size={16} />
                <span>Sair</span>
              </button>
            </div>

            {/* Botão Mobile */}
            <div className="flex items-center md:hidden">
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="text-gray-500 hover:text-gray-700 p-2 rounded-md focus:outline-none"
              >
                {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
              </button>
            </div>
          </div>
        </div>

        {/* Menu Mobile Retrátil */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-gray-200 bg-white px-4 pt-3 pb-4 space-y-2">
            <div className="pb-3 border-b border-gray-100 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <User size={16} className="text-gray-500" />
                <span className="font-semibold text-gray-800 text-sm">{username}</span>
              </div>
              <span className={`text-xs px-2 py-0.5 rounded-full font-bold border ${getRoleBadge(role)}`}>
                {role}
              </span>
            </div>

            <div className="space-y-1">
              {role === 'ADMIN' && (
                <>
                  <Link to="/admin" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/admin')}>Dashboard</Link>
                  <Link to="/recepcao" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/recepcao')}>Recepção</Link>
                  <Link to="/triagem" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/triagem')}>Triagem</Link>
                  <Link to="/faturamento" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/faturamento')}>Faturamento</Link>
                </>
              )}

              {role === 'MEDICO' && (
                <>
                  <Link to="/agenda" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/agenda')}>Fila de Atendimento</Link>
                  <Link to="/minhas-consultas" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/minhas-consultas')}>Histórico de Consultas</Link>
                </>
              )}

              {role === 'RECEPCIONISTA' && (
                <Link to="/recepcao" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/recepcao')}>Recepção & Agendamento</Link>
              )}

              {role === 'TRIAGEM' && (
                <Link to="/triagem" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/triagem')}>Triagem Manchester</Link>
              )}

              {role === 'FATURAMENTO' && (
                <Link to="/faturamento" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/faturamento')}>Gestão Financeira</Link>
              )}

              {role === 'PACIENTE' && (
                <>
                  <Link to="/medicos" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/medicos')}>Agendar Consulta</Link>
                  <Link to="/minhas-consultas" onClick={() => setMobileMenuOpen(false)} className={mobileLinkClass('/minhas-consultas')}>Minhas Consultas</Link>
                </>
              )}
            </div>

            <div className="pt-2 border-t border-gray-100">
              <button
                onClick={handleLogout}
                className="w-full text-left text-red-600 hover:bg-red-50 px-3 py-2 rounded-md font-medium text-sm flex items-center gap-2"
              >
                <LogOut size={16} /> Sair da conta
              </button>
            </div>
          </div>
        )}
      </header>

      <main className="flex-1 w-full max-w-7xl mx-auto p-4 sm:p-6 lg:p-8">
        <Outlet />
      </main>
    </div>
  );
}
