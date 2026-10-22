// Converts API validation errors into messages that forms can display.
export function apiError(error, fallback) {
  const data = error.response?.data;
  if (!data || typeof data !== 'object') return fallback;
  return Object.entries(data).map(([field, messages]) =>
    `${field === 'detail' || field === 'non_field_errors' ? '' : `${field.replaceAll('_', ' ')}: `}${Array.isArray(messages) ? messages.join(' ') : messages}`
  ).join(' ') || fallback;
}
