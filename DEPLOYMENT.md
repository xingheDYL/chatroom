# 部署说明文档

## 服务器环境要求

- **JDK**: 1.8+
- **Maven**: 3.6+
- **MySQL**: 5.7+
- **Redis**: 任意稳定版本
- **Node.js**: 14+ (仅用于构建前端)

## 部署步骤

### 一、后端部署

#### 1. 修改配置文件

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatroom?createDatabaseIfNotExist=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: YOUR_MYSQL_PASSWORD  # 修改为你的 MySQL 密码

  data:
    redis:
      host: localhost
      port: 6379

  mail:
    host: smtp.qq.com
    port: 465
    username: YOUR_EMAIL@qq.com  # 修改为你的邮箱
    password: YOUR_AUTH_CODE     # 修改为你的邮箱授权码

jwt:
  secret: your-super-secret-jwt-key-change-this-in-production-min-256-bits  # 生产环境请修改

cors:
  allowed-origins: http://YOUR_SERVER_IP:8901  # 修改为你的服务器IP
```

#### 2. 构建后端

```bash
cd backend
mvn clean package -DskipTests
```

构建完成后，JAR 文件位于：`backend/target/chatroom-1.0.0.jar`

#### 3. 运行后端

```bash
java -jar target/chatroom-1.0.0.jar
```

或者使用 nohup 后台运行：

```bash
nohup java -jar target/chatroom-1.0.0.jar > backend.log 2>&1 &
```

### 二、前端部署

#### 1. 修改环境配置

编辑 `frontend/.env.production`，将 `YOUR_SERVER_IP` 替换为你的服务器 IP：

```env
VUE_APP_API_BASE_URL=http://YOUR_SERVER_IP:8901
VUE_APP_WS_BASE_URL=ws://YOUR_SERVER_IP:8901
```

#### 2. 构建前端

```bash
cd frontend
npm install
npm run build
```

构建完成后，静态文件位于：`frontend/dist/` 目录

#### 3. 部署前端到 Nginx

安装 Nginx：

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx
```

创建 Nginx 配置文件：

```bash
sudo nano /etc/nginx/sites-available/chatroom
```

添加以下配置：

```nginx
server {
    listen 80;
    server_name YOUR_SERVER_IP;  # 或你的域名

    # 前端静态文件
    location / {
        root /path/to/frontend/dist;  # 修改为实际路径
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://localhost:8901/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 代理到后端
    location /ws/ {
        proxy_pass http://localhost:8901/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

启用配置：

```bash
# 创建符号链接
sudo ln -s /etc/nginx/sites-available/chatroom /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

### 三、防火墙配置

开放必要的端口：

```bash
# Ubuntu/Debian
sudo ufw allow 80/tcp
sudo ufw allow 8901/tcp
sudo ufw allow 443/tcp

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --reload
```

### 四、使用 Systemd 管理后端服务（推荐）

创建服务文件：

```bash
sudo nano /etc/systemd/system/chatroom.service
```

添加以下内容：

```ini
[Unit]
Description=Chatroom Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=YOUR_USERNAME
WorkingDirectory=/path/to/backend
ExecStart=/usr/bin/java -jar /path/to/backend/target/chatroom-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动和管理服务：

```bash
# 重新加载 systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start chatroom

# 设置开机自启
sudo systemctl enable chatroom

# 查看状态
sudo systemctl status chatroom

# 查看日志
sudo journalctl -u chatroom -f
```

## 访问应用

部署完成后，通过浏览器访问：

```
http://YOUR_SERVER_IP
```

## 故障排查

### 后端无法启动

1. 检查 JDK 版本：`java -version`
2. 检查端口占用：`netstat -tlnp | grep 8901`
3. 查看日志：`tail -f backend.log`

### 前端无法访问后端

1. 检查后端是否运行：`curl http://localhost:8901/api/auth/login`
2. 检查 Nginx 配置：`sudo nginx -t`
3. 查看 Nginx 日志：`sudo tail -f /var/log/nginx/error.log`

### WebSocket 连接失败

1. 确认 Nginx 配置中包含了 WebSocket 升级配置
2. 检查防火墙是否允许 WebSocket 连接

### MySQL 连接失败

1. 检查 MySQL 服务状态：`sudo systemctl status mysql`
2. 确认数据库用户名密码正确
3. 检查 MySQL 是否允许远程连接
