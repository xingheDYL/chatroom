const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  pages: {
    index: {
      entry: 'src/main.js',
      template: 'index.html',
      filename: 'index.html',
      title: '聊天室',
    }
  },
  devServer: {
    port: 5173,
    host: '0.0.0.0',
    allowedHosts: 'all',
    // 完全禁用 WebSocket 服务器
    webSocketServer: false,
    hot: false,
    liveReload: false,
    client: {
      overlay: false
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8901',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8901',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
