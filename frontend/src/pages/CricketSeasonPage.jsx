import React, { useEffect, useState } from "react";

const TEAM_NAMES = [
  "Chennai Super Kings",
  "Mumbai Indians",
  "Royal Challengers Bengaluru",
  "Kolkata Knight Riders",
  "Sunrisers Hyderabad",
  "Rajasthan Royals",
  "Delhi Capitals",
  "Punjab Kings",
  "Gujarat Titans",
  "Lucknow Super Giants",
];

const STORAGE_KEY = "ipl-cricket-season-v1";

function createSeason() {
  const matches = [];
  let id = 1;

  // Double round-robin: every team plays every other team twice.
  for (let i = 0; i < TEAM_NAMES.length; i++) {
    for (let j = i + 1; j < TEAM_NAMES.length; j++) {
      for (let leg = 1; leg <= 2; leg++) {
        matches.push({
          id: id++,
          team1: TEAM_NAMES[i],
          team2: TEAM_NAMES[j],
          leg,
          played: false,
          score1: null,
          score2: null,
          winner: null,
        });
      }
    }
  }

  return {
    matches,
    champion: null,
    playoffs: [],
  };
}

function randomScore() {
  return Math.floor(Math.random() * 100) + 130;
}

function simulateMatch(match) {
  const score1 = randomScore();
  const score2 = randomScore();
  const winner = score1 >= score2 ? match.team1 : match.team2;

  return {
    ...match,
    played: true,
    score1,
    score2,
    winner,
  };
}

function buildStandings(matches) {
  const table = Object.fromEntries(
    TEAM_NAMES.map((team) => [
      team,
      {
        team,
        played: 0,
        won: 0,
        lost: 0,
        points: 0,
        runsFor: 0,
        runsAgainst: 0,
      },
    ])
  );

  matches.filter((match) => match.played).forEach((match) => {
    const a = table[match.team1];
    const b = table[match.team2];

    a.played++;
    b.played++;

    a.runsFor += match.score1;
    a.runsAgainst += match.score2;
    b.runsFor += match.score2;
    b.runsAgainst += match.score1;

    if (match.winner === match.team1) {
      a.won++;
      b.lost++;
      a.points += 2;
    } else {
      b.won++;
      a.lost++;
      b.points += 2;
    }
  });

  return Object.values(table)
    .map((team) => ({
      ...team,
      nrr:
        team.runsAgainst === 0
          ? 0
          : (team.runsFor - team.runsAgainst) / Math.max(team.played, 1),
    }))
    .sort((a, b) => b.points - a.points || b.nrr - a.nrr);
}

