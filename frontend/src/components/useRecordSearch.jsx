import { useState } from 'react';
import RecordSearch from './RecordSearch';
import { matchesSearch } from '../api/search';

export default function useRecordSearch(items, fields, label) {
  const [query, setQuery] = useState('');
  const filtered = (items || []).filter((item) => matchesSearch(item, fields, query));
  const searchControl = <>
    <RecordSearch items={items} fields={fields} value={query} onChange={setQuery} label={label} />
    {query.trim() && <p className="search-result-count" role="status">{filtered.length} matching result{filtered.length === 1 ? '' : 's'} <button type="button" className="text-link" onClick={() => setQuery('')}>Clear search</button></p>}
  </>;
  return { filtered, searchControl };
}
