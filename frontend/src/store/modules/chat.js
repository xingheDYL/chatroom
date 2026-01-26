import { chatApi } from '@/api/chat'

const state = {
  rooms: [],
  allRooms: [],
  currentRoom: null,
  messages: [],
  members: [],
  onlineUsers: 0
}

const getters = {
  sortedMessages: (state) => {
    return [...state.messages].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
  },
  memberIds: (state) => state.members.map(m => m.id),
  getMemberById: (state) => (userId) => {
    return state.members.find(m => m.id === Number(userId))
  },
  getMemberName: (state, getters) => (userId) => {
    const member = getters.getMemberById(userId)
    return member ? member.username : `用户 ${userId}`
  }
}

const mutations = {
  SET_ROOMS(state, rooms) {
    state.rooms = rooms
  },
  SET_ALL_ROOMS(state, rooms) {
    state.allRooms = rooms
  },
  SET_CURRENT_ROOM(state, room) {
    state.currentRoom = room
  },
  SET_MESSAGES(state, messages) {
    state.messages = messages
  },
  ADD_MESSAGE(state, message) {
    state.messages.push(message)
  },
  SET_MEMBERS(state, members) {
    state.members = members
  },
  SET_ONLINE_USERS(state, count) {
    state.onlineUsers = count
  },
  CLEAR_MESSAGES(state) {
    state.messages = []
  },
  ADD_ROOM(state, room) {
    const existingRoom = state.rooms.find(r => r.id === room.id)
    if (!existingRoom) {
      state.rooms.push(room)
    }
  }
}

const actions = {
  async fetchRooms({ commit }) {
    const response = await chatApi.getRooms()
    // 后端返回 ApiResponse 包装的数据
    const data = response.data
    commit('SET_ROOMS', data)
    return { data }
  },

  async fetchAllRooms({ commit }) {
    const response = await chatApi.getAllRooms()
    const data = response.data
    commit('SET_ALL_ROOMS', data)
    return { data }
  },

  async getRoom(_, roomId) {
    const response = await chatApi.getRoom(roomId)
    const data = response.data
    return { data }
  },

  async createRoom({ commit }, { name, description, type, maxMembers }) {
    const response = await chatApi.createRoom({ name, description, type, maxMembers })
    const data = response.data
    commit('ADD_ROOM', data)
    return { data }
  },

  async joinRoom({ commit }, roomId) {
    const response = await chatApi.joinRoom(roomId)
    const data = response.data
    return { data }
  },

  async leaveRoom({ commit, state }, roomId) {
    const response = await chatApi.leaveRoom(roomId)
    const data = response.data
    commit('SET_ROOMS', state.rooms.filter(r => r.id !== roomId))
    if (state.currentRoom?.id === roomId) {
      commit('SET_CURRENT_ROOM', null)
      commit('CLEAR_MESSAGES')
    }
    return { data }
  },

  async fetchRoomMessages({ commit }, roomId) {
    const response = await chatApi.getMessages(roomId)
    const data = response.data
    commit('SET_MESSAGES', data)
    return { data }
  },

  async fetchRoomMembers({ commit }, roomId) {
    const response = await chatApi.getMembers(roomId)
    const data = response.data
    commit('SET_MEMBERS', data)
    return { data }
  },

  addMessage({ commit }, message) {
    commit('ADD_MESSAGE', message)
  },

  setCurrentRoom({ commit }, room) {
    commit('SET_CURRENT_ROOM', room)
  },

  setOnlineUsers({ commit }, count) {
    commit('SET_ONLINE_USERS', count)
  },

  clearMessages({ commit }) {
    commit('CLEAR_MESSAGES')
  },

  async createOrGetPrivateChat({ commit }, userId) {
    const response = await chatApi.createOrGetPrivateChat(userId)
    const data = response.data
    commit('ADD_ROOM', data)
    return { data }
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
