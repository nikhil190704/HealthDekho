import React, { useState, useEffect } from 'react';
import { CreditCard, AlertTriangle, CheckCircle2 } from 'lucide-react';
import client from '../api/client';

export default function PaymentsView({ policies, initialPolicyNumber, onPaymentSuccess }) {
  const [selectedPolicy, setSelectedPolicy] = useState(initialPolicyNumber || policies[0]?.policyNumber || '');
  const [overview, setOverview] = useState(null);
  const [selectedInstId, setSelectedInstId] = useState('');
  const [amount, setAmount] = useState('');
  const [paymentMode, setPaymentMode] = useState('UPI');
  const [receipt, setReceipt] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { if (initialPolicyNumber) setSelectedPolicy(initialPolicyNumber); }, [initialPolicyNumber]);

  const loadOverview = async (pNum) => {
    if (!pNum) return; setError('');
    try {
      const { data } = await client.get(`/payments/policies/${pNum}`);
      setOverview(data);
      const nextUnpaid = data.installments.find((i) => i.installmentStatus !== 'PAID');
      if (nextUnpaid) { setSelectedInstId(String(nextUnpaid.installmentId)); setAmount(String(nextUnpaid.remainingPrincipal)); }
      else { setSelectedInstId(''); setAmount(''); }
    } catch (err) { setError(err.response?.data?.message || 'Could not load payment details'); }
  };

  useEffect(() => { loadOverview(selectedPolicy); }, [selectedPolicy]);
  const activeInst = overview?.installments?.find((i) => String(i.installmentId) === String(selectedInstId));

  const handlePay = async (e) => {
    e.preventDefault(); setError(''); setReceipt(null); setLoading(true);
    try {
      const { data } = await client.post('/payments/process', { policyNumber: selectedPolicy, installmentId: Number(selectedInstId), amount: Number(amount), paymentMode });
      setReceipt(data); await loadOverview(selectedPolicy); onPaymentSuccess();
    } catch (err) { setError(err.response?.data?.message || 'Payment failed'); }
    finally { setLoading(false); }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2"><CreditCard className="w-5 h-5 text-emerald-600" /><h2 className="text-lg font-bold">Premium Installments & Online Payment</h2></div>
        <select value={selectedPolicy} onChange={(e) => { setReceipt(null); setSelectedPolicy(e.target.value); }} className="border border-slate-300 rounded-xl px-3.5 py-2 text-sm font-medium">
          {policies.map((p) => <option key={p.policyNumber} value={p.policyNumber}>{p.policyNumber} — {p.policyName} ({p.policyStatus})</option>)}
        </select>
      </div>
      {overview && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200"><span className="text-xs text-slate-500">Total Payable</span><p className="text-xl font-bold">₹{Number(overview.totalPremiumPayable).toLocaleString()}</p></div>
            <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200"><span className="text-xs text-emerald-700">Total Paid</span><p className="text-xl font-bold text-emerald-800">₹{Number(overview.totalPaid).toLocaleString()}</p></div>
            <div className="p-4 rounded-xl bg-amber-50 border border-amber-200"><span className="text-xs text-amber-700">Remaining</span><p className="text-xl font-bold text-amber-800">₹{Number(overview.remainingBalance).toLocaleString()}</p></div>
          </div>
          {error && <div className="p-3 rounded-lg bg-rose-50 text-rose-700 text-sm">{error}</div>}
          {receipt && <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-300 text-emerald-900 text-sm flex items-center gap-2"><CheckCircle2 className="w-5 h-5 text-emerald-600" /><span><strong>Confirmed ({receipt.transactionReference})</strong>: Paid ₹{Number(receipt.principalPaid).toLocaleString()} + Penalty ₹{Number(receipt.penaltyCharged).toLocaleString()} = <strong>₹{Number(receipt.totalCharged).toLocaleString()}</strong> (Status: {receipt.policyStatus})</span></div>}
          {activeInst && (
            <form onSubmit={handlePay} className="p-4 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
              <div><label className="block text-xs font-semibold text-slate-600 mb-1">Installment</label>
                <select value={selectedInstId} onChange={(e) => { setSelectedInstId(e.target.value); const f = overview.installments.find((x) => String(x.installmentId) === e.target.value); if (f) setAmount(String(f.remainingPrincipal)); }} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm bg-white">
                  {overview.installments.filter((i) => i.installmentStatus !== 'PAID').map((i) => <option key={i.installmentId} value={i.installmentId}>#{i.installmentNumber} (Due {i.dueDate} • {i.installmentStatus})</option>)}
                </select></div>
              <div><label className="block text-xs font-semibold text-slate-600 mb-1">Amount (Max ₹{activeInst.remainingPrincipal})</label>
                <input type="number" step="0.01" min="1" max={activeInst.remainingPrincipal} required value={amount} onChange={(e) => setAmount(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm bg-white" /></div>
              <div><label className="block text-xs font-semibold text-slate-600 mb-1">Mode</label>
                <select value={paymentMode} onChange={(e) => setPaymentMode(e.target.value)} className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm bg-white">
                  <option value="UPI">UPI</option><option value="NET_BANKING">Net Banking</option><option value="DEBIT_CARD">Debit Card</option><option value="CREDIT_CARD">Credit Card</option>
                </select></div>
              <button type="submit" disabled={loading} className="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-2 px-4 rounded-xl text-sm">{loading ? 'Paying...' : `Pay ₹${(Number(amount || 0) + Number(activeInst.applicablePenaltyToday || 0)).toLocaleString()}`}</button>
              {Number(activeInst.applicablePenaltyToday) > 0 && <div className="md:col-span-4 text-xs text-amber-800 flex items-center gap-1"><AlertTriangle className="w-4 h-4 text-amber-600" /> Grace end ({activeInst.gracePeriodEndDate}) passed — 10% late penalty (₹{Number(activeInst.applicablePenaltyToday).toLocaleString()}) applies.</div>}
            </form>
          )}
          <div className="overflow-x-auto border border-slate-200 rounded-xl">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-100 text-slate-600 uppercase"><tr><th className="p-3">#</th><th className="p-3">Due</th><th className="p-3">Grace End</th><th className="p-3">Scheduled</th><th className="p-3">Paid</th><th className="p-3">Penalty</th><th className="p-3">Status</th></tr></thead>
              <tbody className="divide-y divide-slate-100">{overview.installments.map((inst) => (
                <tr key={inst.installmentId}><td className="p-3 font-bold">#{inst.installmentNumber}</td><td className="p-3">{inst.dueDate}</td><td className="p-3">{inst.gracePeriodEndDate}</td><td className="p-3">₹{Number(inst.scheduledAmount).toLocaleString()}</td><td className="p-3 text-emerald-700 font-semibold">₹{Number(inst.paidAmount).toLocaleString()}</td><td className="p-3 text-rose-600">₹{Number(inst.penaltyAmount || inst.applicablePenaltyToday).toLocaleString()}</td><td className="p-3"><span className={`px-2 py-0.5 rounded-full font-semibold ${inst.installmentStatus === 'PAID' ? 'bg-emerald-100 text-emerald-800' : inst.installmentStatus === 'OVERDUE' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'}`}>{inst.installmentStatus}</span></td></tr>
              ))}</tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}