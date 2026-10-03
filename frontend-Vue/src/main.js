import { createApp } from 'vue'
import { createPinia } from 'pinia';
import App from './App.vue'
import 'normalize.css';
import "./style/style.css"
import Vue3Toastify from 'vue3-toastify';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(Vue3Toastify, {
  theme: "dark", 
  autoClose: 1000, 
})
app.mount('#app')