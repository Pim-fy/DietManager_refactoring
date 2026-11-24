export function formValues(event) {
  return Object.fromEntries(new FormData(event.currentTarget))
}

export function dateString(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function today() {
  return dateString(new Date())
}
