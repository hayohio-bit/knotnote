import { describe, expect, it } from 'vitest'
import { blocksToMd, mdToBlocks } from './SimpleEditor.jsx'

const roundTrip = (md) => blocksToMd(mdToBlocks(md))

describe('SimpleEditor 마크다운 왕복 변환', () => {
  // 서식 모드에서 한 글자만 고쳐도 본문 전체가 재직렬화되므로,
  // 왕복 변환이 원문을 바꾸면 그대로 저장되어 데이터가 손상된다.
  it.each([
    ['순서 목록의 번호를 유지한다', '1. 첫째\n2. 둘째\n3. 셋째'],
    ['1 이 아닌 번호로 시작해도 유지한다', '5. 다섯\n6. 여섯'],
    ['모두 1 로 쓴 목록도 그대로 둔다', '1. 하나\n1. 둘'],
    ['코드블록 내부를 목록으로 오인하지 않는다', '```js\nconst a = 1\n2. not a list\n```'],
    ['언어 표기가 없는 코드블록', '```\nplain\n```'],
    ['빈 코드블록', '```js\n```'],
    ['닫히지 않은 코드블록', '```js\nconst a = 1'],
    ['코드블록 앞뒤의 본문', '앞\n\n```py\nx = 2\n```\n\n뒤'],
    ['중첩 목록', '- 상위\n  - 하위'],
    ['인용문', '> 인용'],
    ['표', '| A | B |\n| --- | --- |\n| 1 | 2 |'],
    ['인라인 강조 마커', '**굵게** 그리고 *기울임*'],
    ['체크박스', '- [x] 완료\n- [ ] 미완료'],
    ['제목과 구분선', '# H1\n## H2\n### H3\n---'],
    ['연속된 빈 줄', 'a\n\n\nb'],
  ])('%s', (_, md) => {
    expect(roundTrip(md)).toBe(md)
  })

  it('번호가 없는 새 항목은 직전 항목에서 이어 붙인다', () => {
    const blocks = mdToBlocks('1. 첫째\n2. 둘째')
    blocks.push({ id: 999, type: 'ol', text: '셋째' })
    expect(blocksToMd(blocks)).toBe('1. 첫째\n2. 둘째\n3. 셋째')
  })

  it('코드블록을 하나의 블록으로 묶는다', () => {
    const blocks = mdToBlocks('```js\na\nb\n```')
    expect(blocks).toHaveLength(1)
    expect(blocks[0]).toMatchObject({ type: 'code', lang: 'js', text: 'a\nb' })
  })
})
