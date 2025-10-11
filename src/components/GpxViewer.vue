<template>
  <div class="gpx-viewer">
    <!-- 左侧控制面板 -->
    <div class="control-panel">
      <div class="panel-header">
        <h2>GPX 路径查看器</h2>
      </div>
      
      <!-- 文件上传 -->
      <div class="upload-section">
        <input 
          type="file" 
          ref="fileInput"
          @change="handleFileUpload"
          accept=".gpx"
          id="gpx-file"
        />
        <label for="gpx-file" class="upload-btn">
          选择 GPX 文件
        </label>
      </div>

      <!-- 数据信息 -->
      <div v-if="gpxData" class="info-section">
        <h3>运动数据</h3>
        <div class="info-item">
          <span class="label">开始时间：</span>
          <span class="value">{{ formatDateTime(gpxData.startTime) }}</span>
        </div>
        <div class="info-item">
          <span class="label">总时长：</span>
          <span class="value">{{ formatDuration(gpxData.totalTime) }}</span>
        </div>
        <div class="info-item">
          <span class="label">总距离：</span>
          <span class="value">{{ formatDistance(gpxData.totalDistance) }}</span>
        </div>
        <div class="info-item">
          <span class="label">轨迹点数：</span>
          <span class="value">{{ gpxData.pointCount }}</span>
        </div>
        <div class="info-item" v-if="gpxData.activityType">
          <span class="label">活动类型：</span>
          <span class="value">{{ gpxData.activityType }}</span>
        </div>
      </div>

      <!-- 地图样式切换 -->
      <div class="map-style-section">
        <h3>地图模式</h3>
        <div class="map-mode-controls">
          <label class="radio-label" :class="{ active: mapMode === 'satellite' }">
            <input type="radio" value="satellite" v-model="mapMode" @change="changeMapMode">
            <span class="radio-content">
              <span class="radio-icon">🛰️</span>
              <span class="radio-text">卫星影像</span>
            </span>
          </label>
          <label class="radio-label" :class="{ active: mapMode === 'satellite-road' }">
            <input type="radio" value="satellite-road" v-model="mapMode" @change="changeMapMode">
            <span class="radio-content">
              <span class="radio-icon">🗺️</span>
              <span class="radio-text">带文字的卫星</span>
            </span>
          </label>
          <label class="radio-label" :class="{ active: mapMode === 'normal' }">
            <input type="radio" value="normal" v-model="mapMode" @change="changeMapMode">
            <span class="radio-content">
              <span class="radio-icon">🗾</span>
              <span class="radio-text">2D 地图</span>
            </span>
          </label>
        </div>
      </div>

      <!-- 活动类型设置 -->
      <div v-if="gpxData" class="activity-section">
        <h3>活动类型</h3>
        <input 
          type="text" 
          v-model="activityName" 
          @input="updateActivityName"
          placeholder="例如：户外步行、晨跑等"
          class="activity-input"
        />
      </div>

      <!-- 颜色设置 -->
      <div v-if="gpxData" class="color-section">
        <h3>路径颜色</h3>
        <div class="color-controls">
          <div class="color-item">
            <label class="color-label">
              <span>颜色</span>
              <input 
                type="color" 
                v-model="routeColor" 
                @change="updateRouteColor"
                class="color-picker"
              />
            </label>
            <button @click="resetRouteColor" class="reset-btn">重置</button>
          </div>
          <div class="color-presets-inline">
            <button 
              v-for="color in colorPresets" 
              :key="'route-' + color.value"
              @click="setRouteColor(color.value)"
              class="preset-color-small"
              :style="{ backgroundColor: color.value }"
              :title="color.name"
            ></button>
          </div>
        </div>
      </div>

      <!-- 导出功能 -->
      <div v-if="gpxData" class="export-section">
        <h3>导出图片</h3>
        <div class="export-controls">
          <div class="export-scale">
            <label>导出倍数：</label>
            <select v-model="exportScale">
              <option value="1">1x (标准)</option>
              <option value="2">2x (推荐)</option>
              <option value="3">3x (高清)</option>
              <option value="4">4x (超清)</option>
            </select>
          </div>
          <button @click="exportImage" class="export-btn" :disabled="exporting">
            {{ exporting ? '导出中...' : '导出图片' }}
          </button>
        </div>
      </div>

      <!-- 提示信息 -->
      <div class="tip-section">
        <p class="tip">💡 提示：拖动地图查看详情，滚轮缩放</p>
        <p class="tip" style="margin-top: 8px;">🗺️ 坐标已自动从 WGS84 转换为 GCJ-02</p>
        <p class="tip" style="margin-top: 8px;">📍 地图模式说明：</p>
        <p class="tip-small" style="margin-top: 4px;">• 卫星影像：纯净卫星图，无任何文字</p>
        <p class="tip-small" style="margin-top: 2px;">• 带文字的卫星：卫星图+地名文字标注</p>
        <p class="tip-small" style="margin-top: 2px;">• 2D地图：传统矢量地图样式</p>
      </div>
    </div>

    <!-- 右侧预览区域 -->
    <div class="preview-panel"> 
      <div ref="previewContent" class="preview-content">
        <!-- 地图容器（整个区域） -->
        <div ref="mapContainer" class="preview-map">
          <!-- 地图会在这里初始化 -->
        </div>
        
        <!-- 叠加的数据信息 -->
        <div v-if="gpxData" class="preview-overlay">
          <!-- 顶部信息 -->
          <div class="preview-header">
            <div class="preview-title">{{ activityName }}</div>
            <div class="preview-time">{{ formatDateTime(gpxData.startTime) }}</div>
          </div>
          
          <!-- 底部数据 -->
          <div class="preview-stats">
            <div class="stat-row">
              <div class="stat-item">
                <div class="stat-label">运动时间</div>
                <div class="stat-value">{{ formatDuration(gpxData.totalTime) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">里程</div>
                <div class="stat-value">{{ formatDistance(gpxData.totalDistance) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载提示 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>{{ loadingText }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { XMLParser } from 'fast-xml-parser'
import html2canvas from 'html2canvas'

// 高德地图 Key - 使用公共测试 Key（建议替换成您自己的）
const AMAP_KEY = '1700798238bf5ff1d138946d98d1b108'
const AMAP_SECURITY_CODE = 'b1e3976a8c2ebf69ccf86bd5f0bd34e3'

// 检测是否在 Android 环境中
const isAndroid = () => {
  return typeof window !== 'undefined' && window.Android !== undefined
}

const mapContainer = ref(null)
const fileInput = ref(null)
const map = ref(null)
const AMap = ref(null)
const gpxData = ref(null)
const loading = ref(false)
const loadingText = ref('加载中...')
// 地图模式: 'satellite' - 纯卫星, 'satellite-road' - 卫星+路网, 'normal' - 2D地图
const mapMode = ref('satellite')  // 默认纯卫星模式

// 导出相关
const exportScale = ref(2)
const exporting = ref(false)
const previewContent = ref(null)

// 活动类型
const activityName = ref('开放水域游泳')

// 颜色设置
const routeColor = ref('#3887be')
const defaultRouteColor = '#3887be'

// 颜色预设
const colorPresets = [
  { name: '蓝色', value: '#3887be' },
  { name: '红色', value: '#ef4444' },
  { name: '绿色', value: '#22c55e' },
  { name: '紫色', value: '#a855f7' },
  { name: '橙色', value: '#f97316' },
  { name: '粉色', value: '#ec4899' }
]

// 地图图层
const satelliteLayer = ref(null)
const roadNetLayer = ref(null)

// 路径相关对象
let polyline = null
let startMarker = null
let endMarker = null
const kmMarkers = []  // 公里标记数组

// 初始化地图
onMounted(async () => {
  loading.value = true
  loadingText.value = '正在加载地图...'
  
  try {
    // 设置安全密钥（如果您的Key需要安全密钥，请在控制台设置后取消下面的注释）
    // window._AMapSecurityConfig = {
    //   securityJsCode: AMAP_SECURITY_CODE
    // }
    
    // 加载高德地图
    AMap.value = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.ControlBar']
    })
    
    // 创建地图实例
    map.value = new AMap.value.Map(mapContainer.value, {
      zoom: 13,
      // center: [119.54, 35.43],
      viewMode: '2D',
      pitch: 0,
      mapStyle: 'amap://styles/normal',
      // 隐藏所有UI控件
      showIndoorMap: false,
      features: ['bg', 'road', 'building', 'point'],  // 默认显示所有要素
      // 不显示地图logo（但会保留版权信息）
    })
    
    // 不添加任何控件，保持地图简洁
    
    // 初始化图层
    
    // 1. 卫星影像图层
    satelliteLayer.value = new AMap.value.TileLayer.Satellite({
      visible: true,  // 默认显示卫星图层
      zIndex: 1
    })
    map.value.add(satelliteLayer.value)
    
    // 2. 路网图层（在卫星图上显示道路名称）
    roadNetLayer.value = new AMap.value.TileLayer.RoadNet({
      visible: false,
      zIndex: 2
    })
    map.value.add(roadNetLayer.value)
    
    // 应用默认的地图模式（纯卫星，无文字）
    changeMapMode()
    
    loading.value = false
  } catch (error) {
    console.error('地图加载失败:', error)
    const msg = '地图加载失败，请刷新页面重试'
    if (isAndroid()) {
      window.Android.showToast(msg)
      window.Android.log('GPX-Viewer', '地图加载失败: ' + error.message)
    } else {
      alert(msg)
    }
    loading.value = false
  }
})

// 清理
onUnmounted(() => {
  if (map.value) {
    map.value.destroy()
  }
})

// 处理文件上传
const handleFileUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  loading.value = true
  loadingText.value = '正在解析 GPX 文件...'
  
  try {
    const text = await file.text()
    parseGPX(text)
  } catch (error) {
    console.error('文件读取失败:', error)
    const msg = '文件读取失败，请重试'
    if (isAndroid()) {
      window.Android.showToast(msg)
      window.Android.log('GPX-Viewer', '文件读取失败: ' + error.message)
    } else {
      alert(msg)
    }
  } finally {
    loading.value = false
  }
}

// 解析 GPX 文件
const parseGPX = (gpxText) => {
  const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: '@_'
  })
  
  const result = parser.parse(gpxText)
  const gpx = result.gpx
  
  // 提取元数据
  const metadata = gpx.metadata || {}
  const trk = gpx.trk
  const extensions = trk.extensions || {}
  
  // 提取轨迹点
  const trackSegments = Array.isArray(trk.trkseg) ? trk.trkseg : [trk.trkseg]
  const allPoints = []
  
  trackSegments.forEach(segment => {
    const points = Array.isArray(segment.trkpt) ? segment.trkpt : [segment.trkpt]
    points.forEach(point => {
      // 原始 WGS84 坐标
      const wgsLng = parseFloat(point['@_lon'])
      const wgsLat = parseFloat(point['@_lat'])
      
      // 转换为 GCJ-02 坐标（高德地图坐标系）
      const gcj02 = transformWGS84ToGCJ02(wgsLng, wgsLat)
      
      allPoints.push({
        lng: gcj02.lng,
        lat: gcj02.lat,
        time: point.time
      })
    })
  })
  
  // 构建数据对象
  gpxData.value = {
    startTime: metadata.time || allPoints[0]?.time,
    totalTime: parseFloat(extensions.totalTime) || 0,
    totalDistance: parseFloat(extensions.totalDistance) || 0,
    pointCount: allPoints.length,
    activityType: trk.type || '',
    points: allPoints
  }
  
  // 设置活动类型
  // activityName.value = gpxData.value.activityType || '户外运动'
  
  // 在地图上显示
  displayRouteOnMap(allPoints)
}

