import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Medicos } from './pages/Medicos';
import { MinhasConsultas } from './pages/MinhasConsultas';
import { Dashboard } from './pages/Dashboard';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          <Route element={<Layout />}>
            <Route path="/" element={
              <ProtectedRoute>
                <div className="bg-white p-6 rounded-lg shadow-sm">
                  <h1 className="text-2xl font-bold mb-4">Bem-vindo ao MedFlow</h1>
                  <p>Selecione uma opção no menu acima.</p>
                </div>
              </ProtectedRoute>
            } />
            <Route path="/medicos" element={
              <ProtectedRoute allowedRoles={['PACIENTE']}>
                <Medicos />
              </ProtectedRoute>
            } />
            <Route path="/minhas-consultas" element={
              <ProtectedRoute allowedRoles={['PACIENTE', 'MEDICO']}>
                <MinhasConsultas />
              </ProtectedRoute>
            } />
            <Route path="/agenda" element={
              <ProtectedRoute allowedRoles={['MEDICO']}>
                <MinhasConsultas />
              </ProtectedRoute>
            } />
            <Route path="/admin" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Dashboard />
              </ProtectedRoute>
            } />
          </Route>
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
