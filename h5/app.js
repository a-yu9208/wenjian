// ========== 模拟数据 ==========
const MOCK = {
  shopName: '月月烧烤',
  table: { area: '室外', number: '8' },
  user: { name: '微信用户', points: 120 },
  pointsRate: { earn: 1, deduct: 10 }, // 消费1元得1分, 10分抵1元
  utensilFee: 1,
  categories: ['烧烤','火锅','饺子','凉菜','酒水','套餐'],
  dishes: [
    { id:1, name:'招牌羊肉串', cat:'烧烤', price:4, img:'', desc:'秘制调料', stock:96, min:5, quick:false, discount:0 },
    { id:2, name:'蒜香鸡翅', cat:'烧烤', price:12, img:'', desc:'外焦里嫩', stock:32, min:1, quick:false, discount:0 },
    { id:3, name:'烤茄子', cat:'烧烤', price:18, img:'', desc:'蒜蓉烤茄子', stock:15, min:1, quick:false, discount:0 },
    { id:4, name:'麻辣牛肉锅', cat:'火锅', price:88, img:'', desc:'招牌锅底', stock:10, min:1, quick:false, discount:0 },
    { id:5, name:'猪肉白菜饺', cat:'饺子', price:22, img:'', desc:'手工现包', stock:30, min:1, quick:false, discount:0 },
    { id:6, name:'花生米', cat:'凉菜', price:8, img:'', desc:'油炸花生', stock:50, min:1, quick:true, discount:0 },
    { id:7, name:'啤酒', cat:'酒水', price:8, img:'', desc:'冰镇', stock:100, min:1, quick:true, discount:0 },
    { id:8, name:'酸梅汤', cat:'酒水', price:10, img:'', desc:'自制酸梅汤', stock:41, min:1, quick:true, discount:0 },
    { id:9, name:'双人套餐', cat:'套餐', price:128, img:'', desc:'羊肉串x10+鸡翅x2+啤酒x2', stock:8, min:1, quick:false, discount:0 },
    { id:10,name:'烤鸡翅', cat:'烧烤', price:10, img:'', desc:'香辣烤翅', stock:40, min:1, quick:false, discount:8 },
  ],
  orders: [] // 当前桌的订单
};

// ========== 状态 ==========
let cart = {}; // { dishId: { qty, note } }
let notes = {}; // { dishId: noteText }
let currentPage = 'welcome';
let usePoints = false;
let currentTableKey = '';  // 当前桌的 localStorage key
let serverOrderIds = [];
let pollTimer = null;

// ========== 工具函数 ==========
const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);

function showToast(msg) {
  const t = document.createElement('div');
  t.textContent = msg;
  t.style.cssText = 'position:fixed;top:20%;left:50%;transform:translateX(-50%);background:rgba(0,0,0,.7);color:#fff;padding:10px 24px;border-radius:20px;z-index:99;font-size:14px';
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 1800);
}

function show(pageId) {
  $$('.page').forEach(p => p.classList.remove('active'));
  const el = $(`#${pageId}`);
  if (el) el.classList.add('active');
  currentPage = pageId;
}

function getCartCount() {
  return Object.values(cart).reduce((s, v) => s + v.qty, 0);
}

function getCartTotal() {
  let total = 0;
  for (const [id, v] of Object.entries(cart)) {
    const d = MOCK.dishes.find(x => x.id == id);
    if (d) total += (d.discount || d.price) * v.qty;
  }
  return total;
}

function getDisplayPrice(d) {
  if (d.discount && d.discount < d.price) {
    return `<span class="orig">¥${d.price}</span>¥${d.discount}`;
  }
  return `¥${d.price}`;
}

function changeQty(id, delta) {
  const d = MOCK.dishes.find(x => x.id == id);
  if (!d) return;
  const cur = cart[id]?.qty || 0;
  let next = cur + delta;

  if (delta > 0 && cur === 0) {
    next = Math.max(d.min, 1);
    if (next > d.stock) return;
    if (d.min > 1) showToast(`${d.name} ${d.min}份起点`);
  }
  if (delta < 0 && next > 0 && next < d.min) {
    next = 0;
  }
  if (next > d.stock) next = d.stock;

  if (next <= 0) {
    delete cart[id];
  } else {
    cart[id] = cart[id] || { qty: 0, note: '' };
    cart[id].qty = next;
  }
  renderMenu();
  renderCartBar();
}