// 在地图上显示路径
const displayRouteOnMap = (points) => {
  if (!map.value || !AMap.value || points.length === 0) return
  
  // 清除旧的覆盖物
  clearMapOverlays()
  
  // 构建路径坐标数组
  const path = points.map(p => [p.lng, p.lat])
  
  // 创建折线
  polyline = new AMap.value.Polyline({
    path: path,
    strokeColor: routeColor.value,
    strokeWeight: 5,
    strokeOpacity: 0.8,
    lineJoin: 'round',
    lineCap: 'round'
  })
  map.value.add(polyline)
  
  // 添加公里标记（每1km一个）
  addKilometerMarkers(points)
  
  // 添加起点标记
  const startPoint = points[0]
  startMarker = new AMap.value.Marker({
    position: [startPoint.lng, startPoint.lat],
    icon: createStartIcon(),
    offset: new AMap.value.Pixel(-12, -12),
    zIndex: 200
  })
  map.value.add(startMarker)
  
  // 添加起点信息窗体
  const startInfo = new AMap.value.InfoWindow({
    content: `<div style="padding: 10px;"><strong>起点</strong><br/>${formatDateTime(startPoint.time)}</div>`,
    offset: new AMap.value.Pixel(0, -30)
  })
  startMarker.on('click', () => {
    startInfo.open(map.value, [startPoint.lng, startPoint.lat])
  })
  
  // 添加终点标记
  const endPoint = points[points.length - 1]
  endMarker = new AMap.value.Marker({
    position: [endPoint.lng, endPoint.lat],
    icon: createEndIcon(),
    offset: new AMap.value.Pixel(-12, -12),
    zIndex: 200
  })
  map.value.add(endMarker)
  
  // 添加终点信息窗体
  const endInfo = new AMap.value.InfoWindow({
    content: `<div style="padding: 10px;"><strong>终点</strong><br/>${formatDateTime(endPoint.time)}</div>`,
    offset: new AMap.value.Pixel(0, -30)
  })
  endMarker.on('click', () => {
    endInfo.open(map.value, [endPoint.lng, endPoint.lat])
  })
  
  // 自动调整视野以显示完整路径
  map.value.setFitView()
}

