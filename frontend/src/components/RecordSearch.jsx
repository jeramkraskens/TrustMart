import SearchSuggest from './SearchSuggest';
import { recordSuggestions } from '../api/search';

export default function RecordSearch({ items, fields, value, onChange, label }) {
  return <div className="record-search"><SearchSuggest value={value} onChange={onChange}
    suggestions={recordSuggestions(items || [], fields, value)} label={label} placeholder={`${label}…`} /></div>;
}