// ========== 渲染函数 ==========
function renderMenu() {
  const list = $('.dish-list');
  if (!list) return;
  let html = '';
  MOCK.categories.forEach(cat => {
    const items = MOCK.dishes.filter(d => d.cat === cat);
    if (!items.length) return;
    html += `<div class="cat-header" id="cat-${cat}">${cat}</div>`;
    items.forEach(d => {
      const qty = cart[d.id]?.qty || 0;
      const soldout = d.stock <= 0;
      html += `<div class="dish-card">
        <div class="dish-img">${d.img ? `<img src="${d.img}">` : '🍖'}</div>
        <div class="dish-info">
          <div>
            <div class="dish-name">${d.name}</div>
            <div class="dish-desc">${d.desc}</div>
            ${d.stock <= 10 && d.stock > 0 ? `<div class="dish-stock">仅剩${d.stock}份</div>` : ''}
            ${d.min > 1 ? `<div class="dish-min">${d.min}份起点</div>` : ''}
          </div>
          <div class="dish-bottom">
            <div class="dish-price">${getDisplayPrice(d)}</div>
            ${soldout ? '<div class="dish-soldout">已售罄</div>' : `
            <div class="qty-ctrl">
              ${qty > 0 ? `<button class="qty-btn minus" onclick="changeQty(${d.id},-1)">−</button>
              <span class="qty-num">${qty}</span>` : ''}
              <button class="qty-btn plus" onclick="changeQty(${d.id},1)">+</button>
            </div>`}
          </div>
        </div>
      </div>`;
    });
  });
  list.innerHTML = html;
}

function renderCatList() {
  const el = $('.cat-list');
  if (!el) return;
  el.innerHTML = MOCK.categories.map((c, i) =>
    `<div class="cat-item${i === 0 ? ' active' : ''}" onclick="scrollToCat('${c}')">${c}</div>`
  ).join('');
}

function scrollToCat(cat) {
  $$('.cat-item').forEach(el => el.classList.toggle('active', el.textContent === cat));
  const target = $(`#cat-${cat}`);
  if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function renderCartBar() {
  const bar = $('.cart-bar');
  if (!bar) return;
  const count = getCartCount();
  if (count === 0) { bar.classList.add('hidden'); return; }
  bar.classList.remove('hidden');
  bar.innerHTML = `
    <div class="cart-info">已选 <span class="count">${count}</span> 件<span class="total">¥${getCartTotal()}</span></div>
    <button class="cart-btn" onclick="goConfirm()">去下单</button>`;
}

function goConfirm() {
  if (getCartCount() === 0) return;
  const wrap = $('#confirm');
  let html = '<div class="topbar"><button class="me-btn" onclick="show(\'menu\')">← 返回</button><span class="title">确认订单</span><span></span></div><div class="confirm-page">';
  for (const [id, v] of Object.entries(cart)) {
    const d = MOCK.dishes.find(x => x.id == id);
    if (!d) continue;
    const price = (d.discount || d.price) * v.qty;
    html += `<div class="confirm-item">
      <div class="row"><span class="name">${d.name} x${v.qty}</span><span>¥${price}</span></div>
      <input class="note-input" placeholder="备注（如：不要辣）" value="${notes[id] || ''}" onchange="notes[${id}]=this.value">
    </div>`;
  }
  html += `<div class="confirm-total">合计：¥${getCartTotal()}</div>`;
  html += `<button class="confirm-submit" onclick="submitOrder()">提交订单</button></div>`;
  wrap.innerHTML = html;
  show('confirm');
}

async function submitOrder() {
  // 找到当前桌台ID
  const dishes = [];
  const apiItems = [];
  for (const [id, v] of Object.entries(cart)) {
    const d = MOCK.dishes.find(x => x.id == id);
    if (!d) continue;
    dishes.push({ name: d.name, qty: v.qty, note: notes[id] || '', served: false, quick: d.quick });
    apiItems.push({ dishId: d.id, quantity: v.qty });
    d.stock = Math.max(0, d.stock - v.qty);
  }
  const isAppend = MOCK.orders.length > 0;
  MOCK.orders.push({
    time: new Date().toTimeString().slice(0, 5),
    dishes,
    status: '等待中',
    isAppend
  });

  // 提交到后端
  if (MOCK.tableId) {
    try {
      const res = await fetch(`${API_BASE}/customer/order`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tableId: MOCK.tableId, items: apiItems })
      });
      const json = await res.json();
      if (json.success && json.data) {
        serverOrderIds.push(json.data.orderId);
        localStorage.setItem(currentTableKey, JSON.stringify(serverOrderIds));
      } else {
        showToast('下单失败: ' + (json.error || json.msg || '未知错误'));
        console.error('submitOrder fail:', json);
        return;
      }
    } catch (e) {
      showToast('网络错误: ' + e.message);
      console.error('submitOrder error:', e);
      return;
    }
  } else {
    showToast('桌台信息缺失，无法下单');
    return;
  }

  cart = {};
  notes = {};
  renderOrderPage();
  show('order');
  showToast('下单成功！');
  startOrderPoll();
  // 首单时自动将桌台状态更新为使用中
  if (!isAppend && MOCK.tableId) {
    fetch(`${API_BASE}/merchant/table-status`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tableId: MOCK.tableId, status: 'occupied' })
    }).catch(() => {});
  }
  // 显示菜单页的查看订单按钮
  const fb = document.getElementById('orderFloatBtn');
  if (fb) fb.classList.remove('hidden');
}