// 清除地图覆盖物
const clearMapOverlays = () => {
  if (polyline) {
    map.value.remove(polyline)
    polyline = null
  }
  if (startMarker) {
    map.value.remove(startMarker)
    startMarker = null
  }
  if (endMarker) {
    map.value.remove(endMarker)
    endMarker = null
  }
  kmMarkers.forEach(marker => map.value.remove(marker))
  kmMarkers.length = 0
}

// WGS84 坐标转 GCJ-02 坐标（GPS 坐标转火星坐标）
const transformWGS84ToGCJ02 = (wgsLng, wgsLat) => {
  const PI = Math.PI
  const a = 6378245.0  // 长半轴
  const ee = 0.00669342162296594323  // 偏心率平方
  
  // 判断是否在国内，不在国内则不做偏移
  if (isOutOfChina(wgsLng, wgsLat)) {
    return { lng: wgsLng, lat: wgsLat }
  }
  
  let dLat = transformLat(wgsLng - 105.0, wgsLat - 35.0)
  let dLng = transformLng(wgsLng - 105.0, wgsLat - 35.0)
  const radLat = wgsLat / 180.0 * PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * PI)
  dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * PI)
  const mgLat = wgsLat + dLat
  const mgLng = wgsLng + dLng
  
  return { lng: mgLng, lat: mgLat }
}

