import { useEffect, useId, useState } from 'react';
import client from '../api/client';

// Section: Locations — load districts and matching towns from the API.
export default function LocationSelect({ district, city, onChange, required = false, districtOnly = false }) {
  const id = useId();
  const [locations, setLocations] = useState(null);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    client.get('/listings/locations/', { signal: controller.signal })
      .then(({ data }) => { setLocations(data); setError(false); })
      .catch((e) => { if (e.code !== 'ERR_CANCELED') setError(true); });
    return () => controller.abort();
  }, [attempt]);
  return <div className={`location-fields ${districtOnly ? 'district-only' : ''}`}>
    <div><label htmlFor={`${id}-district`}>District</label>
      <select id={`${id}-district`} className="tm-input" value={district} required={required} disabled={!locations} onChange={(e) => onChange({ district: e.target.value, city: '' })}>
        <option value="">{!locations ? 'Loading districts…' : required ? 'Select district' : 'All districts'}</option>
        {Object.keys(locations || {}).map((name) => <option key={name}>{name}</option>)}
      </select></div>
    {!districtOnly && <div><label htmlFor={`${id}-city`}>City / town</label>
      <select id={`${id}-city`} className="tm-input" value={city} required={required} disabled={!district || !locations} onChange={(e) => onChange({ district, city: e.target.value })}>
        <option value="">{required ? 'Select nearest town' : 'All towns'}</option>
        {(locations?.[district] || []).map((name) => <option key={name}>{name}</option>)}
      </select></div>}
    {error && <p className="form-error" role="alert">Could not load locations. <button type="button" className="text-link" onClick={() => setAttempt((n) => n + 1)}>Retry</button></p>}
  </div>;
}
