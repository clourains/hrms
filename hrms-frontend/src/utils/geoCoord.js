/**
 * 地理坐标映射（用于世界地图打点）
 * 未收录的城市回落到国家中心坐标；都没有则返回 null
 */

// 仓库/港口城市
const CITY_COORD = {
  深圳: [114.06, 22.54],
  义乌: [120.07, 29.31],
  上海: [121.47, 31.23],
  宁波: [121.55, 29.87],
  广州: [113.26, 23.13],
  北京: [116.41, 39.9],
  杭州: [120.16, 30.27],
  青岛: [120.38, 36.07],
  天津: [117.2, 39.13],
  厦门: [118.09, 24.48],
  成都: [104.07, 30.57],
  苏州: [120.58, 31.3],
  洛杉矶: [-118.24, 34.05],
  纽约: [-74.01, 40.71],
  西雅图: [-122.33, 47.61],
  迈阿密: [-80.19, 25.76],
  休斯顿: [-95.37, 29.76],
  汉堡: [10.0, 53.55],
  鹿特丹: [4.48, 51.92],
  新加坡: [103.82, 1.35],
  东京: [139.69, 35.69],
  首尔: [126.98, 37.57],
  迪拜: [55.27, 25.2],
  巴拿马城: [-79.53, 8.98],
  悉尼: [151.21, -33.87],
  曼谷: [100.5, 13.75],
  伦敦: [-0.13, 51.51]
}

// 港口（发货单起运/目的港）
const PORT_COORD = {
  深圳盐田港: [114.27, 22.58],
  深圳宝安机场: [113.81, 22.64],
  深圳蛇口港: [113.92, 22.48],
  深圳: [114.06, 22.54],
  上海港: [121.5, 31.23],
  宁波港: [121.87, 29.94],
  洛杉矶港: [-118.27, 33.74],
  纽约肯尼迪机场: [-73.78, 40.64],
  纽约港: [-74.02, 40.67],
  纽约: [-74.01, 40.71],
  西雅图港: [-122.35, 47.6],
  迈阿密: [-80.19, 25.76],
  汉堡港: [9.94, 53.54],
  鹿特丹港: [4.4, 51.95]
}

// 国家中心坐标（城市匹配失败时兜底）
const COUNTRY_COORD = {
  中国: [104.0, 35.0],
  美国: [-98.0, 39.0],
  德国: [10.0, 51.0],
  英国: [-3.0, 55.0],
  法国: [2.0, 46.0],
  荷兰: [5.5, 52.1],
  日本: [138.0, 36.0],
  韩国: [127.8, 36.0],
  新加坡: [103.8, 1.3],
  澳大利亚: [133.0, -25.0],
  加拿大: [-106.0, 56.0],
  墨西哥: [-102.0, 23.0],
  巴西: [-51.0, -14.0],
  印度: [78.0, 21.0],
  越南: [106.0, 16.0],
  泰国: [101.0, 15.0],
  马来西亚: [102.0, 4.0],
  印度尼西亚: [113.0, -1.0],
  阿联酋: [54.0, 24.0]
}