// 判断是否在中国境外
const isOutOfChina = (lng, lat) => {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

// 纬度转换
const transformLat = (lng, lat) => {
  const PI = Math.PI
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
  ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0
  return ret
}

// 经度转换
const transformLng = (lng, lat) => {
  const PI = Math.PI
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

// 计算两个经纬度点之间的距离（米）
const calculateDistance = (lat1, lng1, lat2, lng2) => {
  const R = 6371000 // 地球半径（米）
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

// 添加公里标记
const addKilometerMarkers = (points) => {
  if (points.length < 2) return
  
  let cumulativeDistance = 0  // 累计距离（米）
  let nextKmMark = 1000  // 下一个公里标记位置（米）
  let kmCount = 1  // 公里计数
  
  for (let i = 1; i < points.length; i++) {
    const p1 = points[i - 1]
    const p2 = points[i]
    
    // 计算这两点之间的距离
    const segmentDistance = calculateDistance(p1.lat, p1.lng, p2.lat, p2.lng)
    const prevDistance = cumulativeDistance
    cumulativeDistance += segmentDistance
    
    // 检查是否跨越了公里标记点
    while (nextKmMark <= cumulativeDistance && nextKmMark <= prevDistance + segmentDistance) {
      // 计算标记点在当前线段上的位置（比例）
      const ratio = (nextKmMark - prevDistance) / segmentDistance
      const markerLng = p1.lng + (p2.lng - p1.lng) * ratio
      const markerLat = p1.lat + (p2.lat - p1.lat) * ratio
      
      // 创建公里标记
      const kmMarker = new AMap.value.Marker({
        position: [markerLng, markerLat],
        icon: createKmIcon(kmCount),
        offset: new AMap.value.Pixel(-12, -12),
        zIndex: 150
      })
      
      // 添加信息窗体
      const infoWindow = new AMap.value.InfoWindow({
        content: `<div style="padding: 8px; font-size: 14px;"><strong>${kmCount} 公里</strong></div>`,
        offset: new AMap.value.Pixel(0, -30)
      })
      
      kmMarker.on('click', () => {
        infoWindow.open(map.value, [markerLng, markerLat])
      })
      
      map.value.add(kmMarker)
      kmMarkers.push(kmMarker)
      
      // 准备下一个公里标记
      nextKmMark += 1000
      kmCount++
    }
  }
}

// 创建起点图标
const createStartIcon = () => {
  return new AMap.value.Icon({
    size: new AMap.value.Size(24, 24),
    image: 'data:image/svg+xml;base64,' + btoa(`
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="10" fill="#22c55e" stroke="white" stroke-width="2"/>
        <circle cx="12" cy="12" r="4" fill="white"/>
      </svg>
    `),
    imageSize: new AMap.value.Size(24, 24)
  })
}

// 创建终点图标
const createEndIcon = () => {
  return new AMap.value.Icon({
    size: new AMap.value.Size(24, 24),
    image: 'data:image/svg+xml;base64,' + btoa(`
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="10" fill="#ef4444" stroke="white" stroke-width="2"/>
        <circle cx="12" cy="12" r="4" fill="white"/>
      </svg>
    `),
    imageSize: new AMap.value.Size(24, 24)
  })
}

// 创建公里标记图标
const createKmIcon = (kmNumber) => {
  return new AMap.value.Icon({
    size: new AMap.value.Size(24, 24),
    image: 'data:image/svg+xml;base64,' + btoa(`
      <svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="10" fill="white" stroke="${routeColor.value}" stroke-width="2"/>
        <text x="12" y="12" text-anchor="middle" dominant-baseline="central" 
              font-size="10" font-weight="bold" fill="${routeColor.value}">${kmNumber}</text>
      </svg>
    `),
    imageSize: new AMap.value.Size(24, 24)
  })
}

// 切换地图模式
const changeMapMode = () => {
  if (!map.value || !satelliteLayer.value || !roadNetLayer.value) return
  
  switch (mapMode.value) {
    case 'satellite':
      // 纯卫星模式（只显示卫星图，完全不显示任何文字标注）
      map.value.setFeatures([])  // 隐藏所有矢量要素（包括文字、道路等）
      satelliteLayer.value.show()
      roadNetLayer.value.hide()
      break
      
    case 'satellite-road':
      // 带文字的卫星模式（卫星图+文字标注，不显示路网）
      map.value.setFeatures(['point'])  // 只显示POI标注点（地名文字），不显示道路
      satelliteLayer.value.show()
      roadNetLayer.value.hide()  // 不显示路网图层
      break
      
    case 'normal':
      // 2D标准地图模式（矢量地图）
      map.value.setFeatures(['bg', 'road', 'building', 'point'])  // 显示所有要素
      satelliteLayer.value.hide()
      roadNetLayer.value.hide()
      map.value.setMapStyle('amap://styles/normal')
      break
  }
}

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds) return '-'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)
  
  if (hours > 0) {
    return `${hours} 小时 ${minutes} 分钟 ${secs} 秒`
  } else if (minutes > 0) {
    return `${minutes} 分钟 ${secs} 秒`
  } else {
    return `${secs} 秒`
  }
}

