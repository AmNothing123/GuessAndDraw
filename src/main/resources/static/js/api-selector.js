/**
 * API选择器脚本
 * 允许用户选择使用百度API或豆包API进行图像识别
 */

document.addEventListener('DOMContentLoaded', function() {
    // 获取AI猜测按钮元素
    const guessButton = document.getElementById('guessButton');
    const resultArea = document.getElementById('resultArea');
    const loadingIndicator = document.getElementById('loadingIndicator');
    
    // 默认使用百度API
    let currentApi = 'baidu';
    
    // 创建API选择器UI
    function createApiSelector() {
        // 创建API选择器容器
        const apiSelectorContainer = document.createElement('div');
        apiSelectorContainer.className = 'api-selector';
        
        // 创建选择器标签
        const selectorLabel = document.createElement('span');
        selectorLabel.textContent = 'AI引擎:';
        apiSelectorContainer.appendChild(selectorLabel);
        
        // 创建选择器
        const apiSelector = document.createElement('select');
        apiSelector.id = 'apiSelector';
        
        // 添加选项
        const baiduOption = document.createElement('option');
        baiduOption.value = 'baidu';
        baiduOption.textContent = '百度AI';
        apiSelector.appendChild(baiduOption);
        
        const doubanOption = document.createElement('option');
        doubanOption.value = 'doubao';
        doubanOption.textContent = '豆包AI';
        apiSelector.appendChild(doubanOption);
        
        apiSelectorContainer.appendChild(apiSelector);
        
        // 将选择器添加到工具栏
        const toolsContainer = document.querySelector('.tools');
        toolsContainer.appendChild(apiSelectorContainer);
        
        // 添加选择器变更事件
        apiSelector.addEventListener('change', function(e) {
            currentApi = e.target.value;
            console.log('已切换到', currentApi === 'baidu' ? '百度AI' : '豆包AI');
        });
    }


    // 创建API选择器
    createApiSelector();
    
    // 重写aiGuess函数，根据选择的API发送请求
    window.aiGuess = function() {
        loadingIndicator.classList.remove('hidden');
        resultArea.innerHTML = '';

        // 获取画布数据
        const canvas = document.getElementById('drawingCanvas');
        const imageData = canvas.toDataURL('image/png');
        
        // 根据选择的API确定请求URL
        const apiUrl = currentApi === 'baidu' 
            ? '/api/drawing/recognize' 
            : '/api/drawing/doubao/recognize';

        // 发送到后端API
        fetch(apiUrl, {
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
                const confidence = data.confidence;
                const apiName = currentApi === 'baidu' ? '百度AI' : '豆包AI';

                resultArea.innerHTML = `
                    <div class="prediction">
                        <p>${apiName}猜测这是一个: ${prediction}</p>
                        <p>可信度: ${confidence}%</p>
                        <div class="confidence-bar">
                            <div class="confidence-level" style="width: ${confidence}%"></div>
                        </div>
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
    };
});