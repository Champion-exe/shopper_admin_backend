# 管理员后台 (Admin Backend)

基于 Python FastAPI 的管理员后台服务，对接 Shopper 电商平台前端，替代 Java 后台中标记为"开发中"的管理员 API 骨架。

## 技术栈

| 层 | 技术 |
|------|------|
| 框架 | FastAPI (Python 3.8+) |
| 数据验证 | Pydantic v2 |
| 数据存储 | `shopper-main/data/*.json` 共享数据文件 |
| 并发控制 | portalocker 文件锁 |
| API 文档 | Swagger UI（自动生成） |

## 项目结构

```
admin-backend/
├── main.py              # FastAPI 服务（全部接口）
├── requirements.txt     # Python 依赖
├── README.md            # 本文档
└── SKILL.md             # 开发规范 / API 设计说明
```

运行时依赖的外部数据目录：

```
shopper-main/data/       # 与前端、商家后台共享
├── users.json           # 用户（账号 / 密码 / 角色）
├── products.json        # 商品基础信息
├── product-skus.json    # 商品 SKU（规格 / 价格 / 库存）
├── orders.json          # 订单（含商品明细 / 收货地址）
├── categories.json      # 三级商品分类
└── addresses.json       # 用户收货地址
```

## 功能清单

| 模块 | 功能 | 状态 |
|------|------|------|
| 登录鉴权 | 管理员 / 商家登录，Token 机制 | ✅ |
| 数据看板 | 商品总数、用户总数、今日订单、今日收入、订单状态分布 | ✅ |
| 商品管理 | 列表（分页+搜索）、新增、编辑、删除（含 SKU） | ✅ |
| 订单管理 | 列表（分页+状态筛选）、发货、取消 | ✅ |
| 用户管理 | 列表（分页+搜索+角色筛选）、封禁 / 解封、角色修改 | ✅ |

## 用户角色与权限

| 角色 | 登录后台 | 看板 | 商品管理 | 订单管理 | 用户管理 |
|------|---------|------|---------|---------|---------|
| admin | ✅ | ✅ | ✅ | ✅ | ✅ |
| seller | ✅ | ✅ | ✅ | ✅ | ✅ |
| user | ❌ 403 | - | - | - | - |
| 被封禁（role=""） | ❌ 403 | - | - | - | - |

**管理员保护**：admin 角色用户在前端页面不显示操作按钮，无法被封禁或修改角色。

## 启动方式

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 启动服务
python main.py
# → http://localhost:8080
# Swagger UI: http://localhost:8080/docs
```

如果 8080 端口被占用：

```bash
uvicorn main:app --reload --port 9000
```

> 换端口后需同步修改 `shopper-main/frontend/vite.config.ts` 的 proxy target。

## API 接口

所有接口统一响应格式：

```json
{ "code": 200, "msg": "success", "data": { ... } }
```

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/admin/login` | 管理员 / 商家登录，返回 token |
| GET | `/api/admin/dashboard` | 数据看板统计 |
| GET | `/api/admin/product/list` | 商品列表（`keyword`, `categoryId`, `page`, `size`） |
| POST | `/api/admin/product/save` | 新增商品（无 `id`）/ 编辑商品（有 `id`） |
| DELETE | `/api/admin/product/{id}` | 删除商品，同步删除关联 SKU |
| GET | `/api/admin/order/list` | 订单列表（`status`, `page`, `size`） |
| PUT | `/api/admin/order/{id}/ship` | 发货（仅 `pending_ship` 状态可操作） |
| PUT | `/api/admin/order/{id}/cancel` | 取消订单（`completed` / `cancelled` 不可操作） |
| GET | `/api/admin/user/list` | 用户列表（`keyword`, `role`, `page`, `size`，密码自动脱敏） |
| PUT | `/api/admin/user/{id}/status` | 修改用户角色 / 封禁（`role: ""` 即为封禁） |

### 订单状态流转

```
pending_payment → pending_ship → pending_receipt → completed
                       ↓
                   cancelled
```

### 响应码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 业务错误（如订单状态不允许当前操作） |
| 401 | 未登录或 token 无效 |
| 403 | 无权访问（非 admin / seller 角色） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 前后端联调

### 架构

```
浏览器 → localhost:5173 (Vite)
              │
        ┌─────┴──────┐
        ▼            ▼
  /api/admin/*   /api/*
  Python:8080    Java:9090
  (本服务)       (用户端)
```

### 联调步骤

1. **启动本服务**：`python main.py`
2. **启动前端**：`cd shopper-main/frontend && npm install && npm run dev`
3. **关闭 Mock**：确认 `frontend/.env` 中有 `VITE_USE_MOCK=false`
4. **打开页面**：`http://localhost:5173/admin/login`
5. **登录**：`admin` / `admin123`

## 测试账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| `admin` | `admin123` | admin | 系统管理员，所有权限 |
| `seller01` | `seller123` | seller | 华为旗舰店商家 |
| `seller02` | `seller123` | seller | Apple 专营店商家 |
| `zhangsan` | `123456` | user | 普通用户，无法登录后台 |
| `lisi` | `123456` | user | 普通用户（可用于测试封禁功能） |

## 已知问题

| 问题 | 影响 | 处理 |
|------|------|------|
| **Windows 跨语言文件锁不互通** | Java/Node.js 同时写 JSON 可能覆盖 | 演示时错开操作，Mac/Linux 无此问题 |
| **Token 存储在内存** | 服务重启后需重新登录 | 课程演示场景可接受 |
| **密码明文存储** | 安全问题 | 课程作业，非生产环境 |
| **数据文件路径依赖** | 必须放在 `shopper-main/data/` 下 | 启动日志会打印数据目录，便于确认 |

