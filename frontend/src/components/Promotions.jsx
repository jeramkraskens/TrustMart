import { useContext } from 'react';
import { Link } from 'react-router-dom';
import { mediaUrl } from '../api/client';
import AdvertisementOffer from './AdvertisementOffer';
import PromotionContext from './PromotionContext';

export default function Promotions() {
  const { ads, openOffer, eligible } = useContext(PromotionContext);
  if (!ads.length) return null;
  return <section className="tm-container home-promotions" aria-label="Promotions">
    <h2>Featured promotions</h2>
    <div className="promotion-grid">
      {ads.map((ad, index) => <article className="promotion-card" key={ad.id}>
        {ad.image && <img src={mediaUrl(ad.image)} alt={ad.title} loading="lazy" />}
        <div><span className="tm-pill tone-teal">Advertisement</span><h3>{ad.title}</h3>
          {ad.description && <p>{ad.description}</p>}
          <AdvertisementOffer ad={ad} />
          {eligible ? <button type="button" className="tm-button small" onClick={() => openOffer(index)}>View offer →</button> : <Link className="tm-button small" to="/browse">Browse offers →</Link>}
        </div>
      </article>)}
    </div>
  </section>;
}
