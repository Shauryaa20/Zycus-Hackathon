import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

export const getProducts = () => api.get('/products');
export const simulateSale = (id: string, quantity = 1) => api.post(`/products/${id}/orders`, { quantity });
export const getPendingPricing = () => api.get('/pricing-suggestions/pending');
export const getPendingReorder = () => api.get('/reorder-suggestions/pending');
export const updatePricing = (id: number, status: string) => api.patch(`/pricing-suggestions/${id}`, { status });
export const updateReorder = (id: number, status: string) => api.patch(`/reorder-suggestions/${id}`, { status });
export const getStrategy = () => api.get('/admin/strategy');
export const setStrategy = (strategy: string) => api.post('/admin/strategy', { strategy });
export const updateStock = (id: string, stockLevel: number) => api.patch(`/products/${id}/stock`, { stockLevel });

export default api;
