import { createContext, useEffect, useState } from 'react';
import { loginApi, getCurrentUserApi } from '../api/authApi';

export const AuthContext = createContext(null);

function normalizeAuth(payload){
  const root = payload?.data ?? payload;
  const user = root?.user || root?.customer || root?.enseignant || root?.student || root;
  const token = root?.token || payload?.token || payload?.data?.token;
  return { ...user, token, role: user?.role || root?.role, mustChangePassword: user?.mustChangePassword ?? root?.mustChangePassword };
}
function getSavedUser(){try{return JSON.parse(localStorage.getItem('user'))}catch{return null}}

export function AuthProvider({children}){
  const [user,setUser]=useState(getSavedUser); const [loading,setLoading]=useState(false);
  const login=async(email,password)=>{const response=await loginApi({email,password});const body=response.data;if(body.success===false)throw new Error(body.message||'Erreur de connexion');const auth=normalizeAuth(body);if(auth.token)localStorage.setItem('token',auth.token);localStorage.setItem('user',JSON.stringify(auth));setUser(auth);return auth};
  const logout=()=>{localStorage.removeItem('token');localStorage.removeItem('user');setUser(null)};
  const refreshUser=async()=>{try{const response=await getCurrentUserApi();const auth=normalizeAuth(response.data);localStorage.setItem('user',JSON.stringify({...user,...auth}));setUser(u=>({...u,...auth}))}catch{logout()}};
  useEffect(()=>{if(localStorage.getItem('token')&&!user)refreshUser()},[]);
  return <AuthContext.Provider value={{user,loading,setLoading,login,logout,refreshUser,isAuthenticated:Boolean(user||localStorage.getItem('token'))}}>{children}</AuthContext.Provider>
}