async function restoreOrders() {
  for (const oid of serverOrderIds) {
    try {
      const res = await fetch(`${API_BASE}/customer/order-status?orderId=${oid}`);
      const json = await res.json();
      if (json.success && json.data) {
        const d = json.data;
        if (!MOCK.orders.find(o => o.serverId === oid)) {
          MOCK.orders.push({
            serverId: oid,
            time: '',
            dishes: (d.items || []).map(i => ({ name: i.name, qty: i.quantity, note: '', served: false, quick: false })),
            status: d.status,
            isAppend: MOCK.orders.length > 0
          });
        }
      }
    } catch (e) {}
  }
}

function startOrderPoll() {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    if (currentPage !== 'order') return;
    for (let i = 0; i < serverOrderIds.length; i++) {
      try {
        const res = await fetch(`${API_BASE}/customer/order-status?orderId=${serverOrderIds[i]}`);
        const json = await res.json();
        if (json.success && json.data && MOCK.orders[i]) {
          MOCK.orders[i].status = json.data.status;
        }
      } catch (e) {}
    }
    renderOrderPage();
  }, 5000);
}

function renderOrderPage() {
  const wrap = $('#order');
  let html = '<div class="topbar"><button class="me-btn" onclick="goMenuFromOrder()">← 菜单</button><span class="title">订单状态</span><span class="info">' +
    MOCK.table.area + '-' + MOCK.table.number + '号桌</span></div><div class="order-page">';
  MOCK.orders.forEach((o, i) => {
    const label = o.isAppend ? `追加单 ${o.time}` : `第${i + 1}单 ${o.time}`;
    const statusBadge = `<span class="order-status-badge">${o.status}</span>`;
    html += `<div class="order-section"><div class="label">${label} ${statusBadge}</div>`;
    o.dishes.forEach(d => {
      const statusText = d.served ? '已上菜' : (o.status === '制作中' ? '制作中' : o.status === '等待中' ? '等待中' : '制作中');
      const cls = d.served ? 'served' : 'cooking';
      html += `<div class="order-dish"><span>${d.name} x${d.qty}${d.note ? ' ('+d.note+')' : ''}</span><span class="${cls}">${statusText}</span></div>`;
    });
    html += '</div>';
  });
  html += `<div class="order-actions">
    <button class="btn-secondary" onclick="goMenuFromOrder()">继续加菜</button>
    <button class="btn-primary" onclick="goBill()">申请结账</button>
  </div></div>`;
  wrap.innerHTML = html;
}

function goMenuFromOrder() {
  show('menu');
  renderMenu();
  renderCartBar();
}

