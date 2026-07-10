/*
 * Mock 适配器
 * 在 VITE_USE_MOCK=true 时，拦截 axios 请求并返回本地 JSON 数据
 * 在 VITE_USE_MOCK=false 时，请求真实后端 API
 */

import type { AxiosInstance } from 'axios'

interface MockHandler {
  method: string
  path: string
  handler: (params?: any) => any
}

class MockAdapter {
  private handlers: MockHandler[] = []
  private dataCache: Record<string, any> = {}
  private loaded = false

  async loadData() {
    if (this.loaded) return
    try {
      const files = [
        'users', 'categories', 'products', 'product-skus',
        'cart-items', 'orders', 'addresses', 'coupons', 'reviews'
      ]
      for (const file of files) {
        const resp = await fetch(`/data/${file}.json`)
        this.dataCache[file] = await resp.json()
        console.log('[Mock] Loaded', file, 'count:', this.dataCache[file]?.length ?? 'N/A')
      }
      this.loaded = true
      console.log('[Mock] All data loaded, products:', this.dataCache['products']?.length)
    } catch (e) {
      console.warn('[Mock] Data load failed, using fallback', e)
    }
  }

  private logHandler(method: string, url: string, handler: any) {
    console.log('[Mock] Handler found:', handler?.path || 'unknown', 'for', method, url)
  }

  on(method: string, path: string, handler: (params?: any) => any) {
    this.handlers.push({ method: method.toLowerCase(), path, handler })
  }

  async handle(method: string, url: string, params?: any): Promise<any> {
    await this.loadData()
    console.log('[Mock] handle() looking for', method, url, 'cache loaded:', this.loaded)
    const handler = this.handlers.find(h => {
      const match = h.method === method.toLowerCase() && url.startsWith(h.path)
      if (!match) console.log('[Mock]  skip:', h.method, h.path)
      return match
    })
    if (handler) {
      console.log('[Mock]  matched:', handler.method, handler.path)
      return handler.handler(params)
    }
    console.warn('[Mock]  no handler for', method, url)
    return null
  }
}

export const mockAdapter = new MockAdapter()

