import { get } from './apiClient'

export const fetchTeams = () => get('/teams')
export const fetchTeamSeasons = (code) => get(`/teams/${encodeURIComponent(code)}/seasons`)
export const fetchRoster = (code, season) => get(`/teams/${encodeURIComponent(code)}/seasons/${season}/players`)
export const fetchGameModes = () => get('/game-modes')
