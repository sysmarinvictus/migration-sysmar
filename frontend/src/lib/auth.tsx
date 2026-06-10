import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { http, setAccessToken, getAccessToken } from "./apiClient";

interface AuthState {
  username: string | null;
  roles: string[];
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

interface Session {
  username: string;
  roles: string[];
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() =>
    getAccessToken() ? readSession() : null,
  );

  async function login(username: string, password: string) {
    const { data } = await http.post("/auth/login", { username, password });
    setAccessToken(data.accessToken);
    const s = { username: data.username as string, roles: (data.roles as string[]) ?? [] };
    localStorage.setItem("receituario.session", JSON.stringify(s));
    setSession(s);
  }

  function logout() {
    setAccessToken(null);
    localStorage.removeItem("receituario.session");
    setSession(null);
  }

  const value = useMemo<AuthState>(
    () => ({
      username: session?.username ?? null,
      roles: session?.roles ?? [],
      isAuthenticated: !!session,
      hasRole: (role) => session?.roles.includes(role) ?? false,
      login,
      logout,
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function readSession(): Session | null {
  try {
    return JSON.parse(localStorage.getItem("receituario.session") ?? "null");
  } catch {
    return null;
  }
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
