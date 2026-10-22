// Search helpers for API suggestions and filtering already-loaded records.
import client from './client';

export async function marketplaceSuggestions(query, signal) {
  const { data } = await client.get('/listings/suggestions/', { params: { q: query }, signal });
  return data;
}

const normalizeSearch = (value) => String(value ?? '').normalize('NFKC').toLocaleLowerCase().replaceAll('_', ' ').trim();
const fieldValue = (item, field) => field.split('.').reduce((value, key) => value?.[key], item);
export function matchesSearch(item, fields, query) {
  const text = fields.map((field) => normalizeSearch(fieldValue(item, field))).join(' ');
  return normalizeSearch(query).split(/\s+/).filter(Boolean).every((term) => text.includes(term));
}
export function recordSuggestions(items, fields, query) {
  const seen = new Set();
  return items.filter((item) => matchesSearch(item, fields, query)).flatMap((item) => {
    const values = fields.map((field) => fieldValue(item, field)).filter((value) => value !== undefined && value !== null && value !== '');
    const label = String(values[0] ?? '');
    if (!label || seen.has(normalizeSearch(label))) return [];
    seen.add(normalizeSearch(label));
    return [{ id: item.id ?? label, label, value: label, detail: values.slice(1, 3).join(' · ') }];
  }).slice(0, 8);
}
