import { useEffect, useId, useRef, useState } from 'react';
import { ArrowRight, ChevronLeft, ChevronRight, Sparkles, X } from 'lucide-react';
import { mediaUrl } from '../api/client';
import AdvertisementOffer from './AdvertisementOffer';

export default function AdvertisementPopup({ ads, initialIndex = 0, onDismiss, onBrowse }) {
  const dialog = useRef(null);
  const closeTimer = useRef(null);
  const titleId = useId();
  const [index, setIndex] = useState(initialIndex);
  const [closing, setClosing] = useState(false);
  const ad = ads[index];

  // Section: Dialog lifecycle — lock scrolling and restore focus on close.
  useEffect(() => {
    const element = dialog.current;
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    element.showModal();
    document.body.style.overflow = 'hidden';
    return () => {
      clearTimeout(closeTimer.current);
      element.close();
      document.body.style.overflow = previousOverflow;
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus({ preventScroll: true });
    };
  }, []);

  // Section: Dismissal — finish the closing animation before removing the popup.
  const dismiss = () => {
    if (closing) return;
    setClosing(true);
    const delay = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 180;
    closeTimer.current = setTimeout(onDismiss, delay);
  };

  return <dialog ref={dialog} className={`ad-popup ${closing ? 'is-closing' : ''}`} aria-labelledby={titleId}
    onCancel={(event) => { event.preventDefault(); dismiss(); }}
    onClick={(event) => { if (event.target === event.currentTarget) dismiss(); }}>
    <div className="ad-popup-shell">
      <header className="ad-popup-header">
        <span><Sparkles size={17} /> Featured promotion</span>
        <button type="button" className="ad-popup-close" aria-label="Close advertisement" autoFocus onClick={dismiss}><X size={21} /></button>
      </header>
      <div className="ad-popup-content" key={ad.id}>
        {ad.image && <img className="ad-popup-image" src={mediaUrl(ad.image)} alt={ad.title} />}
        <div className="ad-popup-copy">
          <span className="tm-pill tone-teal">Advertisement</span>
          <h2 id={titleId}>{ad.title}</h2>
          {ad.description && <p>{ad.description}</p>}
          <AdvertisementOffer ad={ad} />
          <div className="ad-popup-actions">
            {/^https?:\/\//i.test(ad.link_url) && <a className="tm-button" href={ad.link_url} target="_blank" rel="noopener noreferrer sponsored" onClick={dismiss}>View offer <ArrowRight size={17} /></a>}
            <button type="button" className="tm-button secondary" onClick={onBrowse || dismiss}>{onBrowse ? 'Browse listings' : 'Continue browsing'}</button>
          </div>
        </div>
      </div>
      {ads.length > 1 && <nav className="ad-popup-navigation" aria-label="Advertisement navigation">
        <button type="button" aria-label="Previous advertisement" disabled={closing} onClick={() => setIndex((old) => (old - 1 + ads.length) % ads.length)}><ChevronLeft size={20} /></button>
        <span role="status">{index + 1} of {ads.length}</span>
        <button type="button" aria-label="Next advertisement" disabled={closing} onClick={() => setIndex((old) => (old + 1) % ads.length)}><ChevronRight size={20} /></button>
      </nav>}
    </div>
  </dialog>;
}
