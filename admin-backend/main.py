"""
管理员后台 — FastAPI 服务（对接 shopper-main 前端）
"""

import json
import os
import uuid
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional

import portalocker
import uvicorn
from fastapi import Depends, FastAPI, Header, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse, JSONResponse
from pydantic import BaseModel, Field

# ============================================================
# 配置
# ============================================================

# 数据目录：指向 shopper-main/data/（与 admin-backend 同级）
DATA_DIR = Path(__file__).parent.parent / "shopper-main" / "data"

# 管理员服务端口
PORT = 8080

# ============================================================
# 枚举（对齐前端状态值）
# ============================================================

class OrderStatus(str, Enum):
    pending_payment = "pending_payment"
    pending_ship = "pending_ship"
    pending_receipt = "pending_receipt"
    completed = "completed"
    cancelled = "cancelled"


class UserRole(str, Enum):
    user = "user"
    admin = "admin"
    seller = "seller"


# ============================================================
# Pydantic 模型（对齐 shopper-main 数据结构）
# ============================================================

class LoginRequest(BaseModel):
    username: str
    password: str


class Product(BaseModel):
    id: int
    name: str
    description: str = ""
    brand: str = ""
    categoryId: int = 0
    sellerId: int = 0
    mainImage: str = ""
    images: List[str] = []
    tags: List[str] = []
    sales: int = 0
    rating: float = 0.0
    isHot: bool = False
    isNew: bool = False
    isSeckill: bool = False
    createTime: str = ""


class ProductSku(BaseModel):
    id: int
    productId: int
    spec: str
    price: float
    originalPrice: float
    stock: int
    sold: int = 0


class OrderItem(BaseModel):
    productId: int
    productName: str
    skuId: int
    spec: str
    price: float
    quantity: int
    image: str


class Order(BaseModel):
    id: int
    orderNo: str = ""
    userId: int
    status: str = "pending_payment"
    totalAmount: float = 0.0
    discountAmount: float = 0.0
    payAmount: float = 0.0
    paymentMethod: str = ""
    addressId: int = 0
    consignee: str = ""
    phone: str = ""
    address: str = ""
    remark: str = ""
    items: List[OrderItem] = []
    createTime: str = ""
    paymentTime: Optional[str] = None
    deliveryTime: Optional[str] = None
    confirmTime: Optional[str] = None


class User(BaseModel):
    id: int
    username: str
    password: str = ""
    nickname: str = ""
    phone: str = ""
    avatar: str = ""
    role: str = "user"
    shopName: Optional[str] = None
    createTime: str = ""


# ---------- 更新用模型 ----------

class ProductSaveRequest(BaseModel):
    id: Optional[int] = None
    name: str
    description: str = ""
    brand: str = ""
    categoryId: int = 0
    sellerId: int = 0
    mainImage: str = ""
    images: List[str] = []
    tags: List[str] = []
    rating: float = 0.0
    isHot: bool = False
    isNew: bool = False
    isSeckill: bool = False


class OrderStatusUpdate(BaseModel):
    status: str


class UserStatusUpdate(BaseModel):
    role: Optional[str] = None


# ============================================================
# 统一响应
# ============================================================

def success(data: Any = None, msg: str = "success") -> Dict:
    return {"code": 200, "msg": msg, "data": data}


def error(code: int = 500, msg: str = "服务器错误") -> Dict:
    return {"code": code, "msg": msg, "data": None}


# ============================================================
# 文件读写 + 并发锁
# ============================================================

def _file_path(filename: str) -> Path:
    return DATA_DIR / filename


def read_json(filename: str) -> list:
    """读取 JSON 文件，不存在则返回空列表。"""
    path = _file_path(filename)
    if not path.exists():
        return []
    with open(path, "r", encoding="utf-8") as f:
        portalocker.lock(f, portalocker.LOCK_SH)
        data = json.load(f)
        portalocker.unlock(f)
    return data


def write_json(filename: str, data: list) -> None:
    """写入 JSON 文件，使用文件锁。"""
    path = _file_path(filename)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        portalocker.lock(f, portalocker.LOCK_EX)
        json.dump(data, f, ensure_ascii=False, indent=2)
        portalocker.unlock(f)


def next_id(items: list, key: str = "id") -> int:
    """生成下一个自增 ID。"""
    if not items:
        return 1001
    return max(item.get(key, 0) for item in items) + 1


# ============================================================
# Token 管理（内存中）
# ============================================================

TOKEN_STORE: Dict[str, Dict] = {}  # token -> user_info


# ============================================================
# 鉴权依赖
# ============================================================

