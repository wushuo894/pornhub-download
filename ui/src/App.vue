<template>
  <div class="demo-collapse">
    <div style="display: flex; justify-content: space-between;">
      <el-button @click="startDownload">开始下载</el-button>
      <el-select v-model="select" placeholder="Select" style="width: 240px">
        <el-option
            v-for="item in selectList"
            :key="item.value"
            :label="item.label"
            :value="item.value"
        />
      </el-select>
    </div>
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
          <el-card shadow="never" style="margin: 3px 0;" v-for="video in it.videoList">
            <span>{{ video.title }}</span>
            <el-progress v-if="video.downloadInfo.error" :percentage="100" status="exception"/>
            <el-progress v-else-if="video.downloadInfo.end" :percentage="100" status="success"/>
            <el-progress v-else-if="!video.downloadInfo.end"
                         :percentage="Number(((video.downloadInfo.downloadLength / video.downloadInfo.length) * 100).toFixed(2))"/>
            <div style="display: flex;justify-content: space-between;">
              <div>
                {{ (video.downloadInfo.length / (1024 * 1024)).toFixed(2) }} MB
                /
                {{ (video.downloadInfo.downloadLength / (1024 * 1024)).toFixed(2) }} MB
              </div>
              <div>{{ (video.downloadInfo.speed)?.toFixed(2) }} MB/S</div>
            </div>
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

const selectList = ref([
  {
    value: 0,
    label: '全部',
    fun: () => true
  },
  {
    value: 1,
    label: '待开始',
    fun: (item) => !item.downloadInfo.start && !item.downloadInfo.end
  },
  {
    value: 2,
    label: '进行中',
    fun: (item) => item => item.downloadInfo.start && !item.downloadInfo.end
  },
  {
    value: 3,
    label: '异常',
    fun: (item) => item.downloadInfo.error
  },
  {
    value: 4,
    label: '已完成',
    fun: (item) => !item.downloadInfo.error && item.downloadInfo.end
  },
])

const select = ref(0)

const list = ref([])

const page = ref({
  size: 15,
  totalPage: 2,
  currentPage: 1,
})

let info = (list) => {
  return `待开始${list.filter(item => !item.downloadInfo.start && !item.downloadInfo.end).length}
          进行中${list.filter(item => item.downloadInfo.start && !item.downloadInfo.end).length}
          异常${list.filter(item => item.downloadInfo.error).length}
          已完成${list.filter(item => !item.downloadInfo.error && item.downloadInfo.end).length}
          `
}

let startDownload = () => {
  fetch('/api/download')
}

setInterval(() => {
  fetch('/api/list').then(res => res.json())
      .then(res => {
        res.map(it => {
          // 待开始
          let waitingToStart = []
          // 进行中
          let underway = []
          // 异常
          let error = []
          // 已完成
          let done = []

          let videoList = it.videoList
          videoList = videoList.filter(selectList.value[select.value].fun)
          videoList.forEach(video => {
            if (video.downloadInfo.error) {
              error.push(video)
              return
            }
            if (video.downloadInfo.end) {
              done.push(video)
              return
            }
            if (video.downloadInfo.start && !video.downloadInfo.end) {
              underway.push(video)
              return
            }
            waitingToStart.push(video)
          })

          it.videoList = [].concat(underway, waitingToStart, error, done)
          return it;
        })

        res = res.filter(item => item.videoList.length > 0)

        // 待开始
        let waitingToStart = []
        // 进行中
        let underway = []
        // 已完成
        let done = []

        res.forEach(it => {
          // 正在进行
          if (it.videoList.filter(item => item.downloadInfo.start && !item.downloadInfo.end).length > 0) {
            waitingToStart.push(it)
            return
          }
          // 已完成
          if (it.videoList.filter(item => !item.downloadInfo.start && !item.downloadInfo.end).length < 1) {
            done.push(it)
            return
          }
          // 待开始
          underway.push(it)
        })
        res = [].concat(waitingToStart, underway, done)
        page.value.totalPage = res.length % page.value.size > 0 ? Number((res.length / page.value.size).toFixed(0)) + 1 : Number((res.length / page.value.size).toFixed(0))
        list.value = res
      })
}, 3000)
</script>