function goBill() {
  const wrap = $('#bill');
  let subtotal = 0;
  let html = '<div class="topbar"><button class="me-btn" onclick="renderOrderPage();show(\'order\')">← 返回</button><span class="title">账单明细</span><span></span></div><div class="bill-page">';
  MOCK.orders.forEach(o => {
    o.dishes.forEach(d => {
      const dish = MOCK.dishes.find(x => x.name === d.name);
      const price = dish ? (dish.discount || dish.price) * d.qty : 0;
      subtotal += price;
      html += `<div class="bill-row"><span>${d.name} x${d.qty}</span><span>¥${price}</span></div>`;
    });
  });
  const utensils = MOCK.orders.length > 0 ? 2 : 0;
  const utensilTotal = utensils * MOCK.utensilFee;
  html += `<div class="bill-row"><span>餐具费 x${utensils}</span><span>¥${utensilTotal}</span></div>`;
  // 手机号输入 + 查询积分
  html += `<div class="phone-input-wrap">
    <input type="tel" id="checkoutPhone" class="phone-input" placeholder="输入手机号查询积分并获得积分（选填）" maxlength="11"
      value="${localStorage.getItem('userPhone') || ''}" oninput="onPhoneInput(this.value)">
  </div>`;
  html += `<div id="pointsSection"></div>`;
  const total = subtotal + utensilTotal;
  html += `<div class="bill-row total" id="billTotal"><span>应付</span><span>¥${total}</span></div>`;
  html += `<div class="bill-notice">请找服务员付款</div>`;
  html += `<button class="bill-submit" onclick="notifyMerchant()">确认结账</button></div>`;
  wrap.innerHTML = html;
  show('bill');
  // 保存小计供积分计算用
  MOCK._billSubtotal = subtotal + utensilTotal;
  MOCK._billPoints = 0;
  usePoints = false;
  // 如果已有手机号，自动查询积分
  const saved = localStorage.getItem('userPhone');
  if (saved && /^1\d{10}$/.test(saved)) fetchBillPoints(saved);
}

let _phoneTimer = null;
function onPhoneInput(val) {
  clearTimeout(_phoneTimer);
  const phone = val.trim();
  if (/^1\d{10}$/.test(phone)) {
    _phoneTimer = setTimeout(() => fetchBillPoints(phone), 300);
  } else {
    // 手机号不完整，清空积分区域
    const sec = document.getElementById('pointsSection');
    if (sec) sec.innerHTML = '';
    MOCK._billPoints = 0;
    usePoints = false;
    updateBillTotal();
  }
}

async function fetchBillPoints(phone) {
  const sec = document.getElementById('pointsSection');
  if (!sec) return;
  sec.innerHTML = '<div style="color:var(--text2);font-size:13px;padding:8px 0">查询积分中...</div>';
  try {
    const res = await fetch(`${API_BASE}/customer/points?phone=${encodeURIComponent(phone)}`);
    const json = await res.json();
    if (json.success && json.data) {
      const pts = json.data.points || 0;
      MOCK._billPoints = pts;
      MOCK.user.points = pts;
      const maxDeduct = Math.floor(pts / MOCK.pointsRate.deduct);
      if (pts > 0) {
        sec.innerHTML = `<div class="points-toggle">
          <input type="checkbox" id="usePoints" ${usePoints ? 'checked' : ''} onchange="usePoints=this.checked;updateBillTotal()">
          <label for="usePoints">使用${pts}积分抵扣¥${maxDeduct}</label>
        </div>`;
      } else {
        sec.innerHTML = '<div style="color:var(--text2);font-size:13px;padding:8px 0">该手机号暂无可用积分</div>';
      }
      updateBillTotal();
    } else {
      sec.innerHTML = '<div style="color:var(--text2);font-size:13px;padding:8px 0">积分查询失败</div>';
    }
  } catch (e) {
    sec.innerHTML = '<div style="color:var(--text2);font-size:13px;padding:8px 0">积分查询失败</div>';
  }
}

function updateBillTotal() {
  const el = document.getElementById('billTotal');
  if (!el) return;
  const base = MOCK._billSubtotal || 0;
  const pts = MOCK._billPoints || 0;
  const maxDeduct = Math.floor(pts / MOCK.pointsRate.deduct);
  const deduct = usePoints ? maxDeduct : 0;
  el.innerHTML = `<span>应付</span><span>¥${base - deduct}</span>`;
}

async function notifyMerchant() {
  const phoneEl = document.getElementById('checkoutPhone');
  const phone = phoneEl ? phoneEl.value.trim() : '';
  if (phone && !/^1\d{10}$/.test(phone)) { showToast('手机号格式不对'); return; }
  if (phone) localStorage.setItem('userPhone', phone);

  // 调后端结账
  for (const oid of serverOrderIds) {
    try {
      const res = await fetch(`${API_BASE}/customer/checkout?orderId=${oid}&phone=${encodeURIComponent(phone)}`, { method: 'POST' });
      const json = await res.json();
      if (json.success && json.data && json.data.pointsEarned > 0) {
        showToast(`获得 ${json.data.pointsEarned} 积分，总积分 ${json.data.totalPoints}`);
        MOCK.user.points = json.data.totalPoints;
        if (phone) MOCK.user.phone = phone;
      }
    } catch (e) {}
  }

  if (!serverOrderIds.length) showToast('已通知商家，请等待服务员收款');
  const checkoutIds = [...serverOrderIds];
  serverOrderIds = [];
  localStorage.removeItem(currentTableKey);
  MOCK.orders = [];
  show('welcome');
  const app = $('#welcome');
  app.innerHTML = `<h1>🔥 ${MOCK.shopName}</h1><div class="sub">${MOCK.table.area} ${MOCK.table.number}号桌</div><div class="sub">等待商家确认收款...</div>`;
  pollCheckoutStatus(checkoutIds);
}

