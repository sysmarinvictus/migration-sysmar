import { Navigate, Route, Routes, Link, useLocation } from "react-router-dom";
import { useAuth } from "../lib/auth";
import LoginPage from "./LoginPage";
import EspecialidadeListPage from "../features/especialidade/EspecialidadeListPage";
import EspecialidadeFormPage from "../features/especialidade/EspecialidadeFormPage";
import ConselhoClasseListPage from "../features/conselho-classe/ConselhoClasseListPage";
import ConselhoClasseFormPage from "../features/conselho-classe/ConselhoClasseFormPage";
import LocalListPage from "../features/local/LocalListPage";
import LocalFormPage from "../features/local/LocalFormPage";
import TipoMedicamentoListPage from "../features/tipo-medicamento/TipoMedicamentoListPage";
import TipoMedicamentoFormPage from "../features/tipo-medicamento/TipoMedicamentoFormPage";
import PosologiaListPage from "../features/posologia/PosologiaListPage";
import PosologiaFormPage from "../features/posologia/PosologiaFormPage";
import BairroListPage from "../features/bairro/BairroListPage";
import BairroFormPage from "../features/bairro/BairroFormPage";
import DistritoListPage from "../features/distrito/DistritoListPage";
import DistritoFormPage from "../features/distrito/DistritoFormPage";
import UnidadeListPage from "../features/unidade/UnidadeListPage";
import UnidadeFormPage from "../features/unidade/UnidadeFormPage";
import SetorListPage from "../features/setor/SetorListPage";
import SetorFormPage from "../features/setor/SetorFormPage";
import ImpedimentoListPage from "../features/impedimento/ImpedimentoListPage";
import ImpedimentoFormPage from "../features/impedimento/ImpedimentoFormPage";
import MedicamentoListPage from "../features/medicamento/MedicamentoListPage";
import MedicamentoFormPage from "../features/medicamento/MedicamentoFormPage";
import FormaApresentacaoListPage from "../features/forma-apresentacao/FormaApresentacaoListPage";
import FormaApresentacaoFormPage from "../features/forma-apresentacao/FormaApresentacaoFormPage";
import ProfissionalListPage from "../features/profissional/ProfissionalListPage";
import ProfissionalDetailPage from "../features/profissional/ProfissionalDetailPage";
import ProfissionalFormPage from "../features/profissional/ProfissionalFormPage";
import PessoaFormPage from "../features/pessoa/PessoaFormPage";
import ProfissionalExternoFormPage from "../features/profissional-externo/ProfissionalExternoFormPage";
import type { ReactNode } from "react";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/*"
        element={
          <RequireAuth>
            <Shell />
          </RequireAuth>
        }
      />
    </Routes>
  );
}

function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />;
  return <>{children}</>;
}

/** App shell with nav (entries gated by role, mirroring GeneXus menu permissions). */
function Shell() {
  const { username, hasRole, logout } = useAuth();
  return (
    <div className="min-h-screen">
      <nav className="flex items-center gap-4 border-b bg-gray-50 px-6 py-3">
        <strong>Receituário</strong>
        {hasRole("SAUDE_CADASTRO") && <Link to="/especialidades">Especialidades</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/conselhos-classe">Conselhos de Classe</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/locais">Locais</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/tipos-medicamento">Tipos de Medicamento</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/posologias">Posologias</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/bairros">Bairros</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/distritos">Distritos</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/unidades">Unidades</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/unidades">Setores</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/impedimentos">Impedimentos</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/medicamentos">Medicamentos</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/formas-apresentacao">Formas de Apresentação</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/profissionais">Profissionais</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/pessoas/novo">Pessoas</Link>}
        {hasRole("SAUDE_CADASTRO") && <Link to="/profissionais-externos/novo">Prof. Externo</Link>}
        <span className="ml-auto text-sm text-gray-600">{username}</span>
        <button onClick={logout} className="text-sm text-blue-600">Sair</button>
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/especialidades" replace />} />
          <Route path="/especialidades" element={<EspecialidadeListPage />} />
          <Route path="/especialidades/nova" element={<EspecialidadeFormPage />} />
          <Route path="/especialidades/:codigo" element={<EspecialidadeFormPage />} />
          <Route path="/conselhos-classe" element={<ConselhoClasseListPage />} />
          <Route path="/conselhos-classe/novo" element={<ConselhoClasseFormPage />} />
          <Route path="/conselhos-classe/:codigo" element={<ConselhoClasseFormPage />} />
          <Route path="/locais" element={<LocalListPage />} />
          <Route path="/locais/novo" element={<LocalFormPage />} />
          <Route path="/locais/:codigo" element={<LocalFormPage />} />
          <Route path="/tipos-medicamento" element={<TipoMedicamentoListPage />} />
          <Route path="/tipos-medicamento/novo" element={<TipoMedicamentoFormPage />} />
          <Route path="/tipos-medicamento/:codigo" element={<TipoMedicamentoFormPage />} />
          <Route path="/posologias" element={<PosologiaListPage />} />
          <Route path="/posologias/nova" element={<PosologiaFormPage />} />
          <Route path="/posologias/:codigo" element={<PosologiaFormPage />} />
          <Route path="/bairros" element={<BairroListPage />} />
          <Route path="/bairros/novo" element={<BairroFormPage />} />
          <Route path="/bairros/:codigo" element={<BairroFormPage />} />
          <Route path="/distritos" element={<DistritoListPage />} />
          <Route path="/distritos/novo" element={<DistritoFormPage />} />
          <Route path="/distritos/:codigo" element={<DistritoFormPage />} />
          <Route path="/unidades" element={<UnidadeListPage />} />
          <Route path="/unidades/nova" element={<UnidadeFormPage />} />
          <Route path="/unidades/:codigo" element={<UnidadeFormPage />} />
          <Route path="/unidades/:unidadeId/setores" element={<SetorListPage />} />
          <Route path="/unidades/:unidadeId/setores/novo" element={<SetorFormPage />} />
          <Route path="/unidades/:unidadeId/setores/:setorId" element={<SetorFormPage />} />
          <Route path="/impedimentos" element={<ImpedimentoListPage />} />
          <Route path="/impedimentos/novo" element={<ImpedimentoFormPage />} />
          <Route path="/impedimentos/:codigo" element={<ImpedimentoFormPage />} />
          <Route path="/medicamentos" element={<MedicamentoListPage />} />
          <Route path="/medicamentos/novo" element={<MedicamentoFormPage />} />
          <Route path="/medicamentos/:id" element={<MedicamentoFormPage />} />
          <Route path="/formas-apresentacao" element={<FormaApresentacaoListPage />} />
          <Route path="/formas-apresentacao/nova" element={<FormaApresentacaoFormPage />} />
          <Route path="/formas-apresentacao/:id" element={<FormaApresentacaoFormPage />} />
          <Route path="/profissionais" element={<ProfissionalListPage />} />
          <Route path="/profissionais/novo" element={<ProfissionalFormPage />} />
          <Route path="/profissionais/:id" element={<ProfissionalDetailPage />} />
          <Route path="/profissionais/:id/editar" element={<ProfissionalFormPage />} />
          <Route path="/pessoas/novo" element={<PessoaFormPage />} />
          <Route path="/pessoas/:id/editar" element={<PessoaFormPage />} />
          <Route path="/profissionais-externos/novo" element={<ProfissionalExternoFormPage />} />
          <Route path="*" element={<p className="p-6">Página não encontrada.</p>} />
        </Routes>
      </main>
    </div>
  );
}
