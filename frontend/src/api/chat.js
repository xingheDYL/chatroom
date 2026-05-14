import api from './auth'

export const chatApi = {
  getRooms() {
    return api.get('/rooms')
  },

  getAllRooms() {
    return api.get('/rooms/all')
  },

  getRoom(roomId) {
    return api.get(`/rooms/${roomId}`)
  },

  createRoom(data) {
    return api.post('/rooms', data)
  },

  joinRoom(roomId) {
    return api.post(`/rooms/${roomId}/join`)
  },

  leaveRoom(roomId) {
    return api.post(`/rooms/${roomId}/leave`)
  },

  getMessages(roomId) {
    return api.get(`/rooms/${roomId}/messages`)
  },

  sendMessage(roomId, content, messageType = 'text') {
    return api.post(`/rooms/${roomId}/messages`, { content, messageType })
  },

  getMembers(roomId) {
    return api.get(`/rooms/${roomId}/members`)
  },

  createOrGetPrivateChat(userId) {
    return api.post(`/rooms/private/${userId}`)
  },

  uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  getFileUrl(filename) {
    return `/files/${filename}`
  }
}