def get_current_user(authorization: str = Header(None)) -> Dict:
    """从 Authorization 头校验 token，返回当前登录用户信息。"""
    if not authorization:
        raise HTTPException(status_code=401, detail=error(401, "未登录，请先访问 /api/admin/login"))
    token = authorization.replace("Bearer ", "").strip()
    if token not in TOKEN_STORE:
        raise HTTPException(status_code=401, detail=error(401, "token 无效或已过期"))
    return TOKEN_STORE[token]


def require_admin(user: Dict = Depends(get_current_user)) -> Dict:
    """要求管理员或商家角色。"""
    role = user.get("role", "")
    if role not in ("admin", "seller"):
        raise HTTPException(status_code=403, detail=error(403, "无权限访问管理后台"))
    return user


# ============================================================
# FastAPI 应用
# ============================================================

app = FastAPI(
    title="管理员后台 API",
    description="复刻京东 — 管理员后台服务（兼容 shopper-main 前端）",
    version="2.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 全局异常处理：把 HTTPException 的 detail 转成统一格式
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    if isinstance(exc.detail, dict):
        return JSONResponse(status_code=exc.status_code, content=exc.detail)
    return JSONResponse(status_code=exc.status_code, content=error(exc.status_code, str(exc.detail)))


# ============================================================
# 路由
# ============================================================

# ---------- 0. 根路径 ----------

@app.get("/", include_in_schema=False)
def root():
    return RedirectResponse(url="/docs")


# ---------- 1. 管理员登录 ----------

@app.post("/api/admin/login", tags=["管理员"])
def admin_login(body: LoginRequest):
    """管理员/商家登录，返回 token。"""
    users = read_json("users.json")
    user = None
    for u in users:
        if u.get("username") == body.username and u.get("password") == body.password:
            user = u
            break

    if user is None:
        return error(401, "用户名或密码错误")

    role = user.get("role", "user")
    if role not in ("admin", "seller"):
        return error(403, "无权限访问管理后台")

    token = f"mock-admin-token-{user['id']}-{uuid.uuid4().hex[:8]}"
    TOKEN_STORE[token] = user
    return success({"token": token, "user": user}, "登录成功")


# ---------- 2. 数据看板 ----------

@app.get("/api/admin/dashboard", tags=["数据看板"])
def dashboard(user: Dict = Depends(require_admin)):
    """返回统计数据。"""
    products = read_json("products.json")
    orders = read_json("orders.json")
    users = read_json("users.json")

    today = datetime.now().strftime("%Y-%m-%d")
    today_orders = [o for o in orders if o.get("createTime", "").startswith(today)]
    today_revenue = sum(o.get("payAmount", 0) for o in today_orders if o.get("status") not in ("pending_payment", "cancelled"))

    return success({
        "productCount": len(products),
        "todayOrders": len(today_orders),
        "userCount": len(users),
        "todayRevenue": round(today_revenue, 2),
    })


# ---------- 3. 商品管理 ----------

@app.get("/api/admin/product/list", tags=["商品管理"])
def product_list(
    keyword: Optional[str] = Query(None, description="按名称搜索"),
    categoryId: Optional[int] = Query(None, description="按分类筛选"),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    user: Dict = Depends(require_admin),
):
    """分页查询商品列表。"""
    products = read_json("products.json")
    skus = read_json("product-skus.json")

    # 搜索
    if keyword:
        kw = keyword.lower()
        products = [p for p in products if kw in p.get("name", "").lower()]

    # 分类筛选
    if categoryId:
        products = [p for p in products if p.get("categoryId") == categoryId]

    # 给每个商品附上最低价格和总库存
    enriched = []
    for p in products:
        pid = p["id"]
        p_skus = [s for s in skus if s.get("productId") == pid]
        p["minPrice"] = min((s["price"] for s in p_skus), default=0) if p_skus else 0
        p["totalStock"] = sum(s.get("stock", 0) for s in p_skus)
        enriched.append(p)

    total = len(enriched)
    start = (page - 1) * size
    page_data = enriched[start : start + size]

    return success({
        "total": total,
        "page": page,
        "size": size,
        "list": page_data,
    })


@app.post("/api/admin/product/save", tags=["商品管理"])
def product_save(body: ProductSaveRequest, user: Dict = Depends(require_admin)):
    """新增或编辑商品。"""
    products = read_json("products.json")
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    if body.id:
        # 编辑
        for p in products:
            if p["id"] == body.id:
                p.update({
                    "name": body.name,
                    "description": body.description,
                    "brand": body.brand,
                    "categoryId": body.categoryId,
                    "sellerId": body.sellerId,
                    "mainImage": body.mainImage,
                    "images": body.images,
                    "tags": body.tags,
                    "rating": body.rating,
                    "isHot": body.isHot,
                    "isNew": body.isNew,
                    "isSeckill": body.isSeckill,
                })
                write_json("products.json", products)
                return success(p, "商品已更新")
        return error(404, "商品不存在")
    else:
        # 新增
        new_id = next_id(products)
        product = {
            "id": new_id,
            "name": body.name,
            "description": body.description,
            "brand": body.brand,
            "categoryId": body.categoryId,
            "sellerId": body.sellerId,
            "mainImage": body.mainImage,
            "images": body.images,
            "tags": body.tags,
            "sales": 0,
            "rating": body.rating,
            "isHot": body.isHot,
            "isNew": body.isNew,
            "isSeckill": body.isSeckill,
            "createTime": now,
        }
        products.append(product)
        write_json("products.json", products)
        return success(product, "商品已新增")


@app.delete("/api/admin/product/{product_id}", tags=["商品管理"])
def product_delete(product_id: int, user: Dict = Depends(require_admin)):
    """删除商品。"""
    products = read_json("products.json")
    skus = read_json("product-skus.json")

    before = len(products)
    products = [p for p in products if p["id"] != product_id]
    if len(products) == before:
        return error(404, "商品不存在")

    # 同时删掉关联 SKU
    skus = [s for s in skus if s.get("productId") != product_id]

    write_json("products.json", products)
    write_json("product-skus.json", skus)
    return success(None, "商品已删除")


# ---------- 4. 订单管理 ----------

@app.get("/api/admin/order/list", tags=["订单管理"])
def order_list(
    status: Optional[str] = Query(None, description="按状态筛选"),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    user: Dict = Depends(require_admin),
):
    """分页查询订单列表，支持按状态筛选。"""
    orders = read_json("orders.json")

    if status:
        orders = [o for o in orders if o.get("status") == status]

    total = len(orders)
    start = (page - 1) * size
    page_data = orders[start : start + size]

    return success({
        "total": total,
        "page": page,
        "size": size,
        "list": page_data,
    })


@app.put("/api/admin/order/{order_id}/ship", tags=["订单管理"])
def order_ship(order_id: int, user: Dict = Depends(require_admin)):
    """发货。"""
    orders = read_json("orders.json")
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    for o in orders:
        if o["id"] == order_id:
            if o["status"] != "pending_ship":
                return error(400, "只有待发货状态的订单才能发货")
            o["status"] = "pending_receipt"
            o["deliveryTime"] = now
            write_json("orders.json", orders)
            return success(o, "发货成功")
    return error(404, "订单不存在")


@app.put("/api/admin/order/{order_id}/cancel", tags=["订单管理"])
def order_cancel(order_id: int, user: Dict = Depends(require_admin)):
    """取消订单。"""
    orders = read_json("orders.json")
    for o in orders:
        if o["id"] == order_id:
            if o["status"] in ("completed", "cancelled"):
                return error(400, "已完成/已取消的订单无法取消")
            o["status"] = "cancelled"
            write_json("orders.json", orders)
            return success(o, "订单已取消")
    return error(404, "订单不存在")


# ---------- 5. 用户管理 ----------

@app.get("/api/admin/user/list", tags=["用户管理"])
def user_list(
    keyword: Optional[str] = Query(None, description="按用户名/昵称搜索"),
    role: Optional[str] = Query(None, description="按角色筛选"),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    user: Dict = Depends(require_admin),
):
    """分页查看用户列表。"""
    users = read_json("users.json")

    if keyword:
        kw = keyword.lower()
        users = [u for u in users if kw in u.get("username", "").lower() or kw in u.get("nickname", "").lower()]

    if role:
        users = [u for u in users if u.get("role") == role]

    # 脱敏：不返回密码
    safe_users = [{k: v for k, v in u.items() if k != "password"} for u in users]

    total = len(safe_users)
    start = (page - 1) * size
    page_data = safe_users[start : start + size]

    return success({
        "total": total,
        "page": page,
        "size": size,
        "list": page_data,
    })


@app.put("/api/admin/user/{user_id}/status", tags=["用户管理"])
def user_update(user_id: int, body: UserStatusUpdate, user: Dict = Depends(require_admin)):
    """修改用户角色或封禁（将 role 改为空字符串即禁用）。"""
    users = read_json("users.json")
    for u in users:
        if u["id"] == user_id:
            if body.role is not None:
                u["role"] = body.role
            write_json("users.json", users)
            safe = {k: v for k, v in u.items() if k != "password"}
            return success(safe, "用户信息已更新")
    return error(404, "用户不存在")


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print(f"数据目录: {DATA_DIR}")
    print(f"服务地址: http://127.0.0.1:{PORT}")
    print(f"Swagger UI: http://127.0.0.1:{PORT}/docs")
    uvicorn.run(app, host="127.0.0.1", port=PORT)