async function delay(ms: number = 200) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/* 注册所有 Mock 处理器 */
export function setupMock(axiosInstance: AxiosInstance) {
  const { dataCache } = mockAdapter

  /* ===== 用户模块 ===== */
  mockAdapter.on('post', '/api/user/login', (params) => {
    const users = dataCache['users'] || []
    const user = users.find((u: any) =>
      u.username === params.username && u.password === params.password
    )
    if (!user) return { code: 401, msg: '用户名或密码错误', data: null }
    return { code: 200, msg: 'success', data: { token: `mock-token-${user.id}`, user } }
  })

  mockAdapter.on('get', '/api/user/profile', (params) => {
    const token = params?.headers?.Authorization || ''
    const userId = parseInt(token.replace('mock-token-', '')) || 1001
    const users = dataCache['users'] || []
    return { code: 200, msg: 'success', data: users.find((u: any) => u.id === userId) }
  })

  /* ===== 分类模块 ===== */
  mockAdapter.on('get', '/api/category/tree', () => {
    const categories = dataCache['categories'] || []
    function buildTree(parentId: number): any[] {
      return categories
        .filter((c: any) => c.parentId === parentId)
        .map((c: any) => ({ ...c, children: buildTree(c.id) }))
    }
    return { code: 200, msg: 'success', data: buildTree(0) }
  })

  /* 在 URL 过长时提取末尾数字ID */
  function extractIdFromUrl(url: string): number {
    const match = url.match(/\/(\d+)$/)
    return match ? parseInt(match[1]) : 0
  }

  /* ===== 商品模块 ===== */
  function enrichProducts(products: any[]): any[] {
    const skus = dataCache['product-skus'] || []
    return products.map(p => {
      const productSkus = skus.filter((s: any) => s.productId === p.id)
      const minSku = productSkus.reduce((min: any, s: any) =>
        !min || s.price < min.price ? s : min, null)
      return {
        ...p,
        price: minSku?.price || 0,
        originalPrice: minSku?.originalPrice || 0,
        skus: productSkus
      }
    })
  }

  mockAdapter.on('get', '/api/product/hot', () => {
    const products = dataCache['products'] || []
    return { code: 200, msg: 'success', data: enrichProducts(products.filter((p: any) => p.isHot)) }
  })

  mockAdapter.on('get', '/api/product/new', () => {
    const products = dataCache['products'] || []
    return { code: 200, msg: 'success', data: enrichProducts(products.filter((p: any) => p.isNew)) }
  })

  mockAdapter.on('get', '/api/product/seckill', () => {
    const products = dataCache['products'] || []
    return { code: 200, msg: 'success', data: enrichProducts(products.filter((p: any) => p.isSeckill)) }
  })

  /* 将嵌套分类展平为一维数组（带 parentId）并获取所有后代ID */
  function getDescendantIds(categoryId: number): number[] {
    const cats = dataCache['categories'] || []
    const flat: any[] = []
    function flatten(list: any[], parentId: number) {
      for (const c of list) {
        flat.push({ id: c.id, parentId: parentId })
        if (c.children && c.children.length) flatten(c.children, c.id)
      }
    }
    flatten(cats, 0)
    const ids: number[] = [categoryId]
    function walk(id: number) {
      flat.filter((c: any) => c.parentId === id).forEach((c: any) => {
        ids.push(c.id)
        walk(c.id)
      })
    }
    walk(categoryId)
    return ids
  }

  mockAdapter.on('get', '/api/product/list', (params) => {
    let products = [...(dataCache['products'] || [])]
    if (params?.categoryId) {
      const ids = getDescendantIds(parseInt(params.categoryId))
      products = products.filter((p: any) => ids.includes(p.categoryId))
    }
    if (params?.keyword) {
      products = products.filter((p: any) => p.name.includes(params.keyword))
    }
    return { code: 200, msg: 'success', data: enrichProducts(products) }
  })

  mockAdapter.on('get', '/api/product/search', (params) => {
    const products = dataCache['products'] || []
    const keyword = params?.keyword || ''
    return {
      code: 200,
      msg: 'success',
      data: enrichProducts(products.filter((p: any) => p.name.includes(keyword)))
    }
  })

  /* 商品详情 — 匹配 /api/product/10004 等 */
  mockAdapter.on('get', '/api/product/', (params) => {
    const products = dataCache['products'] || []
    const allSkus = dataCache['product-skus'] || []
    const allReviews = dataCache['reviews'] || []
    const id = extractIdFromUrl(params?._url || '')
    const product = products.find((p: any) => p.id === id)
    if (!product) return { code: 404, msg: '商品不存在', data: null }
    const enriched = enrichProducts([product])[0]
    const productSkus = allSkus.filter((s: any) => s.productId === id)
    const productReviews = allReviews.filter((r: any) => r.productId === id)
    return {
      code: 200,
      msg: 'success',
      data: { product: enriched, skus: productSkus, reviews: productReviews }
    }
  })

  /* ===== 购物车模块 ===== */
  mockAdapter.on('get', '/api/cart/list', () => {
    return { code: 200, msg: 'success', data: dataCache['cart-items'] || [] }
  })

  /* ===== 订单模块 ===== */
  mockAdapter.on('get', '/api/order/list', () => {
    return { code: 200, msg: 'success', data: dataCache['orders'] || [] }
  })

  mockAdapter.on('get', '/api/order/', (params) => {
    const id = parseInt(params?.id) || extractIdFromUrl(params?._url || '') || 0
    const orders = dataCache['orders'] || []
    return { code: 200, msg: 'success', data: orders.find((o: any) => o.id === id) }
  })

  /* ===== 地址模块 ===== */
  mockAdapter.on('get', '/api/address/list', () => {
    return { code: 200, msg: 'success', data: dataCache['addresses'] || [] }
  })

  /* ===== 优惠券模块 ===== */
  mockAdapter.on('get', '/api/promotion/coupons', () => {
    return { code: 200, msg: 'success', data: dataCache['coupons'] || [] }
  })

  /* ===== 支付模块 ===== */
  mockAdapter.on('post', '/api/payment/pay', () => {
    return { code: 200, msg: 'success', data: { success: true, message: '模拟支付成功', tradeNo: `MOCK${Date.now()}` } }
  })

  /* ===== 管理员/商家 API — 全部穿透到 Python FastAPI:8080 ===== */
  // 管理后台所有请求（包括 POST /api/admin/login）不注册 Mock handler
  // Mock 系统找不到 handler 时自动 fallback 到真实 HTTP 请求 → Vite proxy → Python:8080

  /* 请求拦截 — 使用 adapter 方式（比 override request 更可靠） */
  const defaultAdapter = axiosInstance.defaults.adapter

  axiosInstance.defaults.adapter = async function(config: any) {
    await delay()
    const url = (config.baseURL || '') + (config.url || '')
    const method = (config.method || 'get').toLowerCase()
    let params = config.params || config.data
    if (typeof params === 'string') {
      try { params = JSON.parse(params) } catch (_) { params = {} }
    }

    console.log('[Mock] Intercept:', method, url)

    const result = await mockAdapter.handle(method, url, { ...params, _url: url })
    console.log('[Mock] Result:', result?.code, result?.data?.length !== undefined ? `data[${result.data.length}]` : '')

    // 有 Mock 结果则直接返回（包括 200/401/501）
    if (result !== null && result !== undefined) {
      return {
        data: result,
        status: 200,
        statusText: 'OK',
        headers: config.headers || {},
        config
      }
    }

    // 无 Mock 处理器，退回真实 HTTP 请求
    console.log('[Mock] No handler, fallback to real adapter')
    if (defaultAdapter) return defaultAdapter(config)
    throw new Error('[Mock] No adapter available')
  }
}