function pollCheckoutStatus(orderIds) {
  if (!orderIds.length) return;
  const timer = setInterval(async () => {
    try {
      const res = await fetch(`${API_BASE}/customer/order-status?orderId=${orderIds[0]}`);
      const json = await res.json();
      if (json.success && json.data && json.data.isPaid) {
        clearInterval(timer);
        const app = $('#welcome');
        app.innerHTML = `<h1>🔥 ${MOCK.shopName}</h1><div class="sub">${MOCK.table.area} ${MOCK.table.number}号桌</div>
          <div class="sub" style="color:#2e7d32;font-size:18px;margin:20px 0">✅ 支付完成，感谢光临！</div>
          <div class="sub" style="color:var(--text2)">页面将自动关闭</div>`;
        setTimeout(() => {
          if (window.WeixinJSBridge) {
            window.WeixinJSBridge.call('closeWindow');
          } else {
            app.innerHTML = `<h1>🔥 ${MOCK.shopName}</h1><div class="sub">欢迎下次光临！</div>`;
          }
        }, 3000);
      }
    } catch (e) {}
  }, 3000);
}

function goMe() {
  const wrap = $('#me');
  const u = MOCK.user;
  const hasOrders = MOCK.orders.length > 0 || serverOrderIds.length > 0;
  wrap.innerHTML = `<div class="topbar"><button class="me-btn" onclick="show('menu')">← 返回</button><span class="title">我的</span><span></span></div>
  <div class="me-page">
    <div class="me-header"><div class="me-avatar">${u.avatar ? `<img src="${u.avatar}" style="width:64px;height:64px;border-radius:50%">` : '👤'}</div><div class="me-name">${u.name}</div><div class="me-points">积分：${u.points}</div></div>
    ${hasOrders ? '<div class="me-action"><button class="btn-primary" onclick="renderOrderPage();show(\'order\');startOrderPoll()">查看本桌订单</button></div>' : ''}
    <div class="me-section"><h3>积分明细</h3><div id="points-log">加载中...</div></div>
  </div>`;
  show('me');
  loadPointsLog();
}

