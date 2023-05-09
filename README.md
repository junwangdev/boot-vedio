使用：

1. 启动后端

2. 修改前端代码内的后端地址

   ```javascript
   // 本地地址，需要修改为后端websocket地址
   const webSocketUrl = 'wss://10.119.85.173:18080/vm/ws/' 
   ```

3. 配置并启动nginx

   ```json
   worker_processes  1;
   
   events {
       worker_connections  1024;
   }
   
   http {
       include       mime.types;
       default_type  application/octet-stream;
   
       sendfile        on;
   
       keepalive_timeout  65;
   
       server {
           #端口
           listen       18080 ssl;
           server_name  localhost.com;
           #配置ssl证书
           ssl_certificate D:\\Develop\\nginx\\nginx-1.22.1\\ssl\\nginx.crt;
           ssl_certificate_key D:\\Develop\\nginx\\nginx-1.22.1\\ssl\\nginx.key;
           ssl_session_timeout 5m;
           ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
           ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;
           ssl_prefer_server_ciphers on;
   
           #配置前端地址
           location / {
               root   F:\\web-project\\webrtc-web\\;
               index  index.html index.htm;
           }
   
   
           # 代理后端地址
           location /vm/ {
               proxy_http_version 1.1;
               proxy_set_header Upgrade $http_upgrade;
               proxy_set_header Connection "upgrade";
               proxy_set_header Host $proxy_host;
               proxy_set_header X-Real-IP $remote_addr;
               proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
               proxy_set_header X-Forwarded-Proto $scheme;
               proxy_pass https://127.0.0.1:8443/;
           }
   
    
           error_page   500 502 503 504  /50x.html;
           location = /50x.html {
               root   html;
           }
       }
   }
   
   ```

   



技术博客：

https://blog.csdn.net/wang_jing_jing/article/details/122881557

https://blog.csdn.net/wang_jing_jing/article/details/122881557

STUN和TURN服务: https://blog.csdn.net/wang_jing_jing/article/details/122875630

https://zhuanlan.zhihu.com/p/488135992

https://zhuanlan.zhihu.com/p/468792680