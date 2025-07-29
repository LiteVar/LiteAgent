const themeConfig = {
  token: {
    // 主色 - 使用更柔和的蓝色
    colorPrimary: 'rgba(42, 130, 228, 1)',
    
    // 链接颜色 - 与主色保持一致性
    colorLink: 'rgba(42, 130, 228, 1)',
    
    // 成功颜色 - 更清新的绿色
    colorSuccess: '#52C41A',
    
    // 警告颜色 - 更协调的橙色
    colorWarning: '#FAAD14',
    
    // 错误颜色 - 柔和的红色
    colorError: '#F5222D',
    
    // 信息颜色 - 轻度蓝色，与主色形成层次
    colorInfo: '#1890FF',
    
    // 全局字体 - 更现代的无衬线字体组合
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    
    // 基础字体大小 - 保持14px的可读性
    fontSize: 14,
    
    // 行高 - 调整为1.6提高可读性
    lineHeight: 1.6,
    
    // 圆角半径 - 增加到6px使界面更柔和
    borderRadius: 6,
    
    // 去除按钮阴影
    btnShadow: 'none',
    
    // 组件间距 - 优化间距层级
    margin: 16,
    marginXS: 8,
    marginSM: 12,
    marginMD: 16,
    marginLG: 24,
    marginXL: 32,
    
    // 按钮点击时的颜色 - 与主色形成协调对比
    colorPrimaryActive: '#3A70C9',
    
    // 按钮悬停时的颜色 - 轻微提亮主色
    colorPrimaryHover: '#66A0FF',
    
    // 新增：文本颜色 - 使用深灰而非纯黑，减轻视觉疲劳
    colorText: '#262626',
    
    // 新增：次要文本颜色
    colorTextSecondary: '#595959',
    
    // 新增：禁用状态颜色
    colorTextDisabled: '#BFBFBF',
    
    // 新增：边框颜色
    colorBorder: '#E8E8E8',
    
    // 新增：背景色
    colorBgContainer: '#FFFFFF',
    
    // 新增：表单控件背景色
    colorFillSecondary: '#F5F5F5',
    
    // 新增：控制阴影效果
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
  },
  components: {
    Input: {
      // 修改输入框样式
      border: 'none',
      paddingBlock: 8, // 对应 0.5rem
      colorBgContainer: 'rgba(245, 245, 245, 1)',
    },
    InputNumber: {
      paddingBlock: 8, 
      border: 'none',
      colorBgContainer: 'rgba(245, 245, 245, 1)',
    },
    TextArea: {
      paddingBlock: 8,
      border: 'none',
      colorBgContainer: 'rgba(100, 245, 245, 1)',
    },
    Select: {
      // 修改选择器样式
      controlHeight: 40,
      colorBgContainer: 'rgba(245, 245, 245, 1)',
    },
    Slider: {
      colorPrimary: 'rgba(42, 130, 228, 1)',
      colorPrimaryActive: 'rgba(42, 130, 228, 1)',
      colorPrimaryHover: 'rgba(42, 130, 228, 1)',
      colorPrimaryFocus: 'rgba(42, 130, 228, 1)',
      colorPrimaryOutline: 'rgba(42, 130, 228, 1)',
      colorPrimaryBorder: 'rgba(42, 130, 228, 1)',
      colorPrimaryBorderHover: 'rgba(42, 130, 228, 1)',
      colorPrimaryBorderActive: 'rgba(42, 130, 228, 1)',
      colorPrimaryBorderFocus: 'rgba(42, 130, 228, 1)',
    },
    
  },
};

export default themeConfig;