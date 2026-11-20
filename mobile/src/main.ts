import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles.css'

createApp(App).use(router).mount('#app')

// 支持 Android 返回键：由原生壳调用 window.history.back()
window.addEventListener('popstate', () => {
  /* 浏览器默认处理 */
})
