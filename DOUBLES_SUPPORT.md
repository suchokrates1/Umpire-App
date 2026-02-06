# Doubles Support - Implementation Guide

## Overview

The Umpire App now supports **Doubles matches** (4 players) in addition to Singles (2 players). This feature implements proper doubles serving rotation and team-based scoring.

## Features

### 1. Player Selection
- **Checkbox** in PlayerSelectionActivity toggles between Singles and Doubles mode
- **Singles**: Select 2 players
- **Doubles**: Select 4 players (Players 1-2 form Team A, Players 3-4 form Team B)
- **Auto-proceed**: Automatically advances when required number of players selected (300ms delay)
- **Visual feedback**: Selected players marked with numbered badges (1-4 for doubles, 1-2 for singles)

### 2. Team Display
- **Team Names**: Shows combined player names "Player1 / Player3" and "Player2 / Player4"
- **Team Icon**: 👥 emoji instead of country flags for doubles
- **Scoreboard**: Displays team names instead of individual players
- **Server Indicator**: Shows which specific player (1-4) is currently serving

### 3. Serving Rotation
Doubles follows official tennis serving rotation rules:

**Rotation Order**: 1 → 2 → 3 → 4 → 1 (repeats)
- Player 1 serves first game (Team A)
- Player 2 serves second game (Team B)  
- Player 3 serves third game (Team A partner)
- Player 4 serves fourth game (Team B partner)
- Back to Player 1 for fifth game

**Tiebreak Rotation**: Same rotation but changes every 2 points instead of after each game

### 4. Match State Changes

#### New Fields in MatchState:
```kotlin
val player3: Player? = null        // Team A partner
val player4: Player? = null        // Team B partner
val isDoubles: Boolean = false     // Match type flag
val team1Name: String? = null      // Optional team name
val team2Name: String? = null      // Optional team name
var currentServer: Int = 1         // 1-4, current server in doubles
```

#### New Helper Methods:
- `getTeam1DisplayName()` - Returns "Player1 / Player3" or custom team name
- `getTeam2DisplayName()` - Returns "Player2 / Player4" or custom team name
- `getCurrentServerName()` - Returns name of player currently serving (1-4)

### 5. ViewModel Logic

#### `rotateDoublesServer(state: MatchState)`
Handles serving rotation after each game:
```kotlin
1 -> 2  // Team A (Player 1) → Team B (Player 2)
2 -> 3  // Team B (Player 2) → Team A (Player 3)
3 -> 4  // Team A (Player 3) → Team B (Player 4)
4 -> 1  // Team B (Player 4) → Team A (Player 1)
```

Updates `isPlayer1Serving` based on currentServer:
- Server 1 or 3: `isPlayer1Serving = true` (Team A)
- Server 2 or 4: `isPlayer1Serving = false` (Team B)

#### Tiebreak Special Handling
In tiebreak/super tiebreak:
- Rotation changes every 2 points
- Same 1→2→3→4 pattern but more frequent
- Side changes every 6 points (standard rule)

### 6. UI Updates

#### MatchActivity
- **Server Selection**: Shows team names with color coding
- **Serve View**: Displays current server's name (not just team)
- **Rally View**: Shows team names for scoring
- **Scoreboard**: Team names, team icon (👥), proper serving indicator

#### Color Coding (Optional)
- `team1_color`: Light blue (#BBDEFB)
- `team2_color`: Light orange (#FFCCBC)
- Applied to server selection buttons for visual distinction

## API Integration

### Match Creation
When creating a doubles match via API:
```json
{
  "player1Name": "Kowalski / Nowak",  // Team A
  "player2Name": "Smith / Johnson",   // Team B
  "isDoubles": true
}
```

The backend receives team names as combined strings. Individual player tracking happens in the app only.

### Statistics
Statistics are tracked per team (Team A vs Team B) not per individual player in doubles mode.

## Testing

### Test Scenarios

1. **Basic Doubles Flow**
   - Select 4 players
   - Start match
   - Verify Player 1 serves first
   - Play game 1 → Player 2 should serve game 2
   - Play game 2 → Player 3 should serve game 3
   - Play game 3 → Player 4 should serve game 4
   - Play game 4 → Player 1 should serve game 5

2. **Tiebreak in Doubles**
   - Reach 4-4 in a set
   - Verify tiebreak starts
   - Server rotates every 2 points: 1→2→3→4→1
   - Sides change at 6 points

3. **Team Display**
   - Verify team names show "Player1 / Player3"
   - Verify team icon 👥 appears instead of flags
   - Verify current server name displayed correctly

4. **Undo in Doubles**
   - Make several actions
   - Undo last action
   - Verify server rotation state restored correctly

## Implementation Files

### Modified Files
1. **MatchState.kt**
   - Added player3, player4, isDoubles, team names
   - Added currentServer field
   - Added helper methods for team display
   - Updated toMatch() to send team names

2. **PlayerSelectionActivity.kt**
   - Updated proceedToNextScreen() to create doubles MatchState
   - Passes all 4 players when isDoubles = true

3. **MatchActivity.kt**
   - Updated updatePlayerNames() for doubles team display
   - Updated updateScoreboard() to show team names
   - Added team color support

4. **MatchViewModel.kt**
   - Added rotateDoublesServer() function
   - Modified addPoint() tiebreak logic for doubles
   - Modified checkGameAndSetStatus() to call rotateDoublesServer()

5. **strings.xml**
   - Added `team_serves` string resource

6. **colors.xml**
   - Already had team1_color and team2_color defined

## Known Limitations

1. **Backend**: Backend currently receives team names as single strings, not individual player IDs
2. **Statistics**: Per-player statistics not tracked separately in doubles (tracked per team)
3. **Match History**: Doubles matches stored with team names, not individual players
4. **Resuming**: Doubles matches can be resumed, but currentServer state preserved

## Future Enhancements

1. **Custom Team Names**: Allow users to name teams (e.g., "Dream Team", "The Aces")
2. **Per-Player Stats**: Track aces, faults, winners for each of 4 players individually
3. **Partner Swapping**: Allow changing partners mid-tournament
4. **Mixed Doubles**: Gender indicator and proper mixed doubles rules
5. **Serving Order Change**: Allow manual override of serving rotation if needed

## Rules Reference

### Official Doubles Serving Rules

**ITF Rules of Tennis - Rule 27: Doubles**

> The order of serving shall be decided at the beginning of each set as follows:
> 
> - The pair who have to serve in the first game shall decide which partner shall do so
> - The opposing pair shall similarly decide which partner shall serve in the second game
> - The partner of the player who served in the first game shall serve in the third game
> - The partner of the player who served in the second game shall serve in the fourth game
> 
> This order shall be maintained throughout the set.

**Tiebreak**:
> Players change serve after every two points, following the same rotation pattern established at the beginning of the set.

## Troubleshooting

### Server rotation not working
- Check `state.isDoubles` is true
- Verify `rotateDoublesServer()` is called after game win
- Check `currentServer` value (should be 1-4)

### Team names not showing
- Verify player3 and player4 are not null
- Check `getTeam1DisplayName()` returns combined names
- Ensure MatchActivity uses doubles-aware display logic

### API errors with doubles
- Backend expects team names as strings
- Use `getTeam1DisplayName()` when sending to API
- Check Match creation includes both team names

## Commit Information

**Branch**: main  
**Feature**: Doubles Support  
**Status**: ✅ Complete  

All doubles functionality implemented and tested. App supports both singles (2 players) and doubles (4 players) with proper serving rotation according to ITF rules.
