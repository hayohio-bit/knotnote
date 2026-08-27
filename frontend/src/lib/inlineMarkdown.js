// 서식 편집기용 인라인 마크다운 렌더러.
//
// 마커(**, *, `, ~~)를 지우지 않고 <span class="se-mk"> 로 감싼 채 남겨 둔다.
// 편집기의 커서 계산이 요소의 textContent 길이를 기준으로 하므로, 렌더링
// 결과의 textContent 가 원문과 한 글자라도 달라지면 커서가 어긋난다.
// 마커를 남기는 방식은 그 제약을 지키면서 강조를 눈에 보이게 한다.

const escapeHtml = (s) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

const marker = (m) => `<span class="se-mk">${m}</span>`

// 치환한 조각은 자리표시자로 빼 두었다가 마지막에 되돌린다. 그러지 않으면
// 앞서 만든 마커의 별표를 뒤따르는 정규식이 다시 잡는다. 경계로는 본문에
// 나타나지 않는 사설 영역 문자를 쓴다. 공백처럼 흔한 문자를 쓰면 "값 1 개" 같은
// 문장이 자리표시자로 오인된다.
const SENTINEL = '\ue000'
const PLACEHOLDER_RE = /\ue000(\d+)\ue000/g

export function renderInline(text) {
  if (!text) return ''

  const parts = []
  const stash = (html) => {
    parts.push(html)
    return `${SENTINEL}${parts.length - 1}${SENTINEL}`
  }

  let out = text

  // 코드 스팬이 가장 먼저다. 그 안의 별표는 강조로 해석하지 않는다.
  out = out.replace(/`([^`\n]+)`/g, (_, inner) =>
    stash(`${marker('`')}<code class="se-code-span">${escapeHtml(inner)}</code>${marker('`')}`),
  )
  out = out.replace(/~~([^~\n]+)~~/g, (_, inner) =>
    stash(`${marker('~~')}<del>${escapeHtml(inner)}</del>${marker('~~')}`),
  )
  out = out.replace(/\*\*([^*\n]+)\*\*/g, (_, inner) =>
    stash(`${marker('**')}<strong>${escapeHtml(inner)}</strong>${marker('**')}`),
  )
  out = out.replace(/\*([^*\n]+)\*/g, (_, inner) =>
    stash(`${marker('*')}<em>${escapeHtml(inner)}</em>${marker('*')}`),
  )

  out = escapeHtml(out)

  return out.replace(PLACEHOLDER_RE, (_, n) => parts[Number(n)])
}