// 英文城市坐标（17TRACK 等第三方接口返回英文地名时使用，键为小写）
const EN_CITY_COORD = {
  // 中国
  shenzhen: [114.06, 22.54], yiwu: [120.07, 29.31], shanghai: [121.47, 31.23],
  ningbo: [121.55, 29.87], guangzhou: [113.26, 23.13], beijing: [116.41, 39.9],
  hangzhou: [120.16, 30.27], qingdao: [120.38, 36.07], tianjin: [117.2, 39.13],
  xiamen: [118.09, 24.48], chengdu: [104.07, 30.57], suzhou: [120.58, 31.3],
  'hong kong': [114.17, 22.32], taipei: [121.56, 25.03],
  // 美国
  'los angeles': [-118.24, 34.05], 'long beach': [-118.19, 33.77],
  'new york': [-74.01, 40.71], newark: [-74.17, 40.74], 'jersey city': [-74.08, 40.73],
  secaucus: [-74.06, 40.79], chicago: [-87.63, 41.88], houston: [-95.37, 29.76],
  dallas: [-96.8, 32.78], 'fort worth': [-97.33, 32.75], atlanta: [-84.39, 33.75],
  miami: [-80.19, 25.76], orlando: [-81.38, 28.54], seattle: [-122.33, 47.61],
  'san francisco': [-122.42, 37.77], oakland: [-122.27, 37.8], 'san diego': [-117.16, 32.72],
  savannah: [-81.1, 32.08], charleston: [-79.93, 32.78], norfolk: [-76.29, 36.85],
  philadelphia: [-75.17, 39.95], boston: [-71.06, 42.36], detroit: [-83.05, 42.33],
  denver: [-104.99, 39.74], phoenix: [-112.07, 33.45], 'las vegas': [-115.14, 36.17],
  'salt lake city': [-111.89, 40.76], 'kansas city': [-94.58, 39.1], 'st louis': [-90.2, 38.63],
  indianapolis: [-86.16, 39.77], columbus: [-82.99, 39.96], cincinnati: [-84.51, 39.1],
  nashville: [-86.78, 36.16], charlotte: [-80.84, 35.23], 'new orleans': [-90.07, 29.95],
  portland: [-122.68, 45.52], austin: [-97.74, 30.27], 'san antonio': [-98.49, 29.42],
  memphis: [-90.05, 35.15], louisville: [-85.76, 38.25], goodyear: [-112.36, 33.44],
  swainsboro: [-82.33, 32.6], compton: [-118.22, 33.9], ontario: [-117.65, 34.06],
  fontana: [-117.43, 34.09], anaheim: [-117.91, 33.84], 'city of industry': [-117.96, 34.02],
  hagerstown: [-77.72, 39.64], harrisburg: [-76.88, 40.27], allentown: [-75.49, 40.6],
  'little rock': [-92.29, 34.75], shreveport: [-93.75, 32.53], longview: [-94.74, 32.5],
  // 欧洲
  london: [-0.13, 51.51], manchester: [-2.24, 53.48], hamburg: [10.0, 53.55],
  frankfurt: [8.68, 50.11], munich: [11.58, 48.14], berlin: [13.4, 52.52],
  amsterdam: [4.9, 52.37], rotterdam: [4.48, 51.92], paris: [2.35, 48.86],
  madrid: [-3.7, 40.42], barcelona: [2.17, 41.39], milan: [9.19, 45.46],
  rome: [12.5, 41.9], warsaw: [21.01, 52.23], stockholm: [18.06, 59.33],
  // 亚太
  singapore: [103.82, 1.35], tokyo: [139.69, 35.69], osaka: [135.5, 34.69],
  seoul: [126.98, 37.57], busan: [129.04, 35.18], bangkok: [100.5, 13.75],
  'ho chi minh city': [106.63, 10.82], hanoi: [105.85, 21.03],
  'kuala lumpur': [101.69, 3.14], jakarta: [106.85, -6.21], manila: [120.98, 14.6],
  sydney: [151.21, -33.87], melbourne: [144.96, -37.81], auckland: [174.76, -36.85],
  // 中东 / 其他
  dubai: [55.27, 25.2], 'abu dhabi': [54.37, 24.45], doha: [51.53, 25.29],
  'panama city': [-79.53, 8.98],
  riyadh: [46.68, 24.71], mumbai: [72.88, 19.08], delhi: [77.21, 28.61],
  chennai: [80.27, 13.08], istanbul: [28.98, 41.01], moscow: [37.62, 55.75],
  'sao paulo': [-46.63, -23.55], 'rio de janeiro': [-43.17, -22.91],
  'buenos aires': [-58.38, -34.6], santiago: [-70.65, -33.45], lima: [-77.04, -12.05],
  bogota: [-74.07, 4.71], johannesburg: [28.05, -26.2], 'cape town': [18.42, -33.92],
  cairo: [31.24, 30.04], lagos: [3.39, 6.52], nairobi: [36.82, -1.29]
}

// 英文国家坐标（城市匹配失败时兜底）
const EN_COUNTRY_COORD = {
  'united states': [-98.0, 39.0], usa: [-98.0, 39.0], us: [-98.0, 39.0],
  china: [104.0, 35.0], taiwan: [121.0, 23.7],
  germany: [10.0, 51.0], 'united kingdom': [-3.0, 55.0], uk: [-3.0, 55.0],
  'great britain': [-3.0, 55.0], france: [2.0, 46.0], netherlands: [5.5, 52.1],
  japan: [138.0, 36.0], 'south korea': [127.8, 36.0], korea: [127.8, 36.0],
  singapore: [103.8, 1.3], australia: [133.0, -25.0], canada: [-106.0, 56.0],
  mexico: [-102.0, 23.0], brazil: [-51.0, -14.0], india: [78.0, 21.0],
  vietnam: [106.0, 16.0], thailand: [101.0, 15.0], malaysia: [102.0, 4.0],
  indonesia: [113.0, -1.0], 'united arab emirates': [54.0, 24.0], uae: [54.0, 24.0],
  italy: [12.5, 42.8], spain: [-3.7, 40.3], poland: [19.1, 52.1], turkey: [35.2, 39.0],
  russia: [100.0, 60.0], philippines: [122.0, 12.0], 'new zealand': [172.0, -41.0],
  'south africa': [24.0, -29.0], egypt: [30.0, 27.0], 'saudi arabia': [45.0, 24.0],
  israel: [35.0, 31.5], qatar: [51.2, 25.3], chile: [-71.0, -33.0],
  argentina: [-64.0, -34.0], colombia: [-74.0, 4.0], peru: [-75.0, -9.0],
  sweden: [15.0, 62.0], belgium: [4.5, 50.6], austria: [14.5, 47.5],
  switzerland: [8.2, 46.8], 'czech republic': [15.5, 49.8], portugal: [-8.0, 39.5],
  greece: [22.0, 39.0], nigeria: [8.0, 9.0], kenya: [37.0, 0.0]
}

