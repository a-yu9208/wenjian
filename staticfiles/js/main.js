// 主 JavaScript 文件

// ========== 顶部进度条 ==========
(function() {
    var bar = document.createElement('div');
    bar.id = 'page-progress';
    bar.style.width = '0%';
    document.body.prepend(bar);

    var w = 0;
    var timer = setInterval(function() {
        w += (90 - w) * 0.08;
        bar.style.width = w + '%';
    }, 80);

    window.addEventListener('load', function() {
        clearInterval(timer);
        bar.style.width = '100%';
        setTimeout(function() {
            bar.style.opacity = '0';
            bar.style.transition = 'opacity 0.4s ease';
            setTimeout(function() { bar.remove(); }, 400);
        }, 300);
    });
})();

// ========== 滚动入场动画 ==========
(function() {
    function initScrollAnimations() {
        document.querySelectorAll('.row > [class*="col-"]').forEach(function(el) {
            if (el.querySelector('.card') || el.querySelector('.file-icon')) {
                el.classList.add('animate-on-scroll');
            }
        });
        document.querySelectorAll('main h2, main .btn-group[role="group"]').forEach(function(el) {
            el.classList.add('animate-on-scroll');
        });

        var observer = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('animated');
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.08 });

        document.querySelectorAll('.animate-on-scroll').forEach(function(el) {
            observer.observe(el);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initScrollAnimations);
    } else {
        initScrollAnimations();
    }
})();

// ========== 页面跳转进度条 ==========
document.addEventListener('click', function(e) {
    var a = e.target.closest('a[href]');
    if (!a || a.target === '_blank' || a.getAttribute('href').charAt(0) === '#') return;
    if (e.ctrlKey || e.metaKey || e.shiftKey) return;
    var bar = document.getElementById('page-progress');
    if (!bar) {
        bar = document.createElement('div');
        bar.id = 'page-progress';
        bar.style.width = '0%';
        bar.style.opacity = '1';
        document.body.prepend(bar);
    } else {
        bar.style.opacity = '1';
        bar.style.width = '0%';
    }
    setTimeout(function() { bar.style.width = '30%'; }, 10);
    setTimeout(function() { bar.style.width = '60%'; }, 200);
});

$(document).ready(function() {
    // CSRF Token 设置
    $.ajaxSetup({
        beforeSend: function(xhr, settings) {
            if (!/^(GET|HEAD|OPTIONS|TRACE)$/i.test(settings.type) && !this.crossDomain) {
                xhr.setRequestHeader("X-CSRFToken", getCookie('csrftoken'));
            }
        }
    });
    
    // 自动隐藏提示消息
    setTimeout(function() {
        $('.alert').fadeOut('slow');
    }, 5000);
    
    // 防止弹窗重复显示 - 检查所有 modal
    preventModalAutoShow();
});

// 获取 CSRF Token
function getCookie(name) {
    let cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.substring(0, name.length + 1) === (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

// 通用 AJAX 错误处理
$(document).ajaxError(function(event, xhr) {
    if (xhr.status === 403) {
        showToast('权限不足', 'error');
    } else if (xhr.status === 404) {
        showToast('资源未找到', 'error');
    } else if (xhr.status >= 500) {
        showToast('服务器错误', 'error');
    }
});

// Toast 提示函数（替代 alert）
function showToast(message, type = 'info') {
    const bgColor = {
        'success': '#28a745',
        'error': '#dc3545',
        'warning': '#ffc107',
        'info': '#17a2b8'
    }[type] || '#17a2b8';
    
    const toast = $(`
        <div class="custom-toast" style="
            position: fixed;
            top: 80px;
            right: 20px;
            background: ${bgColor};
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            z-index: 9999;
            min-width: 250px;
            animation: slideIn 0.3s ease-out;
        ">
            <i class="bi bi-${type === 'success' ? 'check-circle' : type === 'error' ? 'x-circle' : 'info-circle'}"></i>
            ${message}
        </div>
    `);
    
    $('body').append(toast);
    
    setTimeout(function() {
        toast.fadeOut(300, function() {
            $(this).remove();
        });
    }, 3000);
}

// 防止弹窗自动显示
function preventModalAutoShow() {
    // 监听所有 modal 的 show 事件
    $('.modal').on('show.bs.modal', function(e) {
        const modalId = $(this).attr('id');
        
        // 如果是公告类弹窗，检查是否已经显示过
        if ($(this).hasClass('announcement-modal') || $(this).data('auto-show')) {
            const storageKey = 'modal_shown_' + modalId;
            
            // 检查 localStorage
            if (localStorage.getItem(storageKey)) {
                e.preventDefault();
                return false;
            }
            
            // 记录已显示
            localStorage.setItem(storageKey, 'true');
        }
    });
    
    // 清除所有自动显示的弹窗（防止页面加载时自动弹出）
    $('.modal[data-auto-show="true"]').each(function() {
        const modalId = $(this).attr('id');
        const storageKey = 'modal_shown_' + modalId;
        
        // 如果已经显示过，不再显示
        if (!localStorage.getItem(storageKey)) {
            // 延迟显示，避免页面加载时立即弹出
            const modal = $(this);
            setTimeout(function() {
                modal.modal('show');
            }, 1000);
        }
    });
}

// 重置弹窗显示状态（用于测试或管理员操作）
function resetModalStatus(modalId) {
    const storageKey = 'modal_shown_' + modalId;
    localStorage.removeItem(storageKey);
    showToast('弹窗状态已重置', 'success');
}

// 添加 CSS 动画
$('<style>')
    .text(`
        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
    `)
    .appendTo('head');





