import React, { useState } from 'react';
import { Sparkles, CheckCircle2 } from 'lucide-react';
import client from '../api/client';

export default function AiRecommendView({ defaultDob, onRegisterFromAi }) {
  const [dateOfBirth, setDateOfBirth] = useState(defaultDob || '1992-05-14');
  const [insuranceGoals, setInsuranceGoals] = useState('Comprehensive family health hospitalization and long-term financial protection');
  const [preferredTermMonths, setPreferredTermMonths] = useState('');
  const [preferredPolicyType, setPreferredPolicyType] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const payload = { dateOfBirth, insuranceGoals, preferredTermMonths: preferredTermMonths ? Number(preferredTermMonths) : null, preferredPolicyType: preferredPolicyType || null };
      const { data } = await client.post('/recommendations', payload);
      setResult(data);
    } catch (err) { setError(err.response?.data?.message || 'Unable to generate recommendations'); }
    finally { setLoading(false); }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
        <div className="flex items-center gap-2 mb-4"><Sparkles className="w-5 h-5 text-emerald-600" /><h2 className="text-lg font-bold">Spring AI Personalized Policy Advisor</h2></div>
        {error && <div className="mb-4 p-3 rounded-lg bg-rose-50 text-rose-700 text-sm">{error}</div>}
        <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div><label className="block text-xs font-semibold text-slate-600 mb-1">Date of Birth</label>
            <input type="date" required value={dateOfBirth} onChange={(e) => setDateOfBirth(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm" /></div>
          <div><label className="block text-xs font-semibold text-slate-600 mb-1">Preferred Policy Type</label>
            <select value={preferredPolicyType} onChange={(e) => setPreferredPolicyType(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm">
              <option value="">Any Type</option><option value="HEALTH">Health</option><option value="FAMILY_FLOATER">Family Floater</option>
              <option value="TERM_LIFE">Term Life</option><option value="CRITICAL_ILLNESS">Critical Illness</option>
            </select></div>
          <div><label className="block text-xs font-semibold text-slate-600 mb-1">Preferred Term (Months)</label>
            <input type="number" min="6" max="360" placeholder="e.g. 24 or 60" value={preferredTermMonths} onChange={(e) => setPreferredTermMonths(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm" /></div>
          <div className="md:col-span-3"><label className="block text-xs font-semibold text-slate-600 mb-1">Your Insurance & Coverage Goals</label>
            <textarea rows="2" required value={insuranceGoals} onChange={(e) => setInsuranceGoals(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm" /></div>
          <div className="md:col-span-3 flex justify-end">
            <button type="submit" disabled={loading} className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-sm">{loading ? 'Analyzing...' : 'Generate AI Recommendations'}</button></div>
        </form>
      </div>
      {result && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-500 px-1">
            <span>Calculated Age: <strong>{result.calculatedAge} yrs</strong></span>
            <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-semibold">Engine: {result.aiSource}</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {result.recommendations.map((item) => (
              <div key={item.productId} className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800">#{item.rankOrder} • {item.suitabilityLabel} ({item.suitabilityScore}%)</span>
                    <span className="text-xs font-mono text-slate-400">{item.productCode}</span>
                  </div>
                  <h3 className="font-bold text-slate-900 text-base">{item.productName}</h3>
                  <p className="text-xs text-slate-600 mt-2 leading-relaxed">{item.reason}</p>
                  <div className="grid grid-cols-3 gap-2 mt-4 pt-3 border-t border-slate-100 text-xs">
                    <div><span className="text-slate-400 block">Sum Insured</span><strong>₹{Number(item.sumInsured).toLocaleString()}</strong></div>
                    <div><span className="text-slate-400 block">Premium</span><strong>₹{Number(item.premiumAmount).toLocaleString()} / {item.premiumFrequency}</strong></div>
                    <div><span className="text-slate-400 block">Term</span><strong>{item.termMonths} mos</strong></div>
                  </div>
                </div>
                <button onClick={() => onRegisterFromAi(item.productId)} className="mt-4 w-full bg-slate-900 hover:bg-emerald-700 text-white py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5"><CheckCircle2 className="w-4 h-4" /> Register This Recommended Plan</button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}