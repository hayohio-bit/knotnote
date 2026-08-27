import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { act } from 'react'
import { describe, expect, it } from 'vitest'
import { confirmDialog } from '../lib/confirm.js'
import ConfirmHost from './ConfirmHost.jsx'

describe('ConfirmHost + confirmDialog', () => {
  it('confirmDialog 호출 시 메시지와 확인 라벨이 표시된다', async () => {
    render(<ConfirmHost />)
    let promise
    act(() => {
      promise = confirmDialog('노트를 삭제할까요?', { confirmLabel: '삭제', danger: true })
    })
    expect(screen.getByText('노트를 삭제할까요?')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '삭제' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: '삭제' }))
    await expect(promise).resolves.toBe(true)
  })

  it('취소 버튼을 누르면 false 로 resolve 되고 모달이 닫힌다', async () => {
    render(<ConfirmHost />)
    let promise
    act(() => {
      promise = confirmDialog('진행할까요?')
    })
    await userEvent.click(screen.getByRole('button', { name: '취소' }))
    await expect(promise).resolves.toBe(false)
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('Escape 키로 닫으면 false 로 resolve 된다', async () => {
    render(<ConfirmHost />)
    let promise
    act(() => {
      promise = confirmDialog('진행할까요?')
    })
    await userEvent.keyboard('{Escape}')
    await expect(promise).resolves.toBe(false)
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })
})
