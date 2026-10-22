const STATUS_STYLES = {
  draft: 'bg-ink/5 text-ink/60 border-ink/10',
  pending_verification: 'bg-amber-light text-amber border-amber/20',
  verified: 'bg-teal-light text-teal-dark border-teal/20',
  rejected: 'bg-rust-light text-rust border-rust/20',
  sold: 'bg-purple-light text-purple border-purple/20',
  pending: 'bg-amber-light text-amber border-amber/20',
  approved: 'bg-sage-light text-sage border-sage/20',
  open: 'bg-amber-light text-amber border-amber/20',
  resolved: 'bg-sage-light text-sage border-sage/20',
  held: 'bg-amber-light text-amber border-amber/20',
  released: 'bg-sage-light text-sage border-sage/20',
  refunded: 'bg-rust-light text-rust border-rust/20',
};

const STATUS_LABELS = {
  draft: 'Draft',
  pending_verification: 'Pending verification',
  verified: 'Verified',
  rejected: 'Rejected',
  sold: 'Sold',
  pending: 'Pending',
  approved: 'Approved',
  open: 'Open',
  resolved: 'Resolved',
  held: 'Escrow protected',
  released: 'Released',
  refunded: 'Refunded',
};

export function StatusBadge({ status }) {
  return (
    <span className={`inline-block text-xs font-medium px-2.5 py-0.5 rounded-full border ${STATUS_STYLES[status] || 'bg-ink/5 text-ink/60 border-ink/10'}`}>
      {STATUS_LABELS[status] || status}
    </span>
  );
}
