const themeConfig = {
  token: {
    // 主色 - Foundation / Blue / blue-6
    colorPrimary: '#40A5EE',
    
    // 链接颜色 - 与主色保持一致
    colorLink: '#40A5EE',
    
    // 成功颜色
    colorSuccess: '#52C41A',
    
    // 警告颜色
    colorWarning: '#FAAD14',
    
    // 错误颜色 - Foundation / Red / red-6
    colorError: '#CC2D3A',
    
    // 信息颜色
    colorInfo: '#40A5EE',
    
    // 全局字体 - 优先使用 PingFang SC
    fontFamily: '"PingFang SC", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    
    // 基础字体大小
    fontSize: 14,
    
    // 行高 - Figma 常用 1.5714 (22/14)
    lineHeight: 1.5714285714285714,
    
    // 基础圆角 - 对应 Figma 中的 radius-s (12px)
    borderRadius: 12,
    
    // 去除按钮阴影
    btnShadow: 'none',
    
    // 组件间距
    margin: 16,
    marginXS: 8,
    marginSM: 12,
    marginMD: 16,
    marginLG: 24,
    marginXL: 32,
    
    // 文本颜色 - Foundation / Grey / grey-10
    colorText: '#383F44',
    
    // 次要文本颜色 - Foundation / Grey / grey-8
    colorTextDescription: '#58636C',
    
    // 禁用状态颜色
    colorTextDisabled: '#94A0AB',
    
    // 边框颜色
    colorBorder: '#E0E3E6',
    
    // 背景色
    colorBgContainer: '#FFFFFF',
    
    // 遮罩背景色 - 适配玻璃态
    colorBgMask: 'rgba(0, 0, 0, 0.45)',
  },
  components: {
    Button: {
      borderRadius: 8, // Figma 中按钮圆角多为 8px (radius-xs)
      controlHeight: 32, // 默认中等高度
      controlHeightLG: 40,
      fontWeight: 500,
    },
    Input: {
      controlHeight: 48, // 登录/重置页输入框高度
      colorBgContainer: 'rgba(255, 255, 255, 0.6)',
      activeBorderColor: '#40A5EE',
      hoverBorderColor: '#40A5EE',
    },
    Select: {
      controlHeight: 40,
      colorBgContainer: 'rgba(255, 255, 255, 0.6)',
    },
    Modal: {
      borderRadiusLG: 16,
      headerBg: 'transparent',
      titleFontSize: 18,
      titleColor: '#1D4A6B', // Foundation / Blue / blue-10
      paddingContentHorizontalLG: 24,
      paddingMD: 16,
    },
    Card: {
      borderRadiusLG: 12,
      colorBgContainer: 'rgba(255, 255, 255, 0.6)',
    },
    Tabs: {
      titleFontSize: 18,
      itemColor: '#7C8B98', // Foundation / Grey / grey-6
      itemActiveColor: '#1D4A6B', // Foundation / Blue / blue-10
      itemHoverColor: '#1D4A6B',
      itemSelectedColor: '#1D4A6B',
      inkBarColor: '#1D4A6B',
      horizontalItemPadding: '24px 16px 24px 0',
    },
    Menu: {
      itemBorderRadius: 12,
      itemColor: '#383F44',
      itemHoverColor: '#383F44',
      itemSelectedColor: '#383F44',
      itemHoverBg: 'rgba(0, 0, 0, 0.05)',
      itemSelectedBg: 'rgba(255, 255, 255, 1)', // Filter 激活态背景
    }
  },
};

export default themeConfig;