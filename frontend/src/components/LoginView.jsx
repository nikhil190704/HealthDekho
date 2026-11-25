import React, { useState } from 'react';
import { ShieldCheck, Lock, Mail, Sparkles } from 'lucide-react';
import client from '../api/client';

export default function LoginView({ onLogin }) {
  const [email, setEmail] = useState('rahul.sharma@healthdekho.com');
  const [password, setPassword] = useState('Password@123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const { data } = await client.post('/auth/login', { email, password });
      localStorage.setItem('hd_token', data.token);
      localStorage.setItem('hd_user', JSON.stringify(data.customer));
      onLogin(data.customer);
    } catch (err) { setError(err.response?.data?.message || 'Invalid credentials or backend offline'); }
    finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-emerald-950 via-teal-900 to-slate-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-2xl overflow-hidden border border-emerald-100">
        <div className="bg-emerald-700 px-8 py-6 text-white flex items-center gap-3">
          <ShieldCheck className="w-9 h-9 text-emerald-200" />
          <div><h1 className="text-2xl font-bold">HealthDekho</h1><p className="text-emerald-100 text-xs">Customer Insurance Management Portal</p></div>
        </div>
        <form onSubmit={handleSubmit} className="p-8 space-y-5">
          {error && <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-sm">{error}</div>}
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">Customer Email</label>
            <div className="relative"><Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">Password</label>
            <div className="relative"><Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>
          </div>
          <button type="submit" disabled={loading} className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2.5 rounded-xl shadow-md transition disabled:opacity-60">
            {loading ? 'Signing in...' : 'Sign In to My Policies'}
          </button>
          <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span className="flex items-center gap-1"><Sparkles className="w-3.5 h-3.5 text-emerald-600" /> Demo Accounts Ready</span>
            <button type="button" onClick={() => { setEmail('priya.patel@healthdekho.com'); setPassword('Password@123'); }} className="text-emerald-700 font-medium hover:underline">Switch to Priya Patel</button>
          </div>
        </form>
      </div>
    </div>
  );
}