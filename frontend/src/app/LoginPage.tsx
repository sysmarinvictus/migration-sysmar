import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../lib/auth";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await login(username, password);
      navigate("/");
    } catch {
      setError("Usuário ou senha inválidos.");
    }
  }

  return (
    <div className="grid min-h-screen place-items-center bg-gray-100">
      <form onSubmit={onSubmit} className="w-80 rounded bg-white p-6 shadow grid gap-3">
        <h1 className="text-lg font-semibold">Entrar</h1>
        {error && <p role="alert" className="text-red-600 text-sm">{error}</p>}
        <input
          aria-label="Usuário"
          placeholder="Usuário"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="input"
        />
        <input
          aria-label="Senha"
          type="password"
          placeholder="Senha"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="input"
        />
        <button type="submit" className="rounded bg-blue-600 px-4 py-2 text-white">
          Entrar
        </button>
      </form>
    </div>
  );
}
