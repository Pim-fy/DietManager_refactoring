export function formValues(event) { return Object.fromEntries(new FormData(event.currentTarget)) }
export function dateString(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
export function today() { return dateString(new Date()) }
