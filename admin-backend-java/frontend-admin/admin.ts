import request from './request'

export function adminLogin(data: { username: string; password: string }) {
  return request.post('/admin/login', data)
}

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getProductList(params: { page?: number; size?: number; keyword?: string; categoryId?: number }) {
  return request.get('/admin/product/list', { params })
}

export function saveProduct(data: any) {
  return request.post('/admin/product/save', data)
}

export function deleteProduct(id: number) {
  return request.delete(`/admin/product/${id}`)
}

export function getOrderList(params: { page?: number; size?: number; status?: string }) {
  return request.get('/admin/order/list', { params })
}

export function shipOrder(id: number) {
  return request.put(`/admin/order/${id}/ship`)
}

export function cancelOrder(id: number) {
  return request.put(`/admin/order/${id}/cancel`)
}

export function getUserList(params: { page?: number; size?: number; keyword?: string; role?: string }) {
  return request.get('/admin/user/list', { params })
}

export function updateUserStatus(id: number, data: { role: string }) {
  return request.put(`/admin/user/${id}/status`, data)
}
