import MarkdownIt from 'markdown-it'

/**
 * markdown-it 单例。
 * html: false 关闭原始 HTML 渲染，防止 XSS；
 * linkify 开启链接识别。
 */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

/** 渲染 Markdown 文本为 HTML 字符串 */
export function renderMarkdown(text: string): string {
  if (!text) return ''
  return md.render(text)
}
