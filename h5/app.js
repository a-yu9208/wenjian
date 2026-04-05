// ========== 模拟数据 ==========
const MOCK = {
  shopName: '夜月烧烤',
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

// ========== 工具函数 ==========
const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);

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

function submitOrder() {
  const dishes = [];
  for (const [id, v] of Object.entries(cart)) {
    const d = MOCK.dishes.find(x => x.id == id);
    if (!d) continue;
    dishes.push({ name: d.name, qty: v.qty, note: notes[id] || '', served: false, quick: d.quick });
    d.stock = Math.max(0, d.stock - v.qty);
  }
  const isAppend = MOCK.orders.length > 0;
  MOCK.orders.push({
    time: new Date().toTimeString().slice(0, 5),
    dishes,
    status: '待处理',
    isAppend
  });
  cart = {};
  notes = {};
  renderOrderPage();
  show('order');
  // 简单的成功提示
  const toast = document.createElement('div');
  toast.textContent = '下单成功！';
  toast.style.cssText = 'position:fixed;top:20%;left:50%;transform:translateX(-50%);background:rgba(0,0,0,.7);color:#fff;padding:10px 24px;border-radius:20px;z-index:99;font-size:15px';
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 1500);
}

function renderOrderPage() {
  const wrap = $('#order');
  let html = '<div class="topbar"><button class="me-btn" onclick="goMenuFromOrder()">← 菜单</button><span class="title">订单状态</span><span class="info">' +
    MOCK.table.area + '-' + MOCK.table.number + '号桌</span></div><div class="order-page">';
  MOCK.orders.forEach((o, i) => {
    const label = o.isAppend ? `追加单 ${o.time}` : `第${i + 1}单 ${o.time}`;
    html += `<div class="order-section"><div class="label">${label}</div>`;
    o.dishes.forEach(d => {
      const cls = d.served ? 'served' : 'cooking';
      const txt = d.served ? '已上菜' : '制作中';
      html += `<div class="order-dish"><span>${d.name} x${d.qty}${d.note ? ' ('+d.note+')' : ''}</span><span class="${cls}">${txt}</span></div>`;
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
  const pointsAvail = MOCK.user.points;
  const maxDeduct = Math.floor(pointsAvail / MOCK.pointsRate.deduct);
  html += `<div class="points-toggle">
    <input type="checkbox" id="usePoints" ${usePoints ? 'checked' : ''} onchange="usePoints=this.checked;goBill()">
    <label for="usePoints">使用${pointsAvail}积分抵扣¥${maxDeduct}</label>
  </div>`;
  const deduct = usePoints ? maxDeduct : 0;
  const total = subtotal + utensilTotal - deduct;
  html += `<div class="bill-row total"><span>应付</span><span>¥${total}</span></div>`;
  html += `<div class="bill-notice">请找服务员付款</div>`;
  html += `<button class="bill-submit" onclick="notifyMerchant()">已通知商家</button></div>`;
  wrap.innerHTML = html;
  show('bill');
}

function notifyMerchant() {
  alert('已通知商家，请等待服务员前来收款');
  show('welcome');
  const app = $('#welcome');
  app.innerHTML = `<h1>🔥 ${MOCK.shopName}</h1><div class="sub">${MOCK.table.area} ${MOCK.table.number}号桌</div><div class="sub">等待商家确认收款...</div>`;
}

function goMe() {
  const wrap = $('#me');
  const u = MOCK.user;
  wrap.innerHTML = `<div class="topbar"><button class="me-btn" onclick="show('menu')">← 返回</button><span class="title">我的</span><span></span></div>
  <div class="me-page">
    <div class="me-header"><div class="me-avatar">👤</div><div class="me-name">${u.name}</div><div class="me-points">积分：${u.points}</div></div>
    <div class="me-section"><h3>积分明细</h3>
      <div class="me-row"><span>消费获得</span><span>+100</span></div>
      <div class="me-row"><span>抵扣使用</span><span>-30</span></div>
      <div class="me-row"><span>商家赠送</span><span>+50</span></div>
    </div>
  </div>`;
  show('me');
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
        MOCK.table.area = { outside:'室外', first:'一楼', second:'二楼' }[json.data.table.section] || json.data.table.section;
        MOCK.table.number = String(json.data.table.number);
      }
    }
  } catch (e) { /* 用 mock 数据兜底 */ }
}

// ========== 初始化 ==========
async function init() {
  const params = new URLSearchParams(location.search);
  const section = params.get('section') || 'outside';
  const number = params.get('number') || '8';
  MOCK.table.area = { outside:'室外', first:'一楼', second:'二楼' }[section] || section;
  MOCK.table.number = number;

  // 尝试从后端获取店名和桌台信息
  await fetchTableInfo(section, number);

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
      <div class="cart-bar hidden"></div>
    </div>
    <div class="page" id="confirm"></div>
    <div class="page" id="order"></div>
    <div class="page" id="bill"></div>
    <div class="page" id="me"></div>`;

  setTimeout(() => {
    show('menu');
    renderCatList();
    renderMenu();
    renderCartBar();
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
