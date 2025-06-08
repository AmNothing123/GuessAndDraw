//使用canvas来实现绘图功能
// 当用户在画布上绘制时，我们将这些路径和样式保存到一个数组中。
// 当用户点击“AI猜测”按钮时，我们将这些路径和样式发送到服务器，服务器将使用深度学习模型来识别图像中的物体。
// 服务器返回识别结果后，我们将结果显示在页面上。

document.addEventListener('DOMContentLoaded', function() {
    const canvas = document.getElementById('drawingCanvas');
    const ctx = canvas.getContext('2d');
    const clearButton = document.getElementById('clearButton');
    const undoButton = document.getElementById('undoButton');
    const guessButton = document.getElementById('guessButton');
    const colorPicker = document.getElementById('colorPicker');
    const lineWidth = document.getElementById('lineWidth');
    const resultArea = document.getElementById('resultArea');
    const loadingIndicator = document.getElementById('loadingIndicator');

    let isDrawing = false;
    let lastX = 0;
    let lastY = 0;
    let paths = [];
    let currentPath = [];
    let currentStyle = {};

    // 阻止触摸设备上的默认滚动行为
    canvas.addEventListener('touchstart', function(e) {
        e.preventDefault();
    }, { passive: false });
    canvas.addEventListener('touchmove', function(e) {
        e.preventDefault();
    }, { passive: false });
    canvas.addEventListener('touchend', function(e) {
        e.preventDefault();
    }, { passive: false });

    // 设置初始画笔样式
    ctx.strokeStyle = colorPicker.value;
    ctx.lineWidth = lineWidth.value;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    // 获取鼠标/触摸位置
    function getPosition(e) {
        if (e.type.includes('touch')) {
            const rect = canvas.getBoundingClientRect();
            const touch = e.touches[0] || e.changedTouches[0];
            return {
                x: touch.clientX - rect.left,
                y: touch.clientY - rect.top
            };
        } else {
            return {
                x: e.offsetX,
                y: e.offsetY
            };
        }
    }

    // 开始绘画事件
    function startDrawing(e) {
        isDrawing = true;
        const pos = getPosition(e);
        [lastX, lastY] = [pos.x, pos.y];
        currentPath = [];
        currentStyle = {
            color: ctx.strokeStyle,
            width: ctx.lineWidth
        };
        currentPath.push({x: lastX, y: lastY});
    }

    // 绘画事件
    function draw(e) {
        if (!isDrawing) return;
        const pos = getPosition(e);
        ctx.beginPath();
        ctx.moveTo(lastX, lastY);
        ctx.lineTo(pos.x, pos.y);
        ctx.stroke();
        [lastX, lastY] = [pos.x, pos.y];
        currentPath.push({x: pos.x, y: pos.y});
    }

    // 停止绘画
    function stopDrawing() {
        if (isDrawing) {
            isDrawing = false;
            if (currentPath.length > 0) {
                paths.push({
                    points: currentPath,
                    style: currentStyle
                });
            }
        }
    }

    // 清除画布
    function clearCanvas() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        paths = [];
        resultArea.innerHTML = '<p>请在左侧画布上绘制一个物体，然后点击"AI猜测"按钮。</p>';
    }

    function undoLastPath() {
        if (paths.length > 0) {
            paths.pop();
            redrawCanvas();
        }
    }

    function redrawCanvas() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        paths.forEach(path => {
            ctx.strokeStyle = path.style.color;
            ctx.lineWidth = path.style.width;
            ctx.beginPath();
            ctx.moveTo(path.points[0].x, path.points[0].y);
            for (let i = 1; i < path.points.length; i++) {
                ctx.lineTo(path.points[i].x, path.points[i].y);
            }
            ctx.stroke();
        });
        ctx.strokeStyle = colorPicker.value;
        ctx.lineWidth = lineWidth.value;
    }

    // 更新画笔颜色
    function updateColor() {
        ctx.strokeStyle = colorPicker.value;
    }

    // 更新画笔宽度
    function updateLineWidth() {
        ctx.lineWidth = lineWidth.value;
    }

    // AI猜测功能
    function aiGuess() {
        loadingIndicator.classList.remove('hidden');
        resultArea.innerHTML = '';

        // 获取画布数据
        const imageData = canvas.toDataURL('image/png');

        // 发送到后端API
        fetch('/api/drawing/doubao/recognize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                imageData: imageData
            })
        })
        .then(response => response.json())
        .then(data => {
            loadingIndicator.classList.add('hidden');

            if (data.success) {
                const prediction = data.prediction;
                const fullContent = data.fullContent || '';

                // 处理完整内容的显示，将\n转换为<br>标签
                const formattedContent = fullContent
                    .replace(/\n/g, '<br>')
                    .replace(/\s/g, '&nbsp;');

                resultArea.innerHTML = `
                    <div class="prediction">
                        <p>我猜这是一个: ${prediction}</p>
                        <div class="description">${formattedContent}</div>
                              
                    </div>
                `;
            } else {
                resultArea.innerHTML = `
                    <div class="error">
                        <p>识别失败: ${data.message || '未知错误'}</p>
                    </div>
                `;
            }
        })
        .catch(error => {
            loadingIndicator.classList.add('hidden');
            resultArea.innerHTML = `
                <div class="error">
                    <p>请求失败: ${error.message}</p>
                </div>
            `;
            console.error('识别请求失败:', error);
        });
    }

    // 添加鼠标事件监听器
    canvas.addEventListener('mousedown', startDrawing);
    canvas.addEventListener('mousemove', draw);
    canvas.addEventListener('mouseup', stopDrawing);
    canvas.addEventListener('mouseout', stopDrawing);

    // 添加触摸事件监听器
    canvas.addEventListener('touchstart', startDrawing);
    canvas.addEventListener('touchmove', draw);
    canvas.addEventListener('touchend', stopDrawing);
    canvas.addEventListener('touchcancel', stopDrawing);

    // 添加其他控件事件监听器
    clearButton.addEventListener('click', clearCanvas);
    undoButton.addEventListener('click', undoLastPath);
    guessButton.addEventListener('click', aiGuess);
    colorPicker.addEventListener('change', updateColor);
    lineWidth.addEventListener('input', updateLineWidth);
});
