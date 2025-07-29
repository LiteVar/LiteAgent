import hljs from 'highlight.js';

export const codeRenderer = (code: any) => {
  return ` <div class="code-block-wrapper rounded-lg relative">
            <div class="code-header flex justify-between items-center sticky top-0 z-10 px-4 py-2 bg-gray-200 border-b border-gray-200 rounded-t-lg">
              <span class="text-sm">${code.lang || 'plaintext'}</span>
              <span 
                class="copy-btn flex items-center gap-1 text-sm hover:text-blue-500 cursor-pointer"
              >
                复制
              </span>
            </div>
            <pre class="overflow-x-auto"><code class="hljs language-${code.lang} p-4">${
              hljs.highlight(code.text, { language: code.lang || 'plaintext' }).value
            }</code></pre>
          </div>`;
}; 