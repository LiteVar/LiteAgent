import type React from "react";
import { createContext, useState, useContext, useEffect, useCallback } from "react";
import { login as apiLogin } from "../api/sdk.gen";
import { setApiToken, removeApiToken, getApiToken } from "../utils/apiUtils";
import type { SaTokenInfo } from "../api/types.gen";

type AuthState = "loading" | "authenticated" | "unauthenticated";

type User = {
  id: string;
  username: string;
  loginType?: string;
};

type AuthContextType = {
  authState: AuthState;
  user: User | null;
  login: (username: string, password: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => void;
  isLoading: boolean;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [authState, setAuthState] = useState<AuthState>("loading");
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const initializeAuth = useCallback(async () => {
    try {
      const token = getApiToken();
      
      if (!token) {
        setAuthState("unauthenticated");
        return;
      }
      
      // For now, create a basic user object from stored data
      // In the future, you could fetch user info from the API
      const tokenName = localStorage.getItem('tokenName');
      const storedUser = localStorage.getItem('user');
      
      if (storedUser) {
        setUser(JSON.parse(storedUser));
      } else if (tokenName) {
        setUser({
          id: tokenName,
          username: tokenName,
        });
      }
      
      setAuthState("authenticated");
    } catch (error) {
      console.error("Failed to initialize auth:", error);
      setAuthState("unauthenticated");
      removeApiToken();
    }
  }, []);

  useEffect(() => {
    initializeAuth();

    // Listen for localStorage changes (for multi-tab support)
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'token') {
        if (e.newValue) {
          initializeAuth();
        } else {
          setAuthState("unauthenticated");
          setUser(null);
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [initializeAuth]);

  const login = useCallback(async (username: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await apiLogin({
        query: {
          username,
          password,
        },
      });

      if (response.data?.code === 200 && response.data?.data) {
        const tokenInfo: SaTokenInfo = response.data.data;
        
        if (tokenInfo.tokenValue && tokenInfo.isLogin) {
          // Store authentication data
          localStorage.setItem('token', tokenInfo.tokenValue);
          if (tokenInfo.tokenName) {
            localStorage.setItem('tokenName', tokenInfo.tokenName);
          }
          
          // Set API token
          setApiToken(tokenInfo.tokenValue);
          
          // Create user object
          const userData: User = {
            id: tokenInfo.loginId?.toString() || username,
            username,
            loginType: tokenInfo.loginType,
          };
          
          // Store user data
          localStorage.setItem('user', JSON.stringify(userData));
          setUser(userData);
          setAuthState("authenticated");
          
          return { success: true };
        } else {
          return { success: false, error: "Invalid login response" };
        }
      } else {
        return { 
          success: false, 
          error: response.data?.msg || "Login failed" 
        };
      }
    } catch (error: unknown) {
      console.error("Login error:", error);
      
      // Handle different error types
      if (error && typeof error === 'object' && 'status' in error && error.status === 401) {
        return { success: false, error: "Invalid username or password" };
      } else if (error && typeof error === 'object' && 'message' in error && typeof error.message === 'string') {
        return { success: false, error: error.message };
      }
      
      return { success: false, error: "An unexpected error occurred during login" };
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    removeApiToken();
    localStorage.removeItem('user');
    setUser(null);
    setAuthState("unauthenticated");
  }, []);

  const value: AuthContextType = {
    authState,
    user,
    login,
    logout,
    isLoading,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};