import { money } from '../api/presentation';

export default function AdvertisementOffer({ ad }) {
  const hasPrice = ad.effective_offer_price !== null && ad.effective_offer_price !== undefined;
  const discount = Number(ad.effective_discount_percent || 0);
  if (!hasPrice && !discount) return null;
  return <div className="advertisement-offer">
    {hasPrice && <strong aria-label="Offer price">{money(ad.effective_offer_price)}</strong>}
    {hasPrice && ad.original_price && Number(ad.original_price) > Number(ad.effective_offer_price) &&
      <del aria-label="Original price">{money(ad.original_price)}</del>}
    {discount > 0 && <span className="ad-discount">{discount}% OFF</span>}
  </div>;
}
