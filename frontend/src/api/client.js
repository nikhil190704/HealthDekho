import axios from 'axios';
const client = axios.create({ baseURL: 'http://localhost:8080/api/v1', headers: { 'Content-Type': 'application/json' } });
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('hd_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
client.interceptors.response.use((res) => res, (err) => {
  if (err.response?.status === 401 && !err.config?.url?.includes('/auth/login')) {
    localStorage.removeItem('hd_token'); localStorage.removeItem('hd_user'); window.location.reload();
  }
  return Promise.reject(err);
});
export default client;