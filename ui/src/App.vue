<template>
  <div class="demo-collapse">
    <div style="display: flex; justify-content: space-between;">
      <el-button @click="startDownload" :disabled="downloadButton">开始下载</el-button>
      <el-select v-model="select" placeholder="Select" style="width: 240px" @change="selectChange">
        <el-option
            v-for="item in selectList"
            :key="item.value"
            :label="item.label"
            :value="item.value"
        />
      </el-select>
    </div>
    <div style="margin: 4px;"></div>
    <el-collapse accordion v-model="collapseValue" @change="showResidue = false">
      <el-collapse-item :name="i"
                        v-for="(it,i) in pageList">
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
          {{ info(it['videoList']) }}
        </template>
        <div>
          <el-card shadow="never"
                   style="margin: 3px 0;"
                   v-for="video in it['videoList'].filter((_,index) => index <= 10 || showResidue)"
                   v-if="collapseValue === i">
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
              <div>
                剩余 {{ video.downloadInfo.timeRemaining?.toFixed(2) }} 分钟
                {{ (video.downloadInfo.speed)?.toFixed(2) }} MB/S
              </div>
            </div>
          </el-card>
          <el-button @click="showResidue = true">显示剩余 {{ it['videoList'].length - 10 }} 项</el-button>
        </div>
      </el-collapse-item>
    </el-collapse>
    <div style="margin: 4px;"></div>
    <el-pagination background layout="prev, pager, next" v-model:page-size="page.size" :total="page.total"
                   v-model:current-page="page.currentPage" default-current-page="1" @change="pageChange"/>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {ElMessage} from "element-plus";

/**
 * 待开始
 * @param item
 * @returns {boolean}
 */
let waitingToStartFilter = item => !item.downloadInfo.start && !item.downloadInfo.end
/**
 * 进行中
 * @param item
 * @returns {boolean}
 */
let underwayFilter = item => item.downloadInfo.start && !item.downloadInfo.end
/**
 * 异常
 */
let errorFilter = item => item.downloadInfo.error
/**
 * 已完成
 * @param item
 * @returns {false}
 */
let doneFilter = (item) => !item.downloadInfo.error && item.downloadInfo.end

const selectList = ref([
  {
    value: 0,
    label: '全部',
    fun: () => true
  },
  {
    value: 1,
    label: '待开始',
    fun: waitingToStartFilter
  },
  {
    value: 2,
    label: '进行中',
    fun: underwayFilter
  },
  {
    value: 3,
    label: '异常',
    fun: errorFilter
  },
  {
    value: 4,
    label: '已完成',
    fun: doneFilter
  },
])

const collapseValue = ref(-1)

const select = ref(0)

const list = ref([])

const downloadButton = ref(true)

const pageList = ref([])

const showResidue = ref(false)

const page = ref({
  size: 15,
  currentPage: 1,
  total: 1
})

let info = (list) => {
  return `待开始${list.filter(waitingToStartFilter).length}
          进行中${list.filter(underwayFilter).length}
          异常${list.filter(errorFilter).length}
          已完成${list.filter(doneFilter).length}
          `
}

let startDownload = () => {
  fetch('/api/download')
      .then(res => res.json())
      .then(res => {
        let message = res.message;
        if (res.code === 200) {
          ElMessage.success(message)
          return
        }
        ElMessage.error(message)
      })
}

let selectChange = () => {
  page.value.currentPage = 1
  pageChange()
}

let pageChange = async () => {
  let size = page.value.size
  let currentPage = page.value.currentPage
  let start = (size * (currentPage - 1))
  let end = size * (currentPage - 1) + size

  // 进行中
  let underwayAll = []
  // 待开始
  let waitingToStartAll = []
  // 已完成
  let doneAll = []

  let dataList = list.value
  dataList.forEach(it => {
    // 待开始
    let waitingToStart = []
    // 进行中
    let underway = []
    // 异常
    let error = []
    // 已完成
    let done = []

    let videoList = it['videoList']
    videoList
        .filter(selectList.value[select.value].fun)
        .forEach(video => {
          if (underwayFilter(video)) {
            underway.push(video)
            return;
          }
          if (waitingToStartFilter(video)) {
            waitingToStart.push(video)
            return;
          }
          if (errorFilter(video)) {
            error.push(video)
            return
          }
          done.push(video)
        })

    videoList = [].concat(underway, waitingToStart, error, done)
    if (videoList.length < 1) {
      return
    }
    // 正在进行
    if (videoList.filter(underwayFilter).length > 0) {
      underwayAll.push(it)
      return
    }
    // 待开始
    if (videoList.filter(waitingToStartFilter).length > 0) {
      waitingToStartAll.push(it)
      return
    }
    // 已完成
    doneAll.push(it)
  })
  dataList = [].concat(underwayAll, waitingToStartAll, doneAll)
  page.value.total = dataList.length
  pageList.value = dataList.slice(start, end)
}

setInterval(async () => {
  await fetch('/api/list')
      .then(res => res.json())
      .then(res => {
        let data = res.data
        downloadButton.value = data['loadIng']
        list.value = data.list
      })
  await pageChange()
}, 3000)
</script>