export default function CricketSeasonPage() {
  const [season, setSeason] = useState(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) : createSeason();
    } catch {
      return createSeason();
    }
  });

  const [filter, setFilter] = useState("All");
  const [selectedMatch, setSelectedMatch] = useState(null);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(season));
  }, [season]);

  const standings = buildStandings(season.matches);
  const completedMatches = season.matches.filter((match) => match.played);
  const remainingMatches = season.matches.filter((match) => !match.played);

  const visibleMatches =
    filter === "Completed"
      ? season.matches.filter((match) => match.played)
      : filter === "Upcoming"
      ? season.matches.filter((match) => !match.played)
      : season.matches;

  function updateMatch(matchId) {
    setSeason((current) => ({
      ...current,
      matches: current.matches.map((match) =>
        match.id === matchId ? simulateMatch(match) : match
      ),
    }));
  }

  function simulateAllLeagueMatches() {
    setSeason((current) => ({
      ...current,
      matches: current.matches.map((match) =>
        match.played ? match : simulateMatch(match)
      ),
    }));
  }

  function resetSeason() {
    if (window.confirm("Reset all season results? This cannot be undone.")) {
      setSeason(createSeason());
      setSelectedMatch(null);
    }
  }

  function simulatePlayoffs() {
    if (remainingMatches.length > 0) {
      window.alert("Complete all 90 league matches before starting playoffs.");
      return;
    }

    const topFour = buildStandings(season.matches).slice(0, 4);

    // Simplified playoff simulation: top four teams compete for the title.
    const qualifier1 = simulateMatch({
      id: "Q1",
      team1: topFour[0].team,
      team2: topFour[1].team,
      played: false,
    });

    const eliminator = simulateMatch({
      id: "E",
      team1: topFour[2].team,
      team2: topFour[3].team,
      played: false,
    });

    const qualifier2 = simulateMatch({
      id: "Q2",
      team1: qualifier1.winner === qualifier1.team1
        ? qualifier1.team2
        : qualifier1.team1,
      team2: eliminator.winner,
      played: false,
    });

    const final = simulateMatch({
      id: "Final",
      team1: qualifier1.winner,
      team2: qualifier2.winner,
      played: false,
    });

    setSeason((current) => ({
      ...current,
      playoffs: [qualifier1, eliminator, qualifier2, final],
      champion: final.winner,
    }));
  }

  const styles = {
    page: {
      minHeight: "100vh",
      background: "#f4f6fa",
      color: "#172033",
      padding: "28px",
      fontFamily: "Arial, sans-serif",
    },
    container: {
      maxWidth: "1200px",
      margin: "0 auto",
    },
    header: {
      background: "#101b35",
      color: "white",
      padding: "28px",
      borderRadius: "16px",
      marginBottom: "24px",
    },
    grid: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
      gap: "16px",
      marginBottom: "24px",
    },
    card: {
      background: "white",
      borderRadius: "12px",
      padding: "20px",
      boxShadow: "0 3px 12px rgba(0,0,0,0.05)",
      marginBottom: "20px",
      overflowX: "auto",
    },
    button: {
      border: "none",
      borderRadius: "8px",
      padding: "10px 14px",
      cursor: "pointer",
      background: "#2457e6",
      color: "white",
      fontWeight: 600,
      margin: "4px",
    },
    secondaryButton: {
      border: "1px solid #d8deea",
      borderRadius: "8px",
      padding: "9px 12px",
      cursor: "pointer",
      background: "white",
      color: "#172033",
      margin: "4px",
    },
    table: {
      width: "100%",
      borderCollapse: "collapse",
      minWidth: "650px",
      textAlign: "left",
    },
    cell: {
      padding: "12px",
      borderBottom: "1px solid #e9edf4",
    },
  };

  return (
    <main style={styles.page}>
      <div style={styles.container}>
        <header style={styles.header}>
          <div style={{ color: "#a9c1ff", fontSize: "13px" }}>
            IPL DRAFT CLUB
          </div>
          <h1 style={{ margin: "10px 0" }}>Season Results</h1>
          <p style={{ color: "#d5dcef", marginBottom: "20px" }}>
            Follow the league table, match results and playoff race.
          </p>

          <button
            style={styles.button}
            onClick={simulateAllLeagueMatches}
          >
            Simulate All League Matches
          </button>
          <button style={styles.secondaryButton} onClick={resetSeason}>
            Reset Season
          </button>
        </header>

        <section style={styles.grid}>
          {[
            ["Teams", "10"],
            ["League Matches", "90"],
            ["Completed", completedMatches.length],
            ["Remaining", remainingMatches.length],
          ].map(([label, value]) => (
            <div key={label} style={styles.card}>
              <div style={{ color: "#68748b", fontSize: "14px" }}>
                {label}
              </div>
              <div style={{ fontSize: "30px", fontWeight: 700, marginTop: 8 }}>
                {value}
              </div>
            </div>
          ))}
        </section>

        {season.champion && (
          <section
            style={{
              ...styles.card,
              background: "#fff7df",
              border: "1px solid #f3d477",
              textAlign: "center",
            }}
          >
            <div style={{ fontSize: "34px" }}>🏆</div>
            <h2>Season Champion</h2>
            <h3>{season.champion}</h3>
          </section>
        )}

        <section style={styles.card}>
          <h2>Points Table</h2>
          <p style={{ color: "#68748b" }}>
            Win = 2 points. The top four qualify for the playoffs.
          </p>

          <table style={styles.table}>
            <thead>
              <tr>
                {["Pos", "Team", "P", "W", "L", "Pts", "NRR"].map((heading) => (
                  <th key={heading} style={styles.cell}>{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {standings.map((team, index) => (
                <tr
                  key={team.team}
                  style={{
                    background: index < 4 ? "#effbf4" : "white",
                  }}
                >
                  <td style={styles.cell}>{index + 1}</td>
                  <td style={{ ...styles.cell, fontWeight: 600 }}>
                    {team.team}
                    {index < 4 && (
                      <span style={{ color: "#17804a", fontSize: 12 }}>
                        {" "}• Playoff position
                      </span>
                    )}
                  </td>
                  <td style={styles.cell}>{team.played}</td>
                  <td style={styles.cell}>{team.won}</td>
                  <td style={styles.cell}>{team.lost}</td>
                  <td style={{ ...styles.cell, fontWeight: 700 }}>
                    {team.points}
                  </td>
                  <td style={styles.cell}>{team.nrr.toFixed(3)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section style={styles.card}>
          <h2>League Fixtures & Results</h2>
          <div style={{ marginBottom: 12 }}>
            {["All", "Upcoming", "Completed"].map((option) => (
              <button
                key={option}
                style={
                  filter === option ? styles.button : styles.secondaryButton
                }
                onClick={() => setFilter(option)}
              >
                {option}
              </button>
            ))}
          </div>

          <table style={styles.table}>
            <thead>
              <tr>
                {["Match", "Fixture", "Leg", "Result", "Action"].map((heading) => (
                  <th key={heading} style={styles.cell}>{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visibleMatches.map((match) => (
                <tr key={match.id}>
                  <td style={styles.cell}>{match.id}</td>
                  <td style={styles.cell}>
                    {match.team1} vs {match.team2}
                  </td>
                  <td style={styles.cell}>{match.leg}</td>
                  <td style={styles.cell}>
                    {match.played
                      ? `${match.team1} ${match.score1} - ${match.score2} ${match.team2}`
                      : "Upcoming"}
                  </td>
                  <td style={styles.cell}>
                    {match.played ? (
                      <button
                        style={styles.secondaryButton}
                        onClick={() => setSelectedMatch(match)}
                      >
                        View
                      </button>
                    ) : (
                      <button
                        style={styles.button}
                        onClick={() => updateMatch(match.id)}
                      >
                        Simulate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {selectedMatch && (
          <section style={styles.card}>
            <h2>Match Details</h2>
            <h3>{selectedMatch.team1} vs {selectedMatch.team2}</h3>
            <p>
              Score: {selectedMatch.score1} - {selectedMatch.score2}
            </p>
            <p>Winner: {selectedMatch.winner}</p>
            <button
              style={styles.secondaryButton}
              onClick={() => setSelectedMatch(null)}
            >
              Close
            </button>
          </section>
        )}

        <section style={styles.card}>
          <h2>Playoffs</h2>
          <p style={{ color: "#68748b" }}>
            Finish the league stage to unlock the playoff simulation.
          </p>
          <button
            style={styles.button}
            onClick={simulatePlayoffs}
            disabled={remainingMatches.length > 0 || Boolean(season.champion)}
          >
            Simulate Playoffs
          </button>

          {season.playoffs.length > 0 && (
            <div style={styles.grid}>
              {season.playoffs.map((match) => (
                <div key={match.id} style={{ ...styles.card, marginBottom: 0 }}>
                  <div style={{ color: "#68748b" }}>{match.id}</div>
                  <h3>{match.team1}</h3>
                  <p>
                    {match.score1} - {match.score2}
                  </p>
                  <h3>{match.team2}</h3>
                  <strong>Winner: {match.winner}</strong>
                </div>
              ))}
            </div>
          )}
        </section>

        <p style={{ color: "#68748b", fontSize: 13 }}>
          Results are simulated and saved in this browser. They are not connected
          to your backend database.
        </p>
      </div>
    </main>
  );
}