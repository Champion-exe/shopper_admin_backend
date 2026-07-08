# 管理员后台 — 开发规范

## 技术栈
- Python 3.8+
- FastAPI 框架
- Pydantic 数据验证
- portalocker 文件锁
- 数据存储：`shopper-main/data/` 下多个 JSON 文件

## 项目架构

```
浏览器 → localhost:5173 (Vite)
              │
        ┌─────┴──────┐
        ▼            ▼
  /api/admin/*   /api/*
  Python:8080    Java:9090
  (本服务)       (用户端)
```

## API 接口

### 鉴权方式
- POST `/api/admin/login` 获取 token
- 其他接口在 `Authorization` 请求头中携带 token
- token 格式：`mock-admin-token-{user_id}-{随机串}`

### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/login` | 管理员/商家登录 |
| GET | `/api/admin/dashboard` | 数据看板 |
| GET | `/api/admin/product/list` | 商品列表（分页+搜索） |
| POST | `/api/admin/product/save` | 新增/编辑商品 |
| DELETE | `/api/admin/product/{id}` | 删除商品 |
| GET | `/api/admin/order/list` | 订单列表（分页+筛选） |
| PUT | `/api/admin/order/{id}/ship` | 发货 |
| PUT | `/api/admin/order/{id}/cancel` | 取消订单 |
| GET | `/api/admin/user/list` | 用户列表（分页+搜索） |
| PUT | `/api/admin/user/{id}/status` | 修改用户角色 |

### 统一响应格式

```json
{ "code": 200, "msg": "success", "data": {...} }
```

### 数据模型
与 `shopper-main/data/` 中 JSON 文件的结构完全一致：
- `users.json` — 用户（id, username, password, nickname, phone, avatar, role, shopName, createTime）
- `products.json` — 商品（id, name, description, brand, categoryId, sellerId, mainImage, images, tags, sales, rating, isHot, isNew, isSeckill, createTime）
- `product-skus.json` — 商品 SKU（id, productId, spec, price, originalPrice, stock, sold）
- `orders.json` — 订单（id, orderNo, userId, status, totalAmount, payAmount, items[], consignee, phone, address, createTime...）

### 订单状态
```
pending_payment → pending_ship → pending_receipt → completed
                       ↓
                   cancelled
```

## 并发控制
使用 `portalocker` 文件锁读写 JSON 文件。Windows 环境下跨语言文件锁不互通，演示时需错开操作。

## 启动方式
```bash
pip install -r requirements.txt
python main.py
# 服务运行在 http://localhost:8080
# Swagger UI: http://localhost:8080/docs
```

## 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| seller01 | seller123 | 商家 |
| zhangsan | 123456 | 普通用户（无后台权限） |
