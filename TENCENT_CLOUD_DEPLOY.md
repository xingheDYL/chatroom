# 腾讯云服务器部署指南

## 腾讯云服务器环境

假设你使用的是：
- **系统**: Ubuntu 20.04 / 22.04 或 CentOS 7/8
- **应用**: Spring Boot + Vue + MySQL + Redis

## 一、服务器初始化配置

### 1. 登录服务器

```bash
# 使用 SSH 登录（Windows 用户可用 PuTTY 或 PowerShell）
ssh root@你的服务器公网IP

# 或使用腾讯云的 WebShell
```

### 2. 更新系统

**Ubuntu/Debian:**
```bash
sudo apt update && sudo apt upgrade -y
```

**CentOS:**
```bash
sudo yum update -y
```

### 3. 安装必要软件

**Ubuntu/Debian:**
```bash
sudo apt install -y git curl wget vim net-tools
```

**CentOS:**
```bash
sudo yum install -y git curl wget vim net-tools
```

### 4. 配置腾讯云安全组

在腾讯云控制台配置安全组，开放以下端口：

| 端口 | 协议 | 说明 |
|------|------|------|
| 80 | TCP | HTTP (前端访问) |
| 443 | TCP | HTTPS (可选，用于SSL) |
| 8901 | TCP | 后端 API (可选择性开放，建议只通过内网访问) |
| 3306 | TCP | MySQL (建议只允许本地访问，不要开放公网) |
| 6379 | TCP | Redis (建议只允许本地访问，不要开放公网) |

**重要**:
- ✅ 开放 80 端口
- ❌ 不要开放 3306 (MySQL) 和 6379 (Redis) 到公网
- 8901 建议只通过 Nginx 反向代理访问

## 二、安装 Java 环境 (JDK 8)

**Ubuntu/Debian:**
```bash
sudo apt install -y openjdk-8-jdk

# 验证安装
java -version
```

**CentOS:**
```bash
sudo yum install -y java-1.8.0-openjdk-devel

# 验证安装
java -version
```

## 三、安装 MySQL 5.7

**Ubuntu 20.04/22.04:**
```bash
# 安装 MySQL
sudo apt install -y mysql-server

# 启动 MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

**CentOS 7:**
```bash
# 下载并安装 MySQL 5.7
sudo yum localinstall -y https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
sudo yum install -y mysql-community-server --nogpgcheck

# 启动 MySQL
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 获取临时密码
sudo grep 'temporary password' /var/log/mysqld.log

# 安全配置
sudo mysql_secure_installation
```

**创建数据库和用户：**
```bash
sudo mysql -u root -p
```

在 MySQL 命令行中执行：
```sql
CREATE DATABASE chatroom CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'chatroom'@'localhost' IDENTIFIED BY '你的数据库密码';
GRANT ALL PRIVILEGES ON chatroom.* TO 'chatroom'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

## 四、安装 Redis

**Ubuntu/Debian:**
```bash
sudo apt install -y redis-server

# 启动 Redis
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 测试 Redis
redis-cli ping
# 应该返回 PONG
```

**CentOS:**
```bash
sudo yum install -y redis

# 启动 Redis
sudo systemctl start redis
sudo systemctl enable redis
```

## 五、安装 Node.js (用于构建前端)

```bash
# 安装 Node.js 18.x
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 或使用 CentOS
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo yum install -y nodejs

# 验证安装
node -v
npm -v
```

## 六、安装 Nginx

**Ubuntu/Debian:**
```bash
sudo apt install -y nginx

# 启动 Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

**CentOS:**
```bash
sudo yum install -y nginx

# 启动 Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

## 七、部署项目

### 方式一：使用 Git 克隆项目（推荐）

```bash
# 安装 Git（如果还没安装）
sudo apt install -y git

# 克隆项目到服务器
cd /opt
sudo git clone 你的项目仓库地址 chatroom
cd chatroom
```

### 方式二：手动上传项目

**在本地电脑上打包项目：**
```bash
# 只上传源代码，排除 node_modules 和 target
tar --exclude='node_modules' --exclude='target' --exclude='.git' -czf chatroom.tar.gz chatroom/
```

**上传到服务器：**
```bash
# 使用 scp 上传
scp chatroom.tar.gz root@你的服务器IP:/opt/

# 或使用腾讯云的 WebShell 上传功能
```

**在服务器上解压：**
```bash
cd /opt
tar -xzf chatroom.tar.gz
```

### 八、配置后端

```bash
cd /opt/chatroom/backend

# 编辑配置文件
sudo vim src/main/resources/application.yml
```