// 格式化距离
const formatDistance = (meters) => {
  if (!meters) return '-'
  if (meters >= 1000) {
    return `${(meters / 1000).toFixed(2)} 公里`
  } else {
    return `${meters.toFixed(0)} 米`
  }
}

// 更新路径颜色
const updateRouteColor = () => {
  if (gpxData.value && gpxData.value.points) {
    displayRouteOnMap(gpxData.value.points)
  }
}

// 设置路径颜色
const setRouteColor = (color) => {
  routeColor.value = color
  updateRouteColor()
}

// 重置路径颜色
const resetRouteColor = () => {
  routeColor.value = defaultRouteColor
  updateRouteColor()
}

// 更新活动类型
const updateActivityName = () => {
  // 活动类型会自动通过v-model更新，无需额外操作
}

// 导出图片
const exportImage = async () => {
  if (!previewContent.value || !gpxData.value || exporting.value) {
    console.log('导出条件不满足:', { 
      hasPreview: !!previewContent.value, 
      hasData: !!gpxData.value,
      isExporting: exporting.value 
    })
    const msg = '请先上传 GPX 文件'
    if (isAndroid()) {
      window.Android.showToast(msg)
    } else {
      alert(msg)
    }
    return
  }
  
  if (!map.value) {
    const msg = '地图还未加载完成，请稍后再试'
    if (isAndroid()) {
      window.Android.showToast(msg)
    } else {
      alert(msg)
    }
    return
  }
  
  exporting.value = true
  loadingText.value = '正在生成图片...'
  loading.value = true
  
  try {
    console.log('开始导出图片...')
    if (isAndroid()) {
      window.Android.log('GPX-Viewer', '开始导出图片...')
    }
    
    // 等待地图完全渲染
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 强制重绘地图
    if (map.value) {
      map.value.render()
    }
    
    // 再等待一帧
    await new Promise(resolve => requestAnimationFrame(resolve))
    
    console.log('开始截图...')
    
    // 获取实际的预览区域尺寸
    const rect = previewContent.value.getBoundingClientRect()
    console.log('预览区域实际尺寸:', rect.width, 'x', rect.height)
    
    // 截取整个预览区域（包括地图和叠加层）
    const canvas = await html2canvas(previewContent.value, {
      useCORS: true,
      allowTaint: true,
      backgroundColor: null,  // 透明背景
      scale: parseFloat(exportScale.value),
      width: rect.width,
      height: rect.height,
      logging: false,
      foreignObjectRendering: false,
      imageTimeout: 15000,
      onclone: (clonedDoc) => {
        console.log('DOM克隆完成，尝试清理地图控件...')
        // 在克隆的文档中也隐藏控件
        const clonedMap = clonedDoc.querySelector('.preview-map')
        if (clonedMap) {
          const controls = clonedMap.querySelectorAll('.amap-logo, .amap-copyright, .amap-toolbar, .amap-controlbar, .amap-controls, .amap-scalecontrol')
          controls.forEach(el => {
            el.style.display = 'none'
            el.style.visibility = 'hidden'
            el.style.opacity = '0'
          })
        }
      }
    })
    
    console.log('截图完成，尺寸:', canvas.width, 'x', canvas.height)
    
    // 生成文件名
    const timestamp = new Date(gpxData.value.startTime).toISOString().replace(/[:.]/g, '-').split('T')[0]
    const fileName = `GPX-${timestamp}-${Date.now()}-${exportScale.value}x.png`
    
    // 获取 base64 数据
    const dataUrl = canvas.toDataURL('image/png', 0.95)
    
    // 根据环境选择下载方式
    if (isAndroid()) {
      // Android 环境：使用 JSBridge 下载
      window.Android.downloadBase64File(dataUrl, fileName)
    } else {
      // Web 环境：使用浏览器下载
      const link = document.createElement('a')
      link.download = fileName
      link.href = dataUrl
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      alert('图片导出成功！文件名：' + fileName)
    }
    
  } catch (error) {
    console.error('导出失败:', error)
    const msg = '导出失败: ' + error.message
    if (isAndroid()) {
      window.Android.showToast(msg)
      window.Android.log('GPX-Viewer', '导出失败: ' + error.message)
    } else {
      alert(msg + '\n请查看控制台了解详情')
    }
  } finally {
    exporting.value = false
    loading.value = false
  }
}
</script>

