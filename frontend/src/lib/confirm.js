/**
 * 전역 확인 모달. ConfirmHost 가 'knotnote:confirm' 이벤트를 받아 렌더링한다.
 * 사용: if (!(await confirmDialog('삭제할까요?', { confirmLabel: '삭제', danger: true }))) return
 * @returns {Promise<boolean>} 확인이면 true, 취소·닫기면 false
 */
export function confirmDialog(message, { confirmLabel = '확인', danger = false } = {}) {
  return new Promise((resolve) => {
    window.dispatchEvent(
      new CustomEvent('knotnote:confirm', {
        detail: { message, confirmLabel, danger, resolve },
      }),
    )
  })
}