修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatroom?createDatabaseIfNotExist=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: chatroom
    password: 你的数据库密码

  data:
    redis:
      host: localhost
      port: 6379

  mail:
    host: smtp.qq.com
    port: 465
    username: 你的邮箱@qq.com
    password: 你的邮箱授权码

jwt:
  secret: 生产环境请使用更安全的密钥-至少256位

cors:
  allowed-origins: http://你的服务器IP
```

### 九、构建后端

```bash
cd /opt/chatroom/backend

# 构建（跳过测试）
mvn clean package -DskipTests

# JAR 文件位置
ls -lh target/chatroom-1.0.0.jar
```

### 十、配置前端

```bash
cd /opt/chatroom/frontend

# 创建生产环境配置
cat > .env.production << EOF
VUE_APP_API_BASE_URL=http://你的服务器IP:8901
VUE_APP_WS_BASE_URL=ws://你的服务器IP:8901
EOF

# 安装依赖
npm install

# 构建
npm run build

# 检查构建结果
ls -la dist/
```

### 十一、配置 Systemd 服务（后端自动启动）

```bash
sudo vim /etc/systemd/system/chatroom.service
```

添加以下内容：

```ini
[Unit]
Description=Chatroom Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/chatroom/backend
ExecStart=/usr/bin/java -jar /opt/chatroom/backend/target/chatroom-1.0.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

启动服务：

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
sudo journalctl -u chatroom -f -n 100
```

### 十二、配置 Nginx

```bash
sudo vim /etc/nginx/sites-available/chatroom
```

添加以下配置：

```nginx
server {
    listen 80;
    server_name 你的服务器公网IP;

    # 前端静态文件
    location / {
        root /opt/chatroom/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://localhost:8901/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
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

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

启用配置：

```bash
# 删除默认配置（可选）
sudo rm /etc/nginx/sites-enabled/default

# 创建符号链接
sudo ln -s /etc/nginx/sites-available/chatroom /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

## 十三、配置防火墙

**Ubuntu (UFW):**
```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS

# 启用防火墙
sudo ufw enable

# 查看状态
sudo ufw status
```

**CentOS (firewalld):**
```bash
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# 查看状态
sudo firewall-cmd --list-all
```

**同时确保在腾讯云控制台配置安全组开放 80 和 443 端口**

## 十四、验证部署

```bash
# 1. 检查后端状态
sudo systemctl status chatroom

# 2. 检查 Nginx 状态
sudo systemctl status nginx

# 3. 检查端口监听
netstat -tlnp | grep -E '80|8901'

# 4. 测试后端 API
curl http://localhost:8901/api/auth/login

# 5. 查看后端日志
sudo journalctl -u chatroom -f

# 6. 查看 Nginx 日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

## 十五、访问应用

在浏览器中访问：

```
http://你的服务器公网IP
```

## 常见问题排查

### 1. 无法访问网站

```bash
# 检查 Nginx 是否运行
sudo systemctl status nginx

# 检查防火墙
sudo ufw status

# 检查腾讯云安全组是否开放 80 端口
```

### 2. 后端启动失败

```bash
# 查看详细日志
sudo journalctl -u chatroom -n 100 --no-pager

# 检查 MySQL 连接
sudo mysql -u chatroom -p

# 检查 Redis
redis-cli ping
```

### 3. 前端可以访问但 API 调用失败

```bash
# 检查后端是否运行
curl http://localhost:8901/api/auth/login

# 检查 Nginx 配置
sudo nginx -t

# 查看后端日志
sudo journalctl -u chatroom -f
```

### 4. WebSocket 连接失败

1. 检查 Nginx 配置中是否有 WebSocket 升级配置
2. 确认前端配置中的 WebSocket 地址正确
3. 查看后端日志是否有 WebSocket 连接记录

### 5. 邮件发送失败

1. 确认 QQ 邮箱授权码正确
2. 检查后端日志中的具体错误信息
3. 确认服务器可以访问外网

## 更新部署

当需要更新代码时：

```bash
cd /opt/chatroom

# 拉取最新代码
git pull

# 更新后端
cd backend
mvn clean package -DskipTests
sudo systemctl restart chatroom

# 更新前端
cd ../frontend
npm run build
# 静态文件已更新，无需重启 Nginx
```

## 备份建议

```bash
# 数据库备份
sudo mysqldump -u root -p chatroom > chatroom_backup_$(date +%Y%m%d).sql

# 恢复数据库
sudo mysql -u root -p chatroom < chatroom_backup_2024xxxx.sql
```

建议设置定时自动备份：
```bash
# 编辑 crontab
crontab -e

# 每天凌晨 3 点备份数据库
0 3 * * * mysqldump -u root -p你的密码 chatroom > /opt/backups/chatroom_$(date +\%Y\%m\%d).sql
```
