import React, { useState } from 'react';
import { Search } from 'lucide-react';
import client from '../api/client';

export default function SearchView({ onOpenPayments }) {
  const [policyNumber, setPolicyNumber] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [results, setResults] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const params = {};
      if (policyNumber.trim()) params.policyNumber = policyNumber.trim();
      if (customerName.trim()) params.customerName = customerName.trim();
      if (dateOfBirth) params.dateOfBirth = dateOfBirth;
      const { data } = await client.get('/policies/search', { params });
      setResults(data.content || []);
    } catch (err) { setError(err.response?.data?.message || 'Enter at least one search filter'); }
    finally { setLoading(false); }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-5">
      <div className="flex items-center gap-2"><Search className="w-5 h-5 text-emerald-600" /><h2 className="text-lg font-bold">Search My Registered Policies</h2></div>
      {error && <div className="p-3 rounded-lg bg-rose-50 text-rose-700 text-sm">{error}</div>}
      <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
        <div><label className="block text-xs font-semibold text-slate-600 mb-1">Policy Number</label>
          <input type="text" placeholder="e.g. POL-HD-2026-1002" value={policyNumber} onChange={(e) => setPolicyNumber(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm" /></div>
        <div><label className="block text-xs font-semibold text-slate-600 mb-1">Customer Name</label>
          <input type="text" placeholder="e.g. Rahul Sharma" value={customerName} onChange={(e) => setCustomerName(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm" /></div>
        <div><label className="block text-xs font-semibold text-slate-600 mb-1">Date of Birth</label>
          <input type="date" value={dateOfBirth} onChange={(e) => setDateOfBirth(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm" /></div>
        <button type="submit" disabled={loading} className="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2 px-4 rounded-xl text-sm">{loading ? 'Searching...' : 'Search Policies'}</button>
      </form>
      {results && (
        results.length === 0 ? <p className="text-sm text-slate-500">No matching registered policies found.</p> : (
          <div className="overflow-x-auto border border-slate-200 rounded-xl">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-100 text-slate-600 uppercase"><tr><th className="p-3">Policy #</th><th className="p-3">Plan Name</th><th className="p-3">Holder</th><th className="p-3">Term</th><th className="p-3">Premium</th><th className="p-3">Status</th><th className="p-3">Action</th></tr></thead>
              <tbody className="divide-y divide-slate-100">{results.map((p) => (
                <tr key={p.policyId}><td className="p-3 font-mono font-bold">{p.policyNumber}</td><td className="p-3 font-semibold">{p.policyName}</td><td className="p-3">{p.customerName} ({p.customerDob})</td><td className="p-3">{p.policyTermMonths} mos</td><td className="p-3">₹{Number(p.premiumAmount).toLocaleString()} / {p.premiumFrequency}</td><td className="p-3"><span className={`px-2 py-0.5 rounded-full font-semibold ${p.policyStatus === 'PAID' ? 'bg-emerald-100 text-emerald-800' : 'bg-sky-100 text-sky-800'}`}>{p.policyStatus}</span></td><td className="p-3"><button onClick={() => onOpenPayments(p.policyNumber)} className="text-emerald-700 font-semibold hover:underline">Manage Payments</button></td></tr>
              ))}</tbody>
            </table>
          </div>
        )
      )}
    </div>
  );
}