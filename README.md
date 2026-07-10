# 管理员后台 (Admin Backend - Java)

基于 Spring Boot 3 + Vue 3 的管理员后台模块，小组作业"复刻京东电商平台"。

## 技术栈

| 层 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.3 |
| 语言 | Java 17 |
| 数据层 | Spring Data JPA + Hibernate |
| 数据库 | H2 文件模式 |
| 鉴权 | Spring Security + JWT (jjwt 0.12.5) |
| 密码加密 | BCrypt |
| API 文档 | Knife4j (Swagger3) |
| 构建 | Maven |

## 功能清单

| 模块 | 功能 | 接口路径 |
|------|------|---------|
| 登录 | 管理员/商家登录（JWT+BCrypt） | POST /api/auth/login, /api/admin/login |
| 数据看板 | 商品/用户/订单统计 | GET /api/admin/dashboard |
| 商品管理 | 列表(分页+搜索)、新增、编辑、删除 | /api/admin/product/* |
| 订单管理 | 列表(状态筛选)、发货、取消 | /api/admin/order/* |
| 用户管理 | 列表(角色筛选+搜索)、封禁/解封 | /api/admin/user/* |

## 订单状态流转

```
pending_payment → pending_ship → pending_receipt → completed
                       ↓
                   cancelled
```

## 用户角色与权限

| 角色 | 登录后台 | 看板 | 商品管理 | 订单管理 | 用户管理 |
|------|---------|------|---------|---------|---------|
| admin | ✅ | ✅ | ✅ | ✅ | ✅ |
| seller | ✅ | ✅ | ✅ | ✅ | ✅ |
| user | ❌ 403 | - | - | - | - |
| 被封禁 | ❌ 403 | - | - | - | - |

## 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "timestamp": 1720456800000
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 业务错误 |
| 401 | 未登录或密码错误 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

## 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| seller01 | seller123 | 商家 |
| zhangsan | 123456 | 普通用户 |

## 已知问题

| 问题 | 影响 | 处理 |
|------|------|------|
| Windows 跨语言文件锁不互通 | Java/Python 同时写 JSON 可能覆盖 | 演示错开操作，Mac/Linux 无此问题 |
| 密码明文存储 | data/users.json 密码为明文 | 课程作业非生产环境，启动时自动 BCrypt 加密入库 |
