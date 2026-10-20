export const player = (over = {}) => ({
  id: 'ps-1', playerId: 'p-1', name: 'Test Batter', country: 'India', role: 'BATTER', overseas: false,
  rating: 80, teamCode: 'CSK', teamName: 'Chennai Super Kings', season: 2023, ...over,
})

export const resultFixture = {
  draftId: 'd1', mode: 'CLASSIC', modeLabel: 'Classic Draft', team: { id: 't', code: 'CSK', name: 'Chennai Super Kings' },
  season: 2023, challengeCode: null, teamRating: 84, battingScore: 80, bowlingScore: 80, allRoundScore: 80,
  wicketkeepingScore: 80, roleBalanceScore: 100, overseasPlayers: 2, points: 84, completionSeconds: 125, isComplete: true,
  validationMessages: [], completedAt: '2026-01-01T10:00:00Z', shared: false, shareToken: null,
  picks: [
    { pickId: 'k1', pickNumber: 1, player: player({ id: 'a', name: 'Keeper One', role: 'WICKETKEEPER' }) },
    { pickId: 'k2', pickNumber: 2, player: player({ id: 'b', name: 'Import Bat', overseas: true }) },
  ],
}
