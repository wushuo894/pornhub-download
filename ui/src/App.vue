<template>
  <div class="demo-collapse">
    <el-button @click="startDownload">开始下载</el-button>
    <div style="margin: 4px;"></div>
    <el-collapse accordion>
      <el-collapse-item :name="i"
                        v-for="(it,i) in list.slice((page.size * (page.currentPage - 1)), page.size * (page.currentPage-1) + page.size)">
        <template #title>
          <el-avatar
              :src="it.user.avatar"
          />
          <div style="width: 8px;"></div>
          <h4 v-if="it.videoList.filter(item => item.downloadInfo.start && !item.downloadInfo.end).length > 0"
              style="color: blue;">{{ it.user.name }}</h4>
          <h4 v-else-if="it.videoList.filter(item => !item.downloadInfo.start && !item.downloadInfo.end).length < 1"
              style="color: burlywood;">{{ it.user.name }}</h4>
          <h4 v-else>{{ it.user.name }}</h4>
          <div style="width: 8px;"></div>
          {{ info(it.videoList) }}
        </template>
        <div>
          <el-card style="margin: 5px;" v-for="video in it.videoList">
            <template #header>
              <div class="card-header">
                <span>{{ video.title }}</span>
              </div>
            </template>
            <el-progress v-if="video.downloadInfo.error" :percentage="100" status="exception"/>
            <el-progress v-else-if="video.downloadInfo.end" :percentage="100" status="success"/>
            <el-progress v-else-if="!video.downloadInfo.end"
                         :percentage="Number(((video.downloadInfo.downloadLength / video.downloadInfo.length) * 100).toFixed(2))"/>
            大小 {{ (video.downloadInfo.length / (1024 * 1024)).toFixed(2) }} MB
            下载速度 {{ (video.downloadInfo.speed)?.toFixed(2) }} MB/S
          </el-card>
        </div>
      </el-collapse-item>
    </el-collapse>
    <div style="margin: 4px;"></div>
    <el-pagination background layout="prev, pager, next" v-model:page-size="page.size" :total="list.length"
                   v-model:current-page="page.currentPage" default-current-page="1"/>
  </div>
</template>

<script setup>
import {ref} from 'vue'

const activeName = ref('1')
const list = ref([])

const page = ref({
  size: 15,
  totalPage: 2,
  currentPage: 1,
})

let info = (list) => {
  return `待开始 ${list.filter(item => !item.downloadInfo.start && !item.downloadInfo.end).length}
          进行中 ${list.filter(item => item.downloadInfo.start && !item.downloadInfo.end).length}
          异常 ${list.filter(item => item.downloadInfo.error).length}
          已完成 ${list.filter(item => !item.downloadInfo.error && item.downloadInfo.end).length}
          `
}

let startDownload = () => {
  fetch('/api/download')
}

setInterval(() => {
  fetch('/api/list').then(res => res.json())
      .then(res => {
        res.map(it => {
          let videoList = it.videoList
          videoList = videoList.sort((a, b) => {
            if (a.downloadInfo.end) {
              return 1;
            }
            if (b.downloadInfo.end) {
              return -1;
            }
            return 0;
          })
          it.videoList = videoList
          return it;
        })
        res.forEach(it => {
          if (it.videoList.filter(item => item.downloadInfo.start && !item.downloadInfo.end).length > 0) {
            it.sort = 1
            return
          }
          if (it.videoList.filter(item => !item.downloadInfo.start && !item.downloadInfo.end).length > 0) {
            it.sort = 2
            return
          }
          if (it.videoList.filter(item => item.downloadInfo.end).length > 0) {
            it.sort = 3
          }
        })
        res = res.sort((a, b) => {
          if (a.sort > b.sort) {
            return 1;
          } else {
            return -1;
          }
        })
        page.value.totalPage = res.length % page.value.size > 0 ? Number((res.length / page.value.size).toFixed(0)) + 1 : Number((res.length / page.value.size).toFixed(0))
        list.value = res
      })
}, 3000)
</script>
