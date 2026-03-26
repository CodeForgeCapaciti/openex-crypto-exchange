// src/services/api.js
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  console.log('Token from localStorage:', token ? token.substring(0, 20) + '...' : 'No token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    console.log('Added Authorization header');
  }
  return config;
});

// Handle response errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error Details:', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });

    if (error.response?.status === 401) {
      console.log('Unauthorized - redirecting to login');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  register: (email, password, username) =>
    apiClient.post('/auth/register', { email, password, username }),
  login: (email, password) =>
    apiClient.post('/auth/login', { email, password }),
};

// Main API
export const mainAPI = {
  getOrders: () => apiClient.get('/orders').then(res => res.data),
  placeOrder: (order) => apiClient.post('/orders', order).then(res => res.data),
  cancelOrder: (orderId) => apiClient.delete(`/orders/${orderId}`),
  getWallets: () => apiClient.get('/wallets').then(res => res.data),
  deposit: (currency, amount) => apiClient.post('/wallets/deposit', null, { params: { currency, amount } }),
  createWallet: (currency, initialBalance) =>
    apiClient.post('/wallets/create', null, { params: { currency, initialBalance } }).then(res => res.data),
};

// Default export
const api = {
  ...authAPI,
  ...mainAPI,
};

export default api;