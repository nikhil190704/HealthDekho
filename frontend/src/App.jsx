import React, { useState, useEffect } from 'react';
import { ShieldCheck, LayoutDashboard, Sparkles, CreditCard, Search, LogOut, PlusCircle, CheckCircle2 } from 'lucide-react';
import client from './api/client';
import LoginView from './components/LoginView';
import AiRecommendView from './components/AiRecommendView';
import PaymentsView from './components/PaymentsView';
import SearchView from './components/SearchView';

export default function App() {
  const [user, setUser] = useState(() => { try { return JSON.parse(localStorage.getItem('hd_user')); } catch { return null; } });
  const [tab, setTab] = useState('dashboard');
  const [policies, setPolicies] = useState([]);
  const [products, setProducts] = useState([]);
  const [payPolicy, setPayPolicy] = useState('');
  const [banner, setBanner] = useState('');
  const [busyId, setBusyId] = useState(null);

  const loadData = async () => {
    if (!localStorage.getItem('hd_token')) return;
    try {
      const [polRes, prodRes] = await Promise.all([client.get('/policies/my'), client.get('/products/available')]);
      setPolicies(polRes.data);
      setProducts(prodRes.data);
      if (!payPolicy && polRes.data.length > 0) setPayPolicy(polRes.data[0].policyNumber);
    } catch (err) { console.error(err); }
  };

  useEffect(() => { if (user) loadData(); }, [user]);

  const handleRegister = async (productId) => {
    setBusyId(productId); setBanner('');
    try {
      const { data } = await client.post('/policies/register', { productId });
      setBanner(`Successfully registered policy ${data.policyNumber} (${data.policyName})!`);
      await loadData();
      setTab('dashboard');
    } catch (err) { setBanner(err.response?.data?.message || 'Registration failed'); }
    finally { setBusyId(null); }
  };

  const logout = () => { localStorage.removeItem('hd_token'); localStorage.removeItem('hd_user'); setUser(null); };

  if (!user) return <LoginView onLogin={(u) => setUser(u)} />;

  const nav = [
    { id: 'dashboard', label: 'Dashboard & Catalog', icon: LayoutDashboard },
    { id: 'ai', label: 'AI Recommendations', icon: Sparkles },
    { id: 'payments', label: 'Premium Payments', icon: CreditCard },
    { id: 'search', label: 'Policy Search', icon: Search },
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-emerald-800 text-white shadow-md">
        <div className="max-w-6xl mx-auto px-4 py-4 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-2.5"><ShieldCheck className="w-7 h-7 text-emerald-200" /><div><span className="font-bold text-lg tracking-tight">HealthDekho</span><span className="ml-2 text-xs bg-emerald-700 px-2 py-0.5 rounded-full text-emerald-100">Insurance Portal</span></div></div>
          <div className="flex items-center gap-4 text-xs">
            <span>Welcome, <strong>{user.fullName}</strong> ({user.age} yrs • {user.city})</span>
            <button onClick={logout} className="flex items-center gap-1 bg-emerald-900 hover:bg-emerald-950 px-3 py-1.5 rounded-lg font-medium"><LogOut className="w-3.5 h-3.5" /> Logout</button>
          </div>
        </div>
        <div className="bg-emerald-900/60 border-t border-emerald-700/60">
          <div className="max-w-6xl mx-auto px-4 flex gap-2 overflow-x-auto py-2">
            {nav.map((n) => { const Icon = n.icon; return (
              <button key={n.id} onClick={() => setTab(n.id)} className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold transition ${tab === n.id ? 'bg-white text-emerald-900 shadow-sm' : 'text-emerald-100 hover:bg-emerald-800'}`}>
                <Icon className="w-4 h-4" /> {n.label}
              </button>
            ); })}
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 space-y-6">
        {banner && <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-300 text-emerald-900 text-sm flex items-center justify-between"><span className="flex items-center gap-2"><CheckCircle2 className="w-4 h-4 text-emerald-600" />{banner}</span><button onClick={() => setBanner('')} className="text-xs font-bold">✕</button></div>}

        {tab === 'dashboard' && (
          <>
            <section className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <h2 className="text-lg font-bold mb-4">My Registered Policies ({policies.length})</h2>
              <div className="overflow-x-auto border border-slate-200 rounded-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-100 text-slate-600 uppercase"><tr><th className="p-3">Policy #</th><th className="p-3">Plan Name</th><th className="p-3">Type</th><th className="p-3">Term</th><th className="p-3">Premium</th><th className="p-3">Sum Insured</th><th className="p-3">Status</th><th className="p-3">Action</th></tr></thead>
                  <tbody className="divide-y divide-slate-100">{policies.map((p) => (
                    <tr key={p.policyId}><td className="p-3 font-mono font-bold">{p.policyNumber}</td><td className="p-3 font-semibold">{p.policyName}</td><td className="p-3">{p.policyType}</td><td className="p-3">{p.policyTermMonths} mos</td><td className="p-3">₹{Number(p.premiumAmount).toLocaleString()} / {p.premiumFrequency}</td><td className="p-3">₹{Number(p.sumInsured).toLocaleString()}</td><td className="p-3"><span className={`px-2 py-0.5 rounded-full font-semibold ${p.policyStatus === 'PAID' ? 'bg-emerald-100 text-emerald-800' : 'bg-sky-100 text-sky-800'}`}>{p.policyStatus}</span></td><td className="p-3"><button onClick={() => { setPayPolicy(p.policyNumber); setTab('payments'); }} className="text-emerald-700 font-semibold hover:underline">Pay / Installments</button></td></tr>
                  ))}</tbody>
                </table>
              </div>
            </section>

            <section className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <h2 className="text-lg font-bold mb-1">Available Insurance Products ({products.length})</h2>
              <p className="text-xs text-slate-500 mb-4">Active products excluding plans you already hold.</p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {products.map((pr) => (
                  <div key={pr.productId} className="border border-slate-200 rounded-2xl p-5 flex flex-col justify-between hover:border-emerald-300 transition">
                    <div>
                      <div className="flex items-center justify-between"><span className="text-xs font-mono text-slate-400">{pr.productCode}</span><span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-100 font-semibold">{pr.policyType}</span></div>
                      <h3 className="font-bold text-base mt-1">{pr.productName}</h3>
                      <p className="text-xs text-slate-600 mt-1.5">{pr.description}</p>
                      <div className="grid grid-cols-3 gap-2 mt-4 pt-3 border-t border-slate-100 text-xs">
                        <div><span className="text-slate-400 block">Sum Insured</span><strong>₹{Number(pr.sumInsured).toLocaleString()}</strong></div>
                        <div><span className="text-slate-400 block">Premium</span><strong>₹{Number(pr.basePremiumAmount).toLocaleString()} / {pr.premiumFrequency}</strong></div>
                        <div><span className="text-slate-400 block">Entry Age</span><strong>{pr.minEntryAge}–{pr.maxEntryAge} yrs</strong></div>
                      </div>
                    </div>
                    <button disabled={!pr.eligibleForCustomerAge || busyId === pr.productId} onClick={() => handleRegister(pr.productId)}
                      className="mt-4 w-full bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 text-white py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5">
                      <PlusCircle className="w-4 h-4" /> {pr.eligibleForCustomerAge ? (busyId === pr.productId ? 'Registering...' : 'Register for This Policy') : `Ineligible (Age ${pr.minEntryAge}-${pr.maxEntryAge})`}
                    </button>
                  </div>
                ))}
              </div>
            </section>
          </>
        )}

        {tab === 'ai' && <AiRecommendView defaultDob={user.dateOfBirth} onRegisterFromAi={handleRegister} />}
        {tab === 'payments' && <PaymentsView policies={policies} initialPolicyNumber={payPolicy} onPaymentSuccess={loadData} />}
        {tab === 'search' && <SearchView onOpenPayments={(num) => { setPayPolicy(num); setTab('payments'); }} />}
      </main>
    </div>
  );
}