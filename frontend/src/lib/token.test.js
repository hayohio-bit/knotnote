import { beforeEach, describe, expect, it } from 'vitest'
import { tokenStorage } from './token.js'

describe('tokenStorage', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('액세스·리프레시 토큰을 저장하고 읽는다', () => {
    tokenStorage.setAccess('access-1')
    tokenStorage.setRefresh('refresh-1')
    expect(tokenStorage.getAccess()).toBe('access-1')
    expect(tokenStorage.getRefresh()).toBe('refresh-1')
  })

  it('저장된 값이 없으면 null 을 반환한다', () => {
    expect(tokenStorage.getAccess()).toBeNull()
    expect(tokenStorage.getRefresh()).toBeNull()
  })

  it('clear 는 두 토큰을 모두 제거한다', () => {
    tokenStorage.setAccess('access-1')
    tokenStorage.setRefresh('refresh-1')
    tokenStorage.clear()
    expect(tokenStorage.getAccess()).toBeNull()
    expect(tokenStorage.getRefresh()).toBeNull()
  })
})
