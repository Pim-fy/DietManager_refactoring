import { request } from '../../shared/api/client'

/** 토큰 없이 호출하는 인증 API. */
export const authApi = {
  register: body => request('/users/register', { method: 'POST', body }),
  login: body => request('/users/login', { method: 'POST', body }),
  logout: () => request('/users/logout', { method: 'POST' }),
}