async function loadPointsLog() {
  const el = document.getElementById('points-log');
  if (!el) return;
  const phone = localStorage.getItem('userPhone');
  if (!phone) {
    el.innerHTML = '<div class="me-row" style="color:var(--text2)">结账时填写手机号即可累积积分</div>';
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/customer/points?phone=${encodeURIComponent(phone)}`);
    const json = await res.json();
    if (json.success && json.data) {
      MOCK.user.points = json.data.points;
      MOCK.user.phone = phone;
      // 更新积分显示
      const pEl = document.querySelector('.me-points');
      if (pEl) pEl.textContent = `积分：${json.data.points}`;
      if (json.data.logs.length === 0) {
        el.innerHTML = '<div class="me-row" style="color:var(--text2)">暂无积分记录</div>';
      } else {
        el.innerHTML = json.data.logs.map(l =>
          `<div class="me-row"><span>${l.reason}</span><span style="color:${l.delta > 0 ? 'var(--green)' : 'var(--red)'}">${l.delta > 0 ? '+' : ''}${l.delta}</span></div>`
        ).join('') + `<div class="me-row" style="color:var(--text2);font-size:12px">手机号：${phone}</div>`;
      }
    }
  } catch (e) {
    el.innerHTML = '<div class="me-row" style="color:var(--text2)">加载失败</div>';
  }
}

// ========== API ==========
const API_BASE = '';  // 同域，nginx 反代

async function fetchTableInfo(section, number) {
  try {
    const res = await fetch(`${API_BASE}/customer/table?section=${section}&number=${number}`);
    const json = await res.json();
    if (json.success && json.data) {
      if (json.data.shopName) MOCK.shopName = json.data.shopName;
      if (json.data.table) {
        MOCK.tableId = json.data.table.id;
        MOCK.table.area = { outside:'室外', first:'一楼', second:'二楼' }[json.data.table.section] || json.data.table.section;
        MOCK.table.number = String(json.data.table.number);
        MOCK.tableStatus = json.data.table.status;
      }
    }
  } catch (e) { console.error('fetchTableInfo error:', e); }
}

// ========== 初始化 ==========
async function fetchDishes() {
  try {
    const res = await fetch(`${API_BASE}/customer/dishes`);
    const json = await res.json();
    if (json.success && json.data) {
      if (json.data.categories && json.data.categories.length) MOCK.categories = json.data.categories;
      if (json.data.dishes && json.data.dishes.length) {
        MOCK.dishes = json.data.dishes.map(d => ({
          id: d.id, name: d.name, cat: d.category, price: d.price,
          img: d.imageUrl || '', desc: d.description || '',
          stock: d.stock, min: d.minOrderQty || 1, quick: false, discount: 0
        }));
      }
    }
  } catch (e) { console.error('fetchDishes error:', e); }
}

async function init() {
  const params = new URLSearchParams(location.search);
  const section = params.get('section') || 'outside';
  const number = params.get('number') || '8';
  MOCK.table.area = { outside:'室外', first:'一楼', second:'二楼' }[section] || section;
  MOCK.table.number = number;

  // 按桌号隔离订单数据
  currentTableKey = `orderIds_${section}_${number}`;
  serverOrderIds = JSON.parse(localStorage.getItem(currentTableKey) || '[]');
  // 重置本桌的本地订单
  MOCK.orders = [];

  // 尝试从后端获取店名和桌台信息
  await fetchTableInfo(section, number);

  // 桌台待结账时不允许新点单
  if (MOCK.tableStatus && ['Pending Bill', 'pending_bill', '待结账'].includes(MOCK.tableStatus) && serverOrderIds.length === 0) {
    const app = $('#app');
    app.innerHTML = `
      <div class="page welcome active" id="welcome">
        <h1>🔥 ${MOCK.shopName}</h1>
        <div class="sub">${MOCK.table.area} ${MOCK.table.number}号桌</div>
        <div class="sub" style="color:#d32f2f;margin-top:20px">该桌正在结账中，请联系服务员</div>
      </div>`;
    return;
  }

  await fetchDishes();

  // 生成或读取匿名用户ID
  let anonId = localStorage.getItem('anonId');
  if (!anonId) { anonId = 'guest_' + Date.now(); localStorage.setItem('anonId', anonId); }
  MOCK.user.anonId = anonId;

  const app = $('#app');
  app.innerHTML = `
    <div class="page welcome active" id="welcome">
      <h1>🔥 ${MOCK.shopName}</h1>
      <div class="sub">${MOCK.table.area} ${MOCK.table.number}号桌</div>
      <div class="spinner"></div>
    </div>
    <div class="page" id="menu">
      <div class="topbar">
        <div><div class="title">${MOCK.shopName}</div><div class="info">${MOCK.table.area}-${MOCK.table.number}号桌</div></div>
        <button class="me-btn" onclick="goMe()">我的</button>
      </div>
      <div class="menu-wrap"><div class="cat-list"></div><div class="dish-list"></div></div>
      <div class="order-float-btn hidden" id="orderFloatBtn" onclick="renderOrderPage();show('order');startOrderPoll()">📋 查看订单</div>
      <div class="cart-bar hidden"></div>
    </div>
    <div class="page" id="confirm"></div>
    <div class="page" id="order"></div>
    <div class="page" id="bill"></div>
    <div class="page" id="me"></div>`;

  setTimeout(async () => {
    show('menu');
    renderCatList();
    renderMenu();
    renderCartBar();
    // 有历史订单则显示查看按钮并恢复数据
    if (serverOrderIds.length > 0) {
      const fb = document.getElementById('orderFloatBtn');
      if (fb) fb.classList.remove('hidden');
      await restoreOrders();
    }
    // 滚动时高亮对应分类
    const dishList = $('.dish-list');
    if (dishList) {
      dishList.addEventListener('scroll', () => {
        const headers = dishList.querySelectorAll('.cat-header');
        let current = '';
        headers.forEach(h => {
          if (h.getBoundingClientRect().top <= 120) current = h.textContent;
        });
        if (current) {
          $$('.cat-item').forEach(el => el.classList.toggle('active', el.textContent === current));
        }
      });
    }
  }, 1500);
}

document.addEventListener('DOMContentLoaded', init);
