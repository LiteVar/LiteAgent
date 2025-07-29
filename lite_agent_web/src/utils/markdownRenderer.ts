import hljs from 'highlight.js';

interface CodeBlock {
  lang?: string;
  text: string;
}

export const codeRenderer = (code: CodeBlock) => {
  let highlightedCode;
  try {
    const language = code.lang || 'plaintext';
    if (language && hljs.getLanguage(language)) {
      highlightedCode = hljs.highlight(code.text, { language }).value;
    } else {
      const result = hljs.highlightAuto(code.text);
      highlightedCode = result.value;
    }
  } catch (error) {
    // 如果高亮失败，直接显示原文
    highlightedCode = code.text;
  }

  return ` <div class="code-block-wrapper rounded-lg relative">
            <div class="code-header flex justify-between items-center sticky top-0 z-10 px-4 py-2 bg-gray-200 border-b border-gray-200 rounded-t-lg">
              <span class="text-sm">${code.lang || 'plaintext'}</span>
              <span 
                class="copy-btn flex items-center gap-1 text-sm hover:text-blue-500 cursor-pointer"
              >
                复制
              </span>
            </div>
            <pre class="overflow-x-auto"><code class="hljs language-${code.lang || 'plaintext'} p-4">${highlightedCode}</code></pre>
          </div>`;
};

export const linkRenderer = (options: { href: string; text: string }) => {
  const { href, text } = options;
  return `<a href="${href}" target="_blank" rel="noopener noreferrer">${text}</a>`;
};
