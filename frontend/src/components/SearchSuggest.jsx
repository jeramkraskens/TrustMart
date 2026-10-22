import { useEffect, useId, useRef, useState } from 'react';
import { Search, X } from 'lucide-react';

export default function SearchSuggest({ value, onChange, onSelect, onSearch, loadSuggestions,
  suggestions = [], label = 'Search', placeholder = 'Search…', className = '', inputClassName = '', disabled = false }) {
  const id = useId();
  const input = useRef(null);
  const list = useRef(null);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [response, setResponse] = useState(null);
  // Section: Suggestions — debounce typing and cancel outdated API requests.
  useEffect(() => {
    if (!open || !loadSuggestions) return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const items = await loadSuggestions(value, controller.signal);
        if (!controller.signal.aborted) setResponse({ value, items });
      } catch (e) {
        if (!controller.signal.aborted && e.code !== 'ERR_CANCELED') setResponse({ value, items: [], error: true });
      }
    }, 200);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [value, open, loadSuggestions]);
  const remote = response?.value === value ? response : null;
  const items = (loadSuggestions ? remote?.items || [] : suggestions).slice(0, 10);
  const loading = Boolean(loadSuggestions && !remote);
  useEffect(() => {
    if (open && active >= 0) list.current?.children[active]?.scrollIntoView({ block: 'nearest' });
  }, [active, open]);
  // Section: Selection — pass the chosen suggestion to the parent page.
  const choose = (item) => {
    setOpen(false); setActive(-1);
    onChange(item.value ?? item.label);
    onSelect?.(item);
  };
  return <div className={`search-suggest ${className}`} onBlur={(e) => {
    if (!e.currentTarget.contains(e.relatedTarget)) { setOpen(false); setActive(-1); }
  }}>
    <div className="suggest-input-row">
      <Search size={17} aria-hidden="true" />
      <input ref={input} className={inputClassName} value={value} disabled={disabled} maxLength={100}
        aria-label={label} placeholder={placeholder} autoComplete="off" role="combobox" aria-autocomplete="list"
        aria-expanded={open} aria-controls={open ? `${id}-list` : undefined}
        aria-activedescendant={open && active >= 0 && items[active] ? `${id}-${active}` : undefined}
        onFocus={() => setOpen(true)}
        onClick={() => setOpen(true)}
        onChange={(e) => { onChange(e.target.value); setOpen(true); setActive(-1); }}
        onKeyDown={(e) => {
          if (e.nativeEvent.isComposing) return;
          if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
            e.preventDefault(); setOpen(true);
            if (items.length) setActive((old) => e.key === 'ArrowDown' ? (old + 1) % items.length : old <= 0 ? items.length - 1 : old - 1);
          } else if (e.key === 'Enter') {
            e.preventDefault();
            if (open && active >= 0 && items[active]) choose(items[active]);
            else { setOpen(false); onSearch?.(value); }
          } else if (e.key === 'Escape') { e.preventDefault(); setOpen(false); setActive(-1); }
          else if (e.key === 'Tab') { setOpen(false); setActive(-1); }
        }} />
      {value && <button className="suggest-clear" type="button" aria-label={`Clear ${label.toLowerCase()}`} disabled={disabled}
        onClick={() => { onChange(''); setActive(-1); input.current?.focus(); }}><X size={16} /></button>}
    </div>
    {open && <div className="suggest-popover">
      <div className="suggest-caption">{value.trim() ? 'Suggestions' : 'Explore'}</div>
      <ul ref={list} id={`${id}-list`} role="listbox" aria-label={`${label} suggestions`}>
        {items.map((item, index) => <li key={item.id ?? item.label} id={`${id}-${index}`} role="option" tabIndex={-1}
          aria-selected={active === index} className={active === index ? 'selected' : ''}
          onMouseDown={(e) => e.preventDefault()} onMouseEnter={() => setActive(index)} onClick={() => choose(item)}>
          <div><strong>{item.label}</strong>{item.detail && <small>{item.detail}</small>}</div>
          {item.kind && <span>{item.kind}</span>}
        </li>)}
      </ul>
      {!items.length && <p role="status" className="suggest-empty">{loading ? 'Loading suggestions…' : remote?.error ? 'Suggestions unavailable. You can still type a search.' : value.trim() ? 'No matching suggestions.' : 'No suggestions available yet.'}</p>}
      {onSearch && <button type="button" className="suggest-all" onClick={() => { setOpen(false); onSearch(value); }}>{value.trim() ? `Search for “${value.trim()}”` : 'Browse all results'} →</button>}
    </div>}
  </div>;
}
