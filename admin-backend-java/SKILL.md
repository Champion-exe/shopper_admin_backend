# Skill: Shopper 电商平台后端开发（Spring Boot 3 专属）

## 1. 角色与核心职责

你是一名专注于 **Shopper 电商项目** 的资深 Java 后端开发工程师。你的核心职责是基于 **Spring Boot 3.x** 构建 RESTful API，严格匹配前端 Vue 3 项目的数据结构。你的代码必须遵循 **"数据契约优先"** 原则，即后端实体类、JSON 字段必须与 `data/*.json` 保持绝对一致。

## 2. 强制性技术栈约束（不可更改）

- **语言版本**：Java 17 及以上。
- **核心框架**：Spring Boot 3.x (Spring MVC)。
- **数据层**：Spring Data JPA (Hibernate)。
- **数据库**：开发环境强制使用 **H2** （文件模式 `jdbc:h2:file:./data/shopper`），生产环境可切换 MySQL。
- **安全框架**：Spring Security + JWT (无状态 Session)。
- **构建工具**：Maven (pom.xml)。
- **API 文档**：Knife4j (Swagger3 增强版)。

## 3. 项目包结构铁律（生成代码时必须遵循）

新建类时必须放入对应的包下，严禁随意放置：

text

```
com.shopper
├── controller       # 接收请求，返回统一响应 (仅做参数校验和转发)
│   └── admin        # 管理员接口 (/api/admin/**)
├── service          # 业务逻辑接口
│   └── impl         # 业务逻辑实现类 (核心代码写在这里)
├── entity           # JPA 实体类 (与 data/*.json 字段 1:1 映射)
├── repository       # JPA 数据访问层 (继承 JpaRepository)
├── dto              # 请求/响应数据传输对象 (禁止将 Entity 直接暴露给前端)
├── config           # 配置类 (SecurityConfig, CorsConfig, SwaggerConfig)
├── utils            # 工具类 (JwtUtil, RedisUtil)
└── exception        # 全局异常处理 (GlobalExceptionHandler)
```



## 4. 数据实体（Entity）硬性定义标准

**前端共享的 `data/\*.json` 即是数据库 Schema**。生成 Entity 时必须严格对应，举例：

- **Product 实体** 必须包含：`id`, `name`, `description`, `brand`, `categoryId`, `sellerId`, `mainImage`, `images`(List/String), `tags`(List/String), `sales`, `rating`, `isHot`, `isNew`, `isSeckill`, `createTime`。
- **User 实体** 必须包含：`id`, `username`, `password`(BCrypt加密), `nickname`, `phone`, `avatar`, `role`(USER/ADMIN/SELLER), `shopName`, `createTime`。
- **Order 实体** 必须包含：`id`, `userId`, `orderNo`(唯一), `totalAmount`, `status`, `address`, `createTime`。
- **关系映射**：禁止使用复杂双向关联（如 `@OneToMany` 直接加载 List），推荐使用 `@Transient` 或单独查询，防止 JSON 序列化死循环。

## 5. API 接口设计与响应契约（强制标准）

- **基础路径**：所有接口统一前缀 `/api`。

- **响应格式**：强制使用以下 JSON 结构，**严禁**返回裸数据或 Map：

  json

  ```
  {
    "code": 200,        // 200成功，401未登录，403无权限，500系统异常
    "message": "success",
    "data": { ... },    // 可以是对象、列表或 null
    "timestamp": 1234567890
  }
  ```

  

- **分页格式**：接收 `page` 和 `size` 参数，返回 `PageResult` 包含 `list`, `total`, `pageNum`, `pageSize`。

- **RESTful 风格**：

  - GET `/api/products` (分页查询)
  - GET `/api/products/{id}` (详情)
  - POST `/api/cart` (加入购物车)
  - POST `/api/auth/login` (登录)

## 6. 认证与安全开发规范

- **JWT 校验**：在 `SecurityConfig` 中配置过滤器，放行 `/api/auth/**`, `/doc.html`, `/h2-console/**`。
- **获取当前用户**：禁止在 Controller 参数里接收裸 token，必须封装工具类 `SecurityUtils.getCurrentUserId()` 从上下文中获取。
- **权限注解**：Controller 方法必须使用 `@PreAuthorize("hasRole('ADMIN')")` 或 `@PreAuthorize("hasRole('SELLER')")` 进行硬性拦截。
- **跨域**：必须配置 `CorsFilter` 允许 `http://localhost:5173` (Vite 默认端口)。

## 7. 业务逻辑层（Service）编码规范

- **事务管理**：增删改方法必须标注 `@Transactional`。
- **异常处理**：禁止 try-catch 吞掉异常，统一 throw 自定义业务异常（如 `BusinessException`），由全局拦截器处理。
- **缓存**：查询热点数据（如商品详情）必须使用 `@Cacheable(value = "product", key = #id)`，并引入 Redis。

## 8. 开发阶段默认数据源配置（启动即用）

生成 `application.yml` 时默认使用以下配置，确保前端同学无需安装 MySQL 即可联调：

yaml

```
spring:
  datasource:
    url: jdbc:h2:file:./data/shopper;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: update # 启动自动建表/更新字段
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```



## 9. 当前待实现的核心功能清单（TODO）

根据前端现有代码，当前需优先完成后端接口开发（按优先级排序）：

1. **认证模块**：注册、登录（返回 JWT）、获取当前用户信息。
2. **商品模块**：首页商品列表（支持分类筛选、关键词搜索）、商品详情。
3. **购物车模块**：列表查询、添加/修改数量、删除选中商品。
4. **订单模块**：创建订单（扣库存）、订单列表、取消订单。
5. **管理后台（Admin）**：商品上架/下架、订单发货处理、数据统计概览。