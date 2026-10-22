// Shared account-form helpers so registration and profile screens use consistent field rules.
// Section: Account payload — include NIC fields only for sellers.
export function accountPayload(form) {
  const data = {};
  Object.entries(form).forEach(([key, value]) => {
    if (key.startsWith('nic_') && form.role !== 'seller') return;
    // Section: NIC values — keep leading zeros and send a blank suffix to clear a saved letter.
    data[key] = value;
  });
  return data;
}

// Section: Account errors — turn API validation messages into readable form feedback.
export function accountError(error) {
  const data = error.response?.data;
  return data ? Object.entries(data).map(([key, value]) => `${key.replaceAll('_', ' ')}: ${Array.isArray(value) ? value.join(' ') : value}`).join(' ') : 'Could not save. Please try again.';
}