<style scoped>
.gpx-viewer {
  width: 100%;
  height: 100%;
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
}

.control-panel {
  width: 350px;
  height: calc(100vh - 40px);
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 20px;
  overflow-y: auto;
  flex-shrink: 0;
}

.panel-header h2 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #1f2937;
}

.upload-section {
  margin-bottom: 20px;
}

input[type="file"] {
  display: none;
}

.upload-btn {
  display: block;
  width: 100%;
  padding: 12px 20px;
  background: #3887be;
  color: white;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: background 0.3s;
  font-weight: 500;
}

.upload-btn:hover {
  background: #2c6a99;
}

.info-section {
  margin-bottom: 20px;
  padding: 15px;
  background: #f9fafb;
  border-radius: 8px;
}

.info-section h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: #374151;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-item .label {
  color: #6b7280;
}

.info-item .value {
  color: #1f2937;
  font-weight: 500;
}

.map-style-section {
  margin-bottom: 20px;
}

.map-style-section h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: #374151;
}

.map-mode-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.radio-label {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.radio-label:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.radio-label.active {
  background: #eff6ff;
  border-color: #3887be;
}

.radio-label input[type="radio"] {
  position: absolute;
  opacity: 0;
  cursor: pointer;
}

.radio-content {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.radio-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.radio-text {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.radio-label.active .radio-text {
  color: #3887be;
}

.color-section {
  margin-bottom: 20px;
}

.color-section h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: #374151;
}

.color-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.color-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.color-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
}

.color-picker {
  width: 50px;
  height: 35px;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.color-picker:hover {
  border-color: #3887be;
}

.reset-btn {
  padding: 6px 12px;
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.reset-btn:hover {
  background: #e5e7eb;
  border-color: #9ca3af;
}

.color-presets {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.color-presets-inline {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  padding-left: 8px;
}

.preset-label {
  font-size: 13px;
  color: #6b7280;
}

.preset-color {
  width: 32px;
  height: 32px;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.preset-color:hover {
  transform: scale(1.1);
  border-color: #3887be;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.preset-color-small {
  width: 24px;
  height: 24px;
  border: 2px solid #e5e7eb;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.preset-color-small:hover {
  transform: scale(1.15);
  border-color: #3887be;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.activity-section {
  margin-bottom: 20px;
}

.activity-section h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: #374151;
}

.activity-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.activity-input:focus {
  outline: none;
  border-color: #3887be;
  box-shadow: 0 0 0 3px rgba(56, 135, 190, 0.1);
}

.activity-input::placeholder {
  color: #9ca3af;
}

.export-section {
  margin-bottom: 20px;
}

.export-section h3 {
  font-size: 16px;
  margin-bottom: 12px;
  color: #374151;
}

.export-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-btn,
.export-btn {
  width: 100%;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.preview-btn {
  background: #f3f4f6;
  color: #374151;
}

.preview-btn:hover {
  background: #e5e7eb;
}

.export-btn {
  background: #3887be;
  color: white;
}

.export-btn:hover:not(:disabled) {
  background: #2c6a99;
}

.export-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.export-scale {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #374151;
}

.export-scale select {
  flex: 1;
  padding: 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.tip-section {
  padding: 12px;
  background: #fef3c7;
  border-radius: 8px;
  border-left: 4px solid #f59e0b;
}

.tip {
  margin: 0;
  font-size: 13px;
  color: #92400e;
  line-height: 1.5;
}

.tip-small {
  margin: 0;
  font-size: 12px;
  color: #a16207;
  line-height: 1.4;
  padding-left: 8px;
}

/* 右侧预览面板 */
.preview-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.preview-label {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.95);
  color: #ef4444;
  font-size: 24px;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  letter-spacing: 2px;
  border: 2px solid #ef4444;
}

.preview-content {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  position: relative;
  /* 使用容器查询方式保持3:4比例 */
}

/* 左右结构：始终保持3:4比例 */
@media (min-width: 901px) {
  .preview-content {
    /* 先尝试按高度计算宽度 */
    width: calc((100vh - 40px) * 3 / 4);
    height: calc(100vh - 40px);
  }
  
  /* 如果宽度超出可用空间，则按宽度计算高度 */
  @supports (width: min(1px, 1px)) {
    .preview-content {
      width: min(calc((100vh - 40px) * 3 / 4), calc(100vw - 350px - 60px));
      height: min(calc(100vh - 40px), calc((100vw - 350px - 60px) * 4 / 3));
    }
  }
}

/* 小屏幕：上下结构 */
@media (max-width: 900px) {
  .gpx-viewer {
    flex-direction: column;
    padding: 10px;
    gap: 10px;
    overflow-y: auto;
  }
  
  .control-panel {
    width: 100%;
    height: auto;
    max-height: none;
  }
  
  .preview-panel {
    width: 100%;
  }
  
  .preview-content {
    /* 上下结构：宽度100%，高度按3:4比例 */
    width: calc(100vw - 20px);
    height: calc((100vw - 20px) * 4 / 3);
  }
  
  .preview-label {
    top: 10px;
    right: 10px;
    font-size: 18px;
    padding: 6px 12px;
  }
}

.preview-map {
  width: 100%;
  height: 100%;
  background: #e5e7eb;
  position: absolute;
  top: 0;
  left: 0;
}

/* 隐藏高德地图的控件和版权信息 */
.preview-map :deep(.amap-logo),
.preview-map :deep(.amap-copyright),
.preview-map :deep(.amap-scalecontrol),
.preview-map :deep(.amap-toolbar),
.preview-map :deep(.amap-controlbar),
.preview-map :deep(.amap-controls) {
  display: none !important;
  opacity: 0 !important;
  visibility: hidden !important;
}

/* 叠加层 */
.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  pointer-events: none;
}

.preview-header {
  background: linear-gradient(180deg, rgba(0,0,0,0.5) 0%, rgba(0,0,0,0) 100%);
  padding: 16px;
  color: white;
}

.preview-title {
  font-size: 20px;
  font-weight: 600;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  margin-bottom: 8px;
}

.preview-time {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.preview-stats {
  background: linear-gradient(0deg, rgba(255,255,255,0.95) 0%, rgba(255,255,255,0.85) 100%);
  /* backdrop-filter: blur(10px); */
  padding: 16px 20px;
  border-radius: 20px 20px 0 0;
}

.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 10px 0; 
  border-radius: 8px; 
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #e5e7eb;
  border-top-color: #3887be;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-overlay p {
  margin-top: 16px;
  color: #6b7280;
  font-size: 16px;
}

/* 滚动条样式 */
.control-panel::-webkit-scrollbar {
  width: 6px;
}

.control-panel::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}

.control-panel::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 10px;
}

.control-panel::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
