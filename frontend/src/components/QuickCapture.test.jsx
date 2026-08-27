import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { notesApi } from '../api/notes.js'
import QuickCapture from './QuickCapture.jsx'

vi.mock('../api/notes.js', () => ({
  notesApi: {
    create: vi.fn(),
  },
}))

function renderQuickCapture() {
  return render(
    <MemoryRouter>
      <QuickCapture />
    </MemoryRouter>,
  )
}

async function openModal() {
  await userEvent.keyboard('{Control>}k{/Control}')
  expect(screen.getByPlaceholderText('제목')).toBeInTheDocument()
}

describe('QuickCapture', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('Ctrl+K 로 열리고 Escape 로 닫힌다', async () => {
    renderQuickCapture()
    expect(screen.queryByPlaceholderText('제목')).not.toBeInTheDocument()

    await openModal()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByPlaceholderText('제목')).not.toBeInTheDocument()
  })

  it('제목 없이 저장하면 오류를 표시하고 API 를 호출하지 않는다', async () => {
    renderQuickCapture()
    await openModal()

    await userEvent.click(screen.getByRole('button', { name: '저장' }))
    expect(screen.getByText('제목을 입력해 주세요.')).toBeInTheDocument()
    expect(notesApi.create).not.toHaveBeenCalled()
  })

  it('저장 성공 시 노트를 생성하고 목록 갱신 이벤트를 발행한다', async () => {
    notesApi.create.mockResolvedValue({ data: { data: { id: 7 } } })
    const changed = vi.fn()
    window.addEventListener('knotnote:notes-changed', changed)

    renderQuickCapture()
    await openModal()
    await userEvent.type(screen.getByPlaceholderText('제목'), '  회의 메모  ')
    await userEvent.type(screen.getByPlaceholderText('내용 (선택 사항)'), '내용입니다')
    await userEvent.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() => expect(notesApi.create).toHaveBeenCalledWith('회의 메모', '내용입니다'))
    expect(changed).toHaveBeenCalled()
    expect(screen.queryByPlaceholderText('제목')).not.toBeInTheDocument()

    window.removeEventListener('knotnote:notes-changed', changed)
  })

  it('저장 실패 시 오류를 표시하고 모달을 유지한다', async () => {
    notesApi.create.mockRejectedValue(new Error('server error'))

    renderQuickCapture()
    await openModal()
    await userEvent.type(screen.getByPlaceholderText('제목'), '실패 케이스')
    await userEvent.click(screen.getByRole('button', { name: '저장' }))

    expect(await screen.findByText('저장에 실패했습니다. 다시 시도해 주세요.')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('제목')).toBeInTheDocument()
  })
})