// 美国州中心坐标（"City, ST" 里的城市未收录时兜底）
const US_STATE_COORD = {
  AL: [-86.8, 32.8], AK: [-152.0, 63.6], AZ: [-111.7, 34.3], AR: [-92.4, 34.9],
  CA: [-119.4, 37.2], CO: [-105.5, 39.0], CT: [-72.7, 41.6], DE: [-75.5, 39.0],
  FL: [-81.7, 27.8], GA: [-83.4, 32.6], HI: [-155.5, 19.6], ID: [-114.5, 44.4],
  IL: [-89.2, 40.0], IN: [-86.3, 39.8], IA: [-93.5, 42.0], KS: [-98.4, 38.5],
  KY: [-85.7, 37.5], LA: [-92.0, 31.0], ME: [-69.2, 45.3], MD: [-76.8, 39.0],
  MA: [-71.8, 42.3], MI: [-85.4, 44.3], MN: [-94.3, 46.3], MS: [-89.7, 32.7],
  MO: [-92.5, 38.4], MT: [-109.6, 47.0], NE: [-99.8, 41.5], NV: [-116.6, 39.3],
  NH: [-71.6, 43.7], NJ: [-74.7, 40.1], NM: [-106.1, 34.4], NY: [-75.5, 43.0],
  NC: [-79.4, 35.6], ND: [-100.5, 47.4], OH: [-82.8, 40.3], OK: [-97.5, 35.6],
  OR: [-120.6, 43.9], PA: [-77.8, 40.9], RI: [-71.5, 41.7], SC: [-80.9, 33.9],
  SD: [-100.2, 44.4], TN: [-86.3, 35.9], TX: [-99.3, 31.5], UT: [-111.7, 39.3],
  VT: [-72.7, 44.1], VA: [-78.8, 37.5], WA: [-120.4, 47.4], WV: [-80.6, 38.6],
  WI: [-89.7, 44.6], WY: [-107.6, 43.1], DC: [-77.0, 38.9]
}

/**
 * 取城市/港口/国家的经纬度
 * @param {string} name 城市或港口名
 * @param {string} country 国家名（可选，作为兜底）
 * @returns {[number, number]|null}
 */
export const getCoord = (name, country) => {
  if (name) {
    if (PORT_COORD[name]) return PORT_COORD[name]
    if (CITY_COORD[name]) return CITY_COORD[name]
    // 港口名常带"港/机场"后缀，去掉后再试一次
    const stripped = name.replace(/(港|机场|口岸)$/, '')
    if (CITY_COORD[stripped]) return CITY_COORD[stripped]
  }
  if (country && COUNTRY_COORD[country]) return COUNTRY_COORD[country]
  return null
}

/**
 * 解析第三方物流接口返回的真实地点（中英文混排，如 "Los Angeles, CA, United States"）
 * 依次尝试：完整串 → 各分段（城市 → 国家 → 美国州）
 * @param {string} location 地点文本
 * @returns {[number, number]|null}
 */
export const getCoordSmart = (location) => {
  if (!location) return null
  const raw = String(location).trim()
  if (!raw) return null
  const parts = raw.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
  const candidates = parts.length > 1 ? [raw, ...parts] : [raw]
  for (const item of candidates) {
    const cn = getCoord(item)
    if (cn) return cn
    const key = item.toLowerCase().replace(/\./g, '').replace(/\s+/g, ' ').trim()
    if (EN_CITY_COORD[key]) return EN_CITY_COORD[key]
    if (EN_COUNTRY_COORD[key]) return EN_COUNTRY_COORD[key]
    const state = key.toUpperCase()
    if (state.length === 2 && US_STATE_COORD[state]) return US_STATE_COORD[state]
  }
  return null
}
