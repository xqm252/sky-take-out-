# 苍穹外卖 (Sky Take-Out)

一个完整的外卖点餐系统，包含管理后台、用户端小程序和服务端 API。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7.3 |
| 数据访问 | MyBatis + PageHelper |
| 数据库 | MySQL 8.0 |
| 连接池 | Druid |
| 缓存 | Redis |
| 接口文档 | Knife4j (Swagger) |
| 认证授权 | JWT（管理端 + 用户端双通道） |
| 文件存储 | 阿里云 OSS |
| 支付 | 微信支付 API v3 |
| 实时推送 | WebSocket |
| 定时任务 | Spring Scheduling |
| Excel 导出 | Apache POI |
| 管理前端 | Vue 2 + TypeScript + Element UI + ECharts |
| 用户端 | uni-app（微信小程序） |

## 功能模块

### 管理端
- **工作台** — 今日订单/营业额/菜品/套餐概览
- **员工管理** — 员工增删改查、状态管理
- **分类管理** — 菜品分类、套餐分类
- **菜品管理** — 菜品增删改查、口味管理、起售/停售
- **套餐管理** — 套餐增删改查、起售/停售
- **订单管理** — 订单查询、接单、拒单、派送、完成
- **数据统计** — 营业额/用户/订单统计、销量排名 TOP10、导出报表
- **实时提醒** — WebSocket 推送新订单通知

### 用户端（微信小程序）
- 浏览菜品/套餐、加入购物车
- 收货地址管理
- 下单、微信支付
- 订单历史查询

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+

### 1. 初始化数据库

```sql
-- 执行项目根目录下的 SQL 脚本
mysql -u root -p < sky.sql
```

脚本会自动创建 `sky_take_out` 数据库并导入初始化数据（包含分类、菜品等示例数据）。

### 2. 配置文件

创建 `sky-server/src/main/resources/application-dev.yml`：

```yaml
sky:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: sky_take_out
    username: root
    password: 你的数据库密码
  redis:
    host: 127.0.0.1
    port: 6379
    password: 你的Redis密码
    database: 10
  alioss:                                    # 非必需，文件上传功能需要
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: 你的AccessKey
    access-key-secret: 你的AccessSecret
    bucket-name: 你的Bucket名称
  wechat:                                    # 非必需，微信支付功能需要
    appid: 你的小程序AppID
    secret: 你的小程序Secret
    mchid: 商户号
    mchSerialNo: 商户证书序列号
    privateKeyFilePath: 商户私钥路径
    apiV3Key: APIv3密钥
    weChatPayCertFilePath: 微信支付平台证书路径
    notifyUrl: 支付回调地址
    refundNotifyUrl: 退款回调地址
```

> 也可参考 `application-dev.yml.example` 模板文件。

### 3. 启动服务

```bash
cd sky-take-out
mvn clean compile
mvn spring-boot:run
```

服务启动后访问：

- **API 接口文档**: [http://localhost:8080/doc.html](http://localhost:8080/doc.html)
- **管理端 API**: `http://localhost:8080/admin/**`
- **用户端 API**: `http://localhost:8080/user/**`

### 4. 启动管理端前端

```bash
cd ../project-sky-admin-vue-ts
npm install
npm run serve
# 访问 http://localhost:8888
```

### 5. 启动小程序

在微信开发者工具中打开 `mp-weixin/` 目录（此为 uni-app 编译产物，源码需在 uni-app 项目中维护）。

## 项目结构

```
sky-take-out/
├── sky-common/              # 公共模块
│   └── com/sky/
│       ├── constant/        # 常量定义
│       ├── context/         # ThreadLocal 上下文
│       ├── enumeration/     # 枚举类
│       ├── exception/       # 自定义异常
│       ├── json/            # JSON 序列化配置
│       ├── properties/      # 配置属性类
│       ├── result/          # 统一响应结果
│       └── utils/           # 工具类（JWT、OSS、HTTP、微信支付）
├── sky-pojo/                # 数据模型模块
│   └── com/sky/
│       ├── dto/             # 数据传输对象
│       ├── entity/          # 数据库实体
│       └── vo/              # 视图对象
├── sky-server/              # 主服务模块
│   └── com/sky/
│       ├── annotation/      # 自定义注解（@AutoFill）
│       ├── aspect/          # AOP 切面（自动填充）
│       ├── config/          # 配置类
│       ├── controller/
│       │   ├── admin/       # 管理端接口
│       │   ├── user/        # 用户端接口
│       │   └── nofity/      # 支付回调接口
│       ├── handler/         # 全局异常处理
│       ├── interceptor/     # JWT 拦截器
│       ├── mapper/          # MyBatis Mapper
│       ├── service/         # 业务逻辑层
│       ├── task/            # 定时任务
│       └── websocket/       # WebSocket 服务
└── sky.sql                  # 数据库初始化脚本
```

## API 接口

接口文档通过 Knife4j 自动生成，启动服务后访问 `/doc.html`。

分为两个接口组：
- **管理端接口** — 扫描 `com.sky.controller.admin` 包
- **用户端接口** — 扫描 `com.sky.controller.user` 包

### 认证方式

- 管理端登录后获取 JWT Token，请求头携带 `token`
- 用户端登录后获取 JWT Token，请求头携带 `authentication`

## 定时任务

| 任务 | 执行频率 | 说明 |
|------|---------|------|
| 超时订单自动取消 | 每分钟 | 下单超过15分钟未支付的订单自动取消 |
| 派送订单自动完成 | 每天凌晨 1:00 | 派送超过60分钟的订单自动标记为已完成 |
| WebSocket 消息推送 | 每5秒 | 推送新订单提醒 |

## 许可

本项目仅供学习参考。
