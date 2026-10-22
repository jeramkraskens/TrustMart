import { useState } from 'react';
import { ShieldCheck, CreditCard, Smartphone, Lock } from 'lucide-react';
import client from '../api/client';

export default function EscrowCheckoutModal({ listing, onClose, onSuccess }) {
  const [method, setMethod] = useState('card');
  const [card, setCard] = useState({ name: '', number: '', expiry: '', cvv: '' });
  const [phone, setPhone] = useState('');
  const [step, setStep] = useState('form'); // form -> processing -> done
  const [error, setError] = useState('');

  const amount = Number(listing.price);
  const commission = Math.round(amount * 0.05 * 100) / 100;

  const formatCardNumber = (v) => v.replace(/\D/g, '').slice(0, 16).replace(/(.{4})/g, '$1 ').trim();
  const formatExpiry = (v) => v.replace(/\D/g, '').slice(0, 4).replace(/(\d{2})(\d)/, '$1/$2');

  const canSubmit = method === 'card'
    ? card.name.trim() && card.number.replace(/\s/g, '').length === 16 && card.expiry.length === 5 && card.cvv.length >= 3
    : phone.trim().length >= 9;

  const handlePay = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;
    setError('');
    setStep('processing');
    // Simulated gateway delay — this project has no live Stripe/PayHere merchant
    // keys, so payment is simulated here while the real escrow logic (holding
    // funds, releasing on delivery confirmation) runs for real on the backend.
    await new Promise((r) => setTimeout(r, 1400));
    try {
      await client.post('/transactions/', { listing: listing.id, payment_method: method });
      setStep('done');
      setTimeout(() => onSuccess(), 1100);
    } catch (err) {
      setError(err.response?.data?.detail || 'Payment could not be completed. Please try again.');
      setStep('form');
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl overflow-hidden">
        {step === 'done' ? (
          <div className="p-10 text-center">
            <div className="w-14 h-14 rounded-full bg-sage-light text-sage flex items-center justify-center mx-auto mb-4">
              <ShieldCheck size={26} />
            </div>
            <h2 className="font-bold text-xl mb-1">Payment secured in escrow</h2>
            <p className="text-sm text-ink/50">Rs. {amount.toLocaleString()} is held safely until you confirm delivery.</p>
          </div>
        ) : (
          <>
            <div className="px-6 pt-6 pb-4 border-b border-ink/10">
              <div className="flex items-center gap-2 mb-1">
                <ShieldCheck size={18} className="text-teal" />
                <h2 className="font-bold text-lg">Escrow-protected checkout</h2>
              </div>
              <p className="text-xs text-ink/50">
                Your payment is held safely by TrustMart and only released to the seller once you confirm delivery.
              </p>
            </div>

            <div className="px-6 py-4 bg-paper-warm/60 border-b border-ink/10 space-y-1 text-sm">
              <div className="flex justify-between"><span className="text-ink/50">{listing.title}</span><span>Rs. {amount.toLocaleString()}</span></div>
              <div className="flex justify-between text-xs text-ink/40"><span>Platform fee (paid by seller on payout)</span><span>Rs. {commission.toLocaleString()}</span></div>
              <div className="flex justify-between font-semibold pt-1 border-t border-ink/10 mt-1"><span>Total due now</span><span>Rs. {amount.toLocaleString()}</span></div>
            </div>

            <form onSubmit={handlePay} className="p-6 space-y-4">
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setMethod('card')}
                  className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg border text-sm font-medium transition-colors ${
                    method === 'card' ? 'border-teal bg-teal-light/40 text-teal-dark' : 'border-ink/15 text-ink/60'
                  }`}
                >
                  <CreditCard size={15} /> Card
                </button>
                <button
                  type="button"
                  onClick={() => setMethod('payhere')}
                  className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg border text-sm font-medium transition-colors ${
                    method === 'payhere' ? 'border-teal bg-teal-light/40 text-teal-dark' : 'border-ink/15 text-ink/60'
                  }`}
                >
                  <Smartphone size={15} /> PayHere
                </button>
              </div>

              {method === 'card' ? (
                <div className="space-y-3">
                  <input
                    placeholder="Cardholder name"
                    value={card.name}
                    onChange={(e) => setCard({ ...card, name: e.target.value })}
                    className="w-full px-3 py-2.5 rounded-lg border border-ink/15 text-sm focus:outline-none focus:border-teal"
                  />
                  <input
                    placeholder="Card number"
                    inputMode="numeric"
                    value={card.number}
                    onChange={(e) => setCard({ ...card, number: formatCardNumber(e.target.value) })}
                    className="w-full px-3 py-2.5 rounded-lg border border-ink/15 text-sm focus:outline-none focus:border-teal font-mono"
                  />
                  <div className="flex gap-3">
                    <input
                      placeholder="MM/YY"
                      value={card.expiry}
                      onChange={(e) => setCard({ ...card, expiry: formatExpiry(e.target.value) })}
                      className="w-1/2 px-3 py-2.5 rounded-lg border border-ink/15 text-sm focus:outline-none focus:border-teal font-mono"
                    />
                    <input
                      placeholder="CVV"
                      inputMode="numeric"
                      value={card.cvv}
                      onChange={(e) => setCard({ ...card, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) })}
                      className="w-1/2 px-3 py-2.5 rounded-lg border border-ink/15 text-sm focus:outline-none focus:border-teal font-mono"
                    />
                  </div>
                </div>
              ) : (
                <input
                  placeholder="PayHere mobile number (e.g. 0771234567)"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value.replace(/[^\d]/g, '').slice(0, 10))}
                  className="w-full px-3 py-2.5 rounded-lg border border-ink/15 text-sm focus:outline-none focus:border-teal"
                />
              )}

              {error && <p className="text-xs text-rust bg-rust-light rounded-lg px-3 py-2">{error}</p>}

              <button
                type="submit"
                disabled={!canSubmit || step === 'processing'}
                className="w-full py-3 rounded-full bg-teal text-white font-semibold hover:bg-teal-dark transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {step === 'processing' ? (
                  'Processing payment...'
                ) : (
                  <>
                    <Lock size={14} /> Pay Rs. {amount.toLocaleString()} securely
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={onClose}
                disabled={step === 'processing'}
                className="w-full text-center text-xs text-ink/40 hover:text-ink/70"
              >
                Cancel
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
