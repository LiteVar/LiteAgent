import React, { useEffect, useRef } from 'react';
import './index.css';

const RecordWave: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    // 设置canvas尺寸
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
    
    // 点和线参数
    const dotRadius = 2; // 小圆点半径
    const dotSpacing = 6; // 点间距（中心到中心）
    const lineWidth = 2; // 竖线宽度
    const maxLineHeight = canvas.height * 0.7; // 最大竖线高度
    
    // 计算可以放置的点数量
    const dotCount = Math.floor(canvas.width / dotSpacing);
    
    // 生成随机高度数组（模拟音频波形）
    const generateHeights = () => {
      const heights = [];
      for (let i = 0; i < dotCount; i++) {
        // 生成随机高度，但保持相邻竖线高度变化平滑
        if (i > 0) {
          const prevHeight = heights[i-1];
          const variance = maxLineHeight * 0.2; // 相邻竖线高度变化范围
          const minHeight = Math.max(maxLineHeight * 0.3, prevHeight - variance);
          const maxHeight = Math.min(maxLineHeight, prevHeight + variance);
          heights.push(minHeight + Math.random() * (maxHeight - minHeight));
        } else {
          heights.push(maxLineHeight * 0.5 + Math.random() * (maxLineHeight * 0.3));
        }
      }
      return heights;
    };
    
    // 初始化高度数组
    let heights = generateHeights();
    
    // 动画参数
    let animationPosition = -1; // 动画当前位置（-1表示还未开始）
    const animationSpeed = 0.4; // 每帧移动的点数量
    const activeLineCount = 10; // 同时激活的点数量
    
    // 绘制函数
    const draw = () => {
      // 清空画布
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // 更新动画位置
      if (animationPosition === -1) {
        // 初始化动画位置
        animationPosition = dotCount;
      } else {
        // 移动动画位置
        animationPosition -= animationSpeed;
        if (animationPosition < -activeLineCount) {
          // 重置动画位置
          animationPosition = dotCount;
          // 重新生成高度
          heights = generateHeights();
        }
      }
      
      // 绘制点和线
      const startX = (canvas.width - (dotCount * dotSpacing)) / 2;
      for (let i = 0; i < dotCount; i++) {
        const x = startX + i * dotSpacing;
        const distance = i - animationPosition;
        
        if (distance >= 0 && distance < activeLineCount) {
          // 在动画范围内的点显示为蓝色竖线
          const height = heights[i];
          const y = (canvas.height - height) / 2;
          
          ctx.beginPath();
          ctx.rect(x - lineWidth/2, y, lineWidth, height);
          ctx.fillStyle = '#4096ff';
          ctx.fill();
        } else {
          // 在动画范围外的点显示为灰色圆点
          ctx.beginPath();
          ctx.arc(x, canvas.height / 2, dotRadius, 0, Math.PI * 2);
          ctx.fillStyle = '#bbbbbb';
          ctx.fill();
        }
      }
      
      requestAnimationFrame(draw);
    };
    
    // 开始动画
    draw();
    
    // 窗口大小变化时重新调整canvas尺寸
    const handleResize = () => {
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    };
    
    window.addEventListener('resize', handleResize);
    
    // 清理函数
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);
  
  return (
    <div className="wave-effect-container">
      <canvas ref={canvasRef} className="wave-effect-canvas" />
    </div>
  );
};

export default RecordWave;