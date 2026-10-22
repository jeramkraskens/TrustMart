// Seller form for both creating a listing and editing an existing listing.
// The route ID selects edit mode; FormData sends text fields, product photos and verification evidence together.
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useEffect } from 'react';
import client from '../api/client';
import PhotoUpload from '../components/PhotoUpload';
import LocationSelect from '../components/LocationSelect';
import { useAuth } from '../context/AuthContext';

export default function CreateListing() {
  const { id } = useParams();
  return <ListingForm key={id || 'new'} id={id} />;
}

function ListingForm({ id }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState({
    title: '', description: '', price: '', condition: 'good', category: '', district: '', city: '', contact_phone: '', whatsapp_enabled: false,
  });
  const [images, setImages] = useState([]);
  const [evidenceNotes, setEvidenceNotes] = useState('');
  const [evidenceFile, setEvidenceFile] = useState(null);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(Boolean(id));
  const [loadError, setLoadError] = useState('');
  const [existing, setExisting] = useState([]);
  const [removed, setRemoved] = useState([]);
  const [oldLocation, setOldLocation] = useState('');

  // Section: Listing form setup — load categories and editable listing details.
  useEffect(() => {
    const controller = new AbortController();
    client.get('/categories/', { signal: controller.signal }).then((res) => setCategories(res.data.results ?? res.data)).catch((e) => { if (e.code !== 'ERR_CANCELED') setLoadError('Could not load categories. Please refresh.'); });
    if (id) client.get(`/listings/${id}/`, { signal: controller.signal }).then(({ data }) => {
      if (data.seller !== user.id || !data.can_manage) {
        setLoadError('Only your own unsold listings without orders can be edited.');
        return;
      }
      setForm({ title: data.title, description: data.description, price: data.price, condition: data.condition, category: data.category || '', district: data.district || '', city: data.city || '', contact_phone: data.contact_phone || '', whatsapp_enabled: data.whatsapp_enabled || false });
      setExisting(data.images || []);
      if (!data.district) setOldLocation(data.location);
    }).catch((e) => { if (e.code !== 'ERR_CANCELED') setLoadError('Could not load this listing. It may have been removed.'); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, user.id]);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  // Section: Listing submission — validate fields, then upload photos and evidence.
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    if (!form.district || !form.city) { setError('Please choose a district and town.'); return; }
    if (!/^[0-9]{10}$/.test(form.contact_phone)) { setError('Phone number must contain exactly 10 digits, without + or spaces.'); return; }
    if (!existing.length && !images.length) { setError('Add at least one product photo.'); return; }
    if (!id && (!evidenceNotes.trim() || !evidenceFile)) { setError('Add verification notes and an evidence file.'); return; }
    setError('');
    setSubmitting(true);
    try {
      // Multipart form data can carry binary files alongside ordinary text fields in one request.
      const fd = new FormData();
      Object.entries(form).forEach(([k, v]) => fd.append(k, v));
      images.forEach((img) => fd.append('uploaded_images', img));
      removed.forEach((imageId) => fd.append('removed_image_ids', imageId));
      if (!id || evidenceNotes.trim()) fd.append('evidence_notes', evidenceNotes);
      if (evidenceFile) fd.append('evidence_file', evidenceFile);
      const options = { headers: { 'Content-Type': 'multipart/form-data' } };
      if (id) await client.patch(`/listings/${id}/`, fd, options);
      else await client.post('/listings/', fd, options);
      navigate('/seller', { state: { notice: id ? 'Changes saved. Your listing is awaiting verification.' : 'Listing submitted for verification.' } });
    } catch (err) {
      const data = err.response?.data;
      setError(data ? Object.entries(data).map(([key, value]) => `${key === 'detail' ? '' : key.replaceAll('_', ' ') + ': '}${Array.isArray(value) ? value.join(' ') : value}`).join(' ') : 'Could not save listing. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <p className="empty-state">Loading listing form…</p>;
  if (loadError) return <div className="tm-container empty-state"><p className="form-error" role="alert">{loadError}</p><Link to="/seller">Back to my listings</Link></div>;
  return (
    <div className="max-w-2xl mx-auto px-6 py-10">
      <Link className="text-link" to="/seller">← Back to my listings</Link>
      <h1 className="font-bold tracking-tight text-3xl mb-1">{id ? 'Edit listing' : 'New listing'}</h1>
      <p className="text-ink/60 mb-8">
        {id ? 'Changes are reviewed again before your listing goes live. Existing photos and evidence are kept unless replaced.' : 'Your listing will be submitted to a moderator for verification before it goes live.'}
      </p>

      <form onSubmit={handleSubmit} onInvalidCapture={() => setError('Please fill in all required fields correctly.')} className="space-y-4">
        <fieldset disabled={submitting} className="space-y-4">
        <div>
          <label className="block text-sm mb-1 text-ink/70">Title</label>
          <input value={form.title} onChange={update('title')} required
            className="w-full px-4 py-2.5 rounded-lg border border-ink/15 bg-white focus:outline-none focus:border-brass" />
        </div>
        <div>
          <label className="block text-sm mb-1 text-ink/70">Description</label>
          <textarea value={form.description} onChange={update('description')} required rows={4}
            className="w-full px-4 py-2.5 rounded-lg border border-ink/15 bg-white focus:outline-none focus:border-brass" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm mb-1 text-ink/70">Price (Rs.)</label>
            <input type="number" value={form.price} onChange={update('price')} required min="0" step="0.01"
              className="w-full px-4 py-2.5 rounded-lg border border-ink/15 bg-white focus:outline-none focus:border-brass" />
          </div>
          <div>
            <label className="block text-sm mb-1 text-ink/70">Condition</label>
            <select value={form.condition} onChange={update('condition')}
              className="w-full px-4 py-2.5 rounded-lg border border-ink/15 bg-white focus:outline-none focus:border-brass">
              <option value="new">Like New</option>
              <option value="excellent">Excellent</option>
              <option value="good">Good</option>
              <option value="fair">Fair</option>
            </select>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm mb-1 text-ink/70">Category</label>
            <select value={form.category} onChange={update('category')} required
              className="w-full px-4 py-2.5 rounded-lg border border-ink/15 bg-white focus:outline-none focus:border-brass">
              <option value="">Select...</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
        </div>
        <section className="listing-form-section">
          <h2>Item location</h2>
          <p className="muted">Choose your district and nearest town.</p>
          {oldLocation && <p className="muted">Previous location: {oldLocation}. Please select it below.</p>}
          <LocationSelect district={form.district} city={form.city} required onChange={(value) => setForm((old) => ({ ...old, ...value }))} />
        </section>
        <section className="listing-form-section">
          <h2>Contact options</h2>
          <label htmlFor="listing-phone">Public phone number</label>
          <input id="listing-phone" className="tm-input" type="tel" inputMode="numeric" autoComplete="tel-national" required pattern="[0-9]{10}" title="Exactly 10 digits, without + or spaces." placeholder="0771234567" value={form.contact_phone} onChange={update('contact_phone')} aria-describedby="phone-help" />
          <small className="field-help" id="phone-help">Exactly 10 digits, without +, spaces or other characters. This number will be visible to visitors.</small>
          <label className="check-row"><input type="checkbox" checked={form.whatsapp_enabled} onChange={(e) => setForm((old) => ({ ...old, whatsapp_enabled: e.target.checked }))} />Enable WhatsApp on this number</label>
        </section>
        <PhotoUpload files={images} onChange={setImages} disabled={submitting} existing={existing} onRemoveExisting={(imageId) => { setExisting((old) => old.filter((photo) => photo.id !== imageId)); setRemoved((old) => [...old, imageId]); }} />

        <div className="pt-4 border-t border-ink/10">
          <p className="text-sm font-medium mb-2">Verification evidence</p>
          <p className="text-xs text-ink/50 mb-3">
            Help the moderator verify authenticity — receipt, warranty card, or notes on condition.
          </p>
          <label htmlFor="evidence-notes">Verification notes{id ? ' (leave blank to keep existing)' : ''}</label>
          <textarea id="evidence-notes" required={!id} value={evidenceNotes} onChange={(e) => setEvidenceNotes(e.target.value)} rows={3}
            placeholder="e.g. Original receipt attached, purchased in 2023, no visible damage."
            className="w-full px-4 py-2.5 rounded-lg border border-ink/15 bg-white focus:outline-none focus:border-brass mb-3" />
          <label htmlFor="evidence-file">Evidence file{id ? ' (choose a file to replace existing)' : ''}</label>
          <input id="evidence-file" required={!id} type="file" accept="image/*,.pdf" onChange={(e) => setEvidenceFile(e.target.files[0])} className="w-full text-sm" />
        </div>

        {error && <p className="text-rust text-sm break-words" role="alert">{error}</p>}
        <button type="submit" disabled={submitting}
          className="w-full py-3 rounded-full bg-teal text-white hover:bg-teal-dark transition-colors disabled:opacity-50">
          {submitting ? 'Saving...' : id ? 'Save & submit for verification' : 'Submit for verification'}
        </button>
        <Link className="tm-button secondary full-width" to="/seller">Cancel</Link>
        </fieldset>
      </form>
    </div>
  );
}
