import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import VueCropper from 'vue-cropper';
import 'vue-cropper/dist/index.css'
import 'ant-design-vue/dist/reset.css'
import '@/access.ts'
import { vBtnAnimate } from '@/directives/vBtnAnimate'
import { vCardLift } from '@/directives/vCardLift'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(VueCropper)

app.directive('btn-animate', vBtnAnimate)
app.directive('card-lift', vCardLift)

app.mount('#app')
