import { useEffect, useRef, useState } from 'react';
import { ImagePlus } from 'lucide-react';
import client, { mediaUrl } from '../../api/client';
import { getAll } from '../../api/marketplace';
import { apiError } from '../../api/errors';
import useRecordSearch from '../../components/useRecordSearch';
import AdvertisementOffer from '../../components/AdvertisementOffer';

const emptyForm = { title: '', description: '', link_url: '', is_active: true, sort_order: 0, original_price: '', offer_price: '', discount_percent: '' };

export default function AdminAdvertisements() {
  const [ads, setAds] = useState([]);
  const { filtered, searchControl } = useRecordSearch(ads, ['title', 'description', 'link_url'], 'Search advertisements');
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(emptyForm);
  const [imageFile, setImageFile] = useState(null);
  const [existingImage, setExistingImage] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [offerType, setOfferType] = useState('none');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);
  const [pendingId, setPendingId] = useState(null);
  const fileInput = useRef(null);
  const preview = useRef(null);
  const formRef = useRef(null);
  const busy = saving || pendingId !== null;
  const fieldProps = (name) => ({ name, 'aria-invalid': Boolean(fieldErrors[name]), 'aria-describedby': fieldErrors[name] ? `ad-error-${name}` : undefined });
  const fieldError = (name) => fieldErrors[name] && <span id={`ad-error-${name}`} className="ad-field-error">{fieldErrors[name]}</span>;

  useEffect(() => {
    const controller = new AbortController();
    getAll('/advertisements/', {}, controller.signal).then(setAds)
      .catch((err) => { if (!controller.signal.aborted) setError(apiError(err, 'Could not load advertisements. Please refresh.')); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!imageFile || !preview.current) return;
    const url = URL.createObjectURL(imageFile);
    preview.current.src = url;
    return () => URL.revokeObjectURL(url);
  }, [imageFile]);

  const resetForm = () => {
    setForm(emptyForm); setImageFile(null); setEditingId(null); setExistingImage('');
    setOfferType('none');
    setFieldErrors({});
    if (fileInput.current) fileInput.current.value = '';
  };
  const startEdit = (ad) => {
    setError(''); setSuccess(''); setEditingId(ad.id);
    setFieldErrors({});
    setForm({ title: ad.title, description: ad.description || '', link_url: ad.link_url || '', is_active: ad.is_active, sort_order: ad.sort_order,
      original_price: ad.original_price ?? '', offer_price: ad.offer_price ?? '', discount_percent: ad.discount_percent ?? '' });
    setOfferType(ad.offer_price != null ? 'price' : ad.discount_percent != null ? 'discount' : 'none');
    setImageFile(null); setExistingImage(ad.image || '');
    if (fileInput.current) fileInput.current.value = '';
    formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };
  const updateAds = (ad) => setAds((old) => [...old.filter((item) => item.id !== ad.id), ad]
    .sort((a, b) => a.sort_order - b.sort_order || new Date(b.created_at) - new Date(a.created_at)));
  // Section: Advertisement editor — validate banner fields and offer pricing.
  const submit = async (event) => {
    event.preventDefault(); setError(''); setSuccess('');
    const errors = {};
    if (!form.title.trim()) errors.title = 'Enter a title.';
    if (!form.description.trim()) errors.description = 'Enter a description.';
    if (!form.link_url.trim()) errors.link_url = 'Enter a link URL.';
    else {
      try { const url = new URL(form.link_url); if (!['http:', 'https:'].includes(url.protocol)) throw new Error(); }
      catch { errors.link_url = 'Enter a valid http:// or https:// link.'; }
    }
    if (!imageFile && !existingImage) errors.image = 'Add a banner image.';
    if (form.sort_order === '' || !Number.isInteger(Number(form.sort_order)) || Number(form.sort_order) < 0) errors.sort_order = 'Enter a whole number of 0 or more.';
    if (offerType !== 'none') {
      if (form.original_price === '' || !Number.isFinite(Number(form.original_price)) || Number(form.original_price) <= 0) errors.original_price = 'Enter an original price greater than 0.';
      if (offerType === 'price' && (form.offer_price === '' || !Number.isFinite(Number(form.offer_price)) || Number(form.offer_price) < 0)) errors.offer_price = 'Enter an offer price of 0 or more.';
      else if (offerType === 'price' && Number(form.offer_price) > Number(form.original_price)) errors.offer_price = 'Offer price cannot exceed the original price.';
      if (offerType === 'discount' && (form.discount_percent === '' || !Number.isFinite(Number(form.discount_percent)) || Number(form.discount_percent) <= 0 || Number(form.discount_percent) > 100)) errors.discount_percent = 'Enter a discount greater than 0 and no more than 100%.';
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length) {
      setError('Please complete the highlighted advertisement fields.');
      formRef.current?.elements.namedItem(Object.keys(errors)[0])?.focus();
      return;
    }
    setSaving(true);
    const fd = new FormData();
    Object.entries(form).forEach(([key, value]) => fd.append(key, value));
    if (imageFile) fd.append('image', imageFile);
    try {
      const { data } = editingId ? await client.patch(`/advertisements/${editingId}/`, fd) : await client.post('/advertisements/', fd);
      updateAds(data);
      setSuccess(editingId ? 'Advertisement updated.' : 'Advertisement added. Active banners appear on the homepage.');
      resetForm();
    } catch (err) {
      const data = err.response?.data;
      if (data && typeof data === 'object') setFieldErrors(Object.fromEntries(Object.entries(data).map(([key, value]) => [key, Array.isArray(value) ? value.join(' ') : String(value)])));
      setError(apiError(err, 'Could not save advertisement. Please try again.'));
    }
    finally { setSaving(false); }
  };
  // Section: Publication controls — activate, deactivate or delete advertisements.
  const toggleActive = async (ad) => {
    setPendingId(ad.id); setError(''); setSuccess('');
    try {
      const { data } = await client.patch(`/advertisements/${ad.id}/`, { is_active: !ad.is_active });
      updateAds(data);
      if (editingId === ad.id) setForm((old) => ({ ...old, is_active: data.is_active }));
      setSuccess(`Advertisement ${data.is_active ? 'activated' : 'deactivated'}.`);
    } catch (err) { setError(apiError(err, 'Could not change advertisement status.')); }
    finally { setPendingId(null); }
  };
  const deleteAd = async (ad) => {
    if (!confirm(`Delete advertisement "${ad.title}"? This cannot be undone.`)) return;
    setPendingId(ad.id); setError(''); setSuccess('');
    try {
      await client.delete(`/advertisements/${ad.id}/`);
      setAds((old) => old.filter((item) => item.id !== ad.id));
      if (editingId === ad.id) resetForm();
      setSuccess('Advertisement deleted.');
    } catch (err) { setError(apiError(err, 'Could not delete advertisement.')); }
    finally { setPendingId(null); }
  };

  return <div className="ad-admin">
    <h1>Advertisements</h1>
    <p>Active advertisements appear on the homepage and in an animated popup once per browser session. Lower sort numbers appear first.</p>
    <a className="text-link" href="/" target="_blank" rel="noreferrer">View homepage →</a>
    {error && <p className="form-error" role="alert">{error}</p>}
    {success && <p className="ad-success" role="status">{success}</p>}
    <form ref={formRef} onSubmit={submit} noValidate className="ad-form">
      <h2>{editingId ? 'Edit advertisement' : 'New advertisement'}</h2>
      <p>Complete every displayed field before saving. Choose “No offer” if this advertisement has no discount.</p>
      <fieldset disabled={busy}>
        <div className="ad-form-grid">
          <label>Title<input {...fieldProps('title')} className="tm-input" required maxLength={150} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />{fieldError('title')}</label>
          <label>Link URL<input {...fieldProps('link_url')} className="tm-input" required type="url" placeholder="https://..." value={form.link_url} onChange={(e) => setForm({ ...form, link_url: e.target.value })} />{fieldError('link_url')}</label>
        </div>
        <label>Description<input {...fieldProps('description')} className="tm-input" required maxLength={255} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />{fieldError('description')}</label>
        <div className="ad-offer-fields">
          <label>Offer type<select className="tm-input" value={offerType} onChange={(e) => {
            setOfferType(e.target.value);
            setFieldErrors((old) => ({ ...old, original_price: '', offer_price: '', discount_percent: '' }));
            setForm((old) => ({ ...old, original_price: e.target.value === 'none' ? '' : old.original_price, offer_price: '', discount_percent: '' }));
          }}>
            <option value="none">No offer</option>
            <option value="price">Offer price</option>
            <option value="discount">Percentage discount</option>
          </select></label>
          {offerType !== 'none' && <div className="ad-form-grid">
            <label>Original price (Rs.)<input {...fieldProps('original_price')} className="tm-input" required type="number" min="0.01" max="9999999999.99" step="0.01" placeholder="e.g. 10000" value={form.original_price} onChange={(e) => setForm({ ...form, original_price: e.target.value })} />{fieldError('original_price')}</label>
            {offerType === 'price' ?
              <label>Offer price (Rs.)<input {...fieldProps('offer_price')} className="tm-input" type="number" min="0" max={form.original_price || '9999999999.99'} step="0.01" required placeholder="e.g. 8000" value={form.offer_price} onChange={(e) => setForm({ ...form, offer_price: e.target.value })} />{fieldError('offer_price')}</label> :
              <label>Discount (%)<input {...fieldProps('discount_percent')} className="tm-input" type="number" min="0.01" max="100" step="0.01" required placeholder="e.g. 20" value={form.discount_percent} onChange={(e) => setForm({ ...form, discount_percent: e.target.value })} />{fieldError('discount_percent')}</label>}
          </div>}
          {offerType !== 'none' && <p>Enter an original price to show the savings. With a percentage discount, the offer price is calculated automatically.</p>}
        </div>
        <div className="ad-form-grid">
          <div>
            <p>Banner image</p>
            <input ref={fileInput} type="file" accept="image/*" hidden aria-label="Choose banner image" onChange={(e) => {
              const file = e.target.files[0];
              if (!file) return;
              if (!file.type.startsWith('image/') || !file.size) { setError('Please choose a non-empty image file.'); e.target.value = ''; return; }
              setError(''); setImageFile(file);
            }} />
            <button {...fieldProps('image')} type="button" className="ad-image-picker" onClick={() => fileInput.current?.click()}>
              {imageFile ? <img ref={preview} alt="Selected banner preview" /> : existingImage ? <img src={mediaUrl(existingImage)} alt="Current banner" /> : <ImagePlus size={28} />}
              <span>{imageFile ? imageFile.name : existingImage ? 'Replace banner image' : 'Add banner image'}</span>
            </button>
            {fieldError('image')}
          </div>
          <div>
            <label>Sort order<input {...fieldProps('sort_order')} className="tm-input" type="number" min="0" required value={form.sort_order} onChange={(e) => setForm({ ...form, sort_order: e.target.value })} />{fieldError('sort_order')}</label>
            <label className="ad-active"><input type="checkbox" checked={form.is_active} onChange={(e) => setForm({ ...form, is_active: e.target.checked })} /> Active</label>
          </div>
        </div>
        <div className="button-row">
          <button className="tm-button" type="submit">{saving ? 'Saving…' : editingId ? 'Update advertisement' : 'Add advertisement'}</button>
          {editingId && <button className="tm-button outline" type="button" onClick={resetForm}>Cancel</button>}
        </div>
      </fieldset>
    </form>
    {searchControl}
    {loading ? <p>Loading advertisements…</p> : !ads.length ? <p>No advertisements yet — add one above.</p> : !filtered.length ? <p>No matching advertisements.</p> : <div className="ad-card-grid">
      {filtered.map((ad) => <article className="ad-card" key={ad.id}>
        {ad.image && <img src={mediaUrl(ad.image)} alt={ad.title} />}
        <div className="ad-card-content">
          <span className={`tm-pill ${ad.is_active ? 'tone-green' : 'tone-orange'}`}>{ad.is_active ? 'Active' : 'Inactive'}</span>
          <h2>{ad.title}</h2><p>{ad.description}</p><small>Sort order: {ad.sort_order}</small>
          <AdvertisementOffer ad={ad} />
          <div className="button-row">
            <button className="tm-button outline small" disabled={busy} onClick={() => startEdit(ad)}>Edit</button>
            <button className="tm-button outline small" disabled={busy} onClick={() => toggleActive(ad)}>{pendingId === ad.id ? 'Saving…' : ad.is_active ? 'Deactivate' : 'Activate'}</button>
            <button className="text-link" disabled={busy} onClick={() => deleteAd(ad)}>Delete</button>
          </div>
        </div>
      </article>)}
    </div>}
  </div>;
}
