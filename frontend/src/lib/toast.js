let seq = 0

/**
 * 전역 토스트. ToastHost 가 'knotnote:toast' 이벤트를 받아 렌더링한다.
 * 사용: toast.success('저장했어요') / toast.error('저장에 실패했어요')
 */
export const toast = {
  show(message, type = 'info') {
    window.dispatchEvent(
      new CustomEvent('knotnote:toast', { detail: { id: ++seq, message, type } }),
    )
  },
  success(message) {
    this.show(message, 'success')
  },
  error(message) {
    this.show(message, 'error')
  },
}
