import mermaid from 'mermaid'
import React, { useEffect, useRef } from 'react'
import ReactMarkdown from 'react-markdown'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { dracula } from 'react-syntax-highlighter/dist/esm/styles/prism'
import remarkGfm from 'remark-gfm'
import './MarkdownPreview.css'

mermaid.initialize({ startOnLoad: false, theme: 'default' })

const Mermaid = ({ chart }) => {
  const containerRef = useRef(null)

  useEffect(() => {
    let isMounted = true
    const renderChart = async () => {
      try {
        const id = 'mermaid-' + Math.random().toString(36).substr(2, 9)
        const { svg } = await mermaid.render(id, chart)
        if (isMounted && containerRef.current) {
          containerRef.current.innerHTML = svg
        }
      } catch (e) {
        if (isMounted && containerRef.current) {
          containerRef.current.innerHTML = `<p style="color:red; font-size:12px;">Mermaid Error: ${e.message}</p>`
        }
      }
    }
    renderChart()
    return () => {
      isMounted = false
    }
  }, [chart])

  return <div ref={containerRef} className="mermaid-container" />
}

export default function MarkdownPreview({ source }) {
  if (!source) return <p className="md-preview-empty">내용이 없습니다.</p>

  return (
    <div className="markdown-preview-body">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code({ node, inline, className, children, ...props }) {
            const match = /language-(\w+)/.exec(className || '')
            const content = String(children).replace(/\n$/, '')

            if (!inline && match && match[1] === 'mermaid') {
              return <Mermaid chart={content} />
            }

            return !inline && match ? (
              <SyntaxHighlighter style={dracula} language={match[1]} PreTag="div" {...props}>
                {content}
              </SyntaxHighlighter>
            ) : (
              <code className={className} {...props}>
                {children}
              </code>
            )
          },
        }}
      >
        {source}
      </ReactMarkdown>
    </div>
  )
}
