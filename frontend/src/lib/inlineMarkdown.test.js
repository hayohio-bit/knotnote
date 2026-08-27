import { describe, expect, it } from 'vitest'
import { renderInline } from './inlineMarkdown.js'

// 렌더링 결과에서 태그를 걷어낸 순수 텍스트. 편집기의 커서 계산이 이 값의
// 길이에 의존하므로 원문과 반드시 같아야 한다.
const plainText = (html) =>
  html
    .replace(/<[^>]+>/g, '')
    .replace(/&quot;/g, '"')
    .replace(/&gt;/g, '>')
    .replace(/&lt;/g, '<')
    .replace(/&amp;/g, '&')

describe('renderInline', () => {
  it.each([
    '**굵게** 와 *기울임* 이 있는 문장.',
    '`code` 안의 **별표** 는 그대로 둔다',
    '~~취소선~~ 과 **굵게** 를 섞은 경우',
    '별표 하나만 있는 경우 *',
    'a ** b ** c',
    '태그처럼 보이는 <script>alert(1)</script> 문자열',
    '앰퍼샌드 & 와 따옴표 " 가 든 문장',
    '숫자를 공백으로 감싼 값 1 개',
    '마커가 없는 평범한 문장',
  ])('원문 텍스트를 그대로 보존한다: %s', (src) => {
    expect(plainText(renderInline(src))).toBe(src)
  })

  it('굵게를 strong 으로 그리고 마커를 남긴다', () => {
    const html = renderInline('**굵게**')
    expect(html).toContain('<strong>굵게</strong>')
    expect(html).toContain('<span class="se-mk">**</span>')
  })

  it('기울임을 em 으로 그린다', () => {
    expect(renderInline('*기울임*')).toContain('<em>기울임</em>')
  })

  it('취소선을 del 로 그린다', () => {
    expect(renderInline('~~지움~~')).toContain('<del>지움</del>')
  })

  it('코드 스팬 안의 별표는 강조로 해석하지 않는다', () => {
    expect(renderInline('`a * b * c`')).not.toContain('<em>')
  })

  it('HTML 을 이스케이프한다', () => {
    const html = renderInline('<img onerror=x>')
    expect(html).not.toContain('<img')
    expect(html).toContain('&lt;img')
  })

  it('빈 문자열은 빈 문자열로 둔다', () => {
    expect(renderInline('')).toBe('')
  })
})
