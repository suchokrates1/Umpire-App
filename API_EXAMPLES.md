# Przykładowe dane API dla testowania

## GET /api/players

Endpoint zwracający listę zawodników.

**Response 200:**
```json
{
  "players": [
    {
      "id": 1,
      "first_name": "Jan",
      "last_name": "Kowalski",
      "full_name": "Jan Kowalski",
      "country": "POL",
      "ranking": 42,
      "photo_url": null
    },
    {
      "id": 2,
      "first_name": "Maria",
      "last_name": "Nowak",
      "full_name": "Maria Nowak",
      "country": "POL",
      "ranking": 38,
      "photo_url": null
    },
    {
      "id": 3,
      "first_name": "Roger",
      "last_name": "Smith",
      "full_name": "Roger Smith",
      "country": "USA",
      "ranking": 15,
      "photo_url": null
    },
    {
      "id": 4,
      "first_name": "Anna",
      "last_name": "Mueller",
      "full_name": "Anna Mueller",
      "country": "GER",
      "ranking": 27,
      "photo_url": null
    },
    {
      "id": 5,
      "first_name": "Pierre",
      "last_name": "Dubois",
      "full_name": "Pierre Dubois",
      "country": "FRA",
      "ranking": 53,
      "photo_url": null
    },
    {
      "id": 6,
      "first_name": "Elena",
      "last_name": "Garcia",
      "full_name": "Elena Garcia",
      "country": "ESP",
      "ranking": 19,
      "photo_url": null
    },
    {
      "id": 7,
      "first_name": "David",
      "last_name": "Brown",
      "full_name": "David Brown",
      "country": "GBR",
      "ranking": 61,
      "photo_url": null
    },
    {
      "id": 8,
      "first_name": "Sofia",
      "last_name": "Romano",
      "full_name": "Sofia Romano",
      "country": "ITA",
      "ranking": 33,
      "photo_url": null
    }
  ],
  "total_count": 8
}
```

## GET /api/courts

Endpoint zwracający listę kortów.

**Response 200:**
```json
{
  "courts": [
    {
      "id": 1,
      "name": "Kort 1",
      "is_available": true,
      "current_match_id": null
    },
    {
      "id": 2,
      "name": "Kort 2",
      "is_available": false,
      "current_match_id": 123
    },
    {
      "id": 3,
      "name": "Kort 3",
      "is_available": true,
      "current_match_id": null
    },
    {
      "id": 4,
      "name": "Kort 4",
      "is_available": true,
      "current_match_id": null
    }
  ],
  "total_count": 4
}
```

## POST /api/matches

Endpoint do tworzenia nowego meczu.

**Request Body:**
```json
{
  "court_id": 1,
  "player1_name": "Jan Kowalski",
  "player2_name": "Maria Nowak",
  "score": {
    "player1_sets": 0,
    "player2_sets": 0,
    "player1_games": 0,
    "player2_games": 0,
    "player1_points": 0,
    "player2_points": 0,
    "sets_history": []
  },
  "status": "not_started",
  "created_at": null,
  "updated_at": null
}
```

**Response 201:**
```json
{
  "id": 456,
  "court_id": 1,
  "player1_name": "Jan Kowalski",
  "player2_name": "Maria Nowak",
  "score": {
    "player1_sets": 0,
    "player2_sets": 0,
    "player1_games": 0,
    "player2_games": 0,
    "player1_points": 0,
    "player2_points": 0,
    "sets_history": []
  },
  "status": "in_progress",
  "created_at": "2025-11-06T10:30:00Z",
  "updated_at": "2025-11-06T10:30:00Z"
}
```
