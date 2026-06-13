package handlers

import (
	"encoding/json"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"

	"github.com/Group-Hackathon/p1/global-app-backend/internal/auth"
	"github.com/Group-Hackathon/p1/global-app-backend/internal/gemini"
)

// Handler holds dependencies for HTTP handlers.
type Handler struct {
	db   *pgxpool.Pool
	auth *auth.Service
}

// New creates a new Handler.
func New(db *pgxpool.Pool, authService *auth.Service) *Handler {
	return &Handler{db: db, auth: authService}
}

// --- Auth Handlers ---

type registerRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type authResponse struct {
	Token string `json:"token"`
	User  userResponse `json:"user"`
}

type userResponse struct {
	ID        string `json:"id"`
	Email     string `json:"email"`
	CreatedAt string `json:"created_at"`
}

// Register creates a new user account.
func (h *Handler) Register(w http.ResponseWriter, r *http.Request) {
	var req registerRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error":"invalid request body"}`, http.StatusBadRequest)
		return
	}
	if req.Email == "" || req.Password == "" {
		http.Error(w, `{"error":"email and password are required"}`, http.StatusBadRequest)
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, `{"error":"internal error"}`, http.StatusInternalServerError)
		return
	}

	var userID string
	var createdAt time.Time
	err = h.db.QueryRow(r.Context(),
		`INSERT INTO users (email, password_hash) VALUES ($1, $2) RETURNING id, created_at`,
		req.Email, string(hash),
	).Scan(&userID, &createdAt)
	if err != nil {
		http.Error(w, `{"error":"email already exists"}`, http.StatusConflict)
		return
	}

	token, err := h.auth.GenerateToken(userID)
	if err != nil {
		http.Error(w, `{"error":"failed to generate token"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(authResponse{
		Token: token,
		User: userResponse{
			ID:        userID,
			Email:     req.Email,
			CreatedAt: createdAt.Format(time.RFC3339),
		},
	})
}

// Login authenticates a user and returns a JWT.
func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	var req registerRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error":"invalid request body"}`, http.StatusBadRequest)
		return
	}

	var userID, passwordHash string
	var createdAt time.Time
	err := h.db.QueryRow(r.Context(),
		`SELECT id, password_hash, created_at FROM users WHERE email = $1`,
		req.Email,
	).Scan(&userID, &passwordHash, &createdAt)
	if err != nil {
		http.Error(w, `{"error":"invalid credentials"}`, http.StatusUnauthorized)
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(req.Password)); err != nil {
		http.Error(w, `{"error":"invalid credentials"}`, http.StatusUnauthorized)
		return
	}

	token, err := h.auth.GenerateToken(userID)
	if err != nil {
		http.Error(w, `{"error":"failed to generate token"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(authResponse{
		Token: token,
		User: userResponse{
			ID:        userID,
			Email:     req.Email,
			CreatedAt: createdAt.Format(time.RFC3339),
		},
	})
}

// --- Profile Handlers ---

type profileRequest struct {
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
	Relation  string `json:"relation"`
}

type profileResponse struct {
	ID        string `json:"id"`
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
	Relation  string `json:"relation"`
	CreatedAt string `json:"created_at"`
}

// CreateProfile creates a patient profile under the authenticated user.
func (h *Handler) CreateProfile(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(auth.UserIDKey).(string)

	var req profileRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error":"invalid request body"}`, http.StatusBadRequest)
		return
	}

	var id string
	var createdAt time.Time
	err := h.db.QueryRow(r.Context(),
		`INSERT INTO profiles (user_id, first_name, last_name, relation) VALUES ($1, $2, $3, $4) RETURNING id, created_at`,
		userID, req.FirstName, req.LastName, req.Relation,
	).Scan(&id, &createdAt)
	if err != nil {
		http.Error(w, `{"error":"failed to create profile"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(profileResponse{
		ID:        id,
		FirstName: req.FirstName,
		LastName:  req.LastName,
		Relation:  req.Relation,
		CreatedAt: createdAt.Format(time.RFC3339),
	})
}

// ListProfiles returns all profiles for the authenticated user.
func (h *Handler) ListProfiles(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(auth.UserIDKey).(string)

	rows, err := h.db.Query(r.Context(),
		`SELECT id, first_name, last_name, relation, created_at FROM profiles WHERE user_id = $1 ORDER BY created_at`,
		userID,
	)
	if err != nil {
		http.Error(w, `{"error":"failed to fetch profiles"}`, http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	profiles := []profileResponse{}
	for rows.Next() {
		var p profileResponse
		var t time.Time
		if err := rows.Scan(&p.ID, &p.FirstName, &p.LastName, &p.Relation, &t); err != nil {
			continue
		}
		p.CreatedAt = t.Format(time.RFC3339)
		profiles = append(profiles, p)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(profiles)
}

// --- Agent Catalog Handlers ---

type agentResponse struct {
	ID              string `json:"id"`
	Name            string `json:"name"`
	Version         string `json:"version"`
	Category        string `json:"category"`
	Description     string `json:"description"`
	PriceCents      int    `json:"price_cents"`
	DurationDaysMin int    `json:"duration_days_min"`
	DurationDaysMax int    `json:"duration_days_max"`
	GeminiModel     string `json:"gemini_model"`
}

// ListAgents returns all active analysis agents.
func (h *Handler) ListAgents(w http.ResponseWriter, r *http.Request) {
	rows, err := h.db.Query(r.Context(),
		`SELECT id, name, version, category, description, price_cents, duration_days_min, duration_days_max, gemini_model
		 FROM agent_catalog WHERE active = true ORDER BY name`,
	)
	if err != nil {
		http.Error(w, `{"error":"failed to fetch agents"}`, http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	agents := []agentResponse{}
	for rows.Next() {
		var a agentResponse
		if err := rows.Scan(&a.ID, &a.Name, &a.Version, &a.Category, &a.Description, &a.PriceCents, &a.DurationDaysMin, &a.DurationDaysMax, &a.GeminiModel); err != nil {
			continue
		}
		agents = append(agents, a)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(agents)
}

// --- Subscription Handlers ---

type subscriptionRequest struct {
	ProfileID         string                 `json:"profile_id"`
	AgentID           string                 `json:"agent_id"`
	DurationDays      int                    `json:"duration_days"`
	PrivateBackendURL string                 `json:"private_backend_url"`
	Parameters        map[string]interface{} `json:"parameters"`
}

type subscriptionResponse struct {
	ID                string `json:"id"`
	ProfileID         string `json:"profile_id"`
	AgentID           string `json:"agent_id"`
	Status            string `json:"status"`
	PrivateBackendURL string `json:"private_backend_url"`
	StartsAt          string `json:"starts_at"`
	ExpiresAt         string `json:"expires_at"`
}

// RecommendRequest is the payload from the mobile app
type RecommendRequest struct {
	Symptoms        string                 `json:"symptoms"`
	AppointmentDate string                 `json:"appointment_date,omitempty"`
	Rules           map[string]interface{} `json:"rules,omitempty"`
}

// RecommendAgentHandler uses Gemini to find the best matching agent
func RecommendAgentHandler(db *pgxpool.Pool, geminiClient *gemini.Client) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req RecommendRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "Invalid request payload", http.StatusBadRequest)
			return
		}

		if req.Symptoms == "" {
			http.Error(w, "Symptoms are required", http.StatusBadRequest)
			return
		}

		prompt := "You are a medical triage assistant building a personalized tracking protocol for a patient.\n" +
			"Patient symptoms: " + req.Symptoms + "\n"

		if req.AppointmentDate != "" {
			prompt += "Next appointment: " + req.AppointmentDate + "\n"
		}
		if len(req.Rules) > 0 {
			rulesBytes, _ := json.Marshal(req.Rules)
			prompt += "Patient sensors and rules enabled: " + string(rulesBytes) + "\n"
		}

		prompt += "\nBased ONLY on this information, generate a short daily tracking plan summary (e.g., '1 photo - morning', 'Pain check - evening'). Do not include greetings or disclaimers, just the daily tasks."

		resp, err := geminiClient.GenerateText(r.Context(), prompt)
		if err != nil {
			log.Printf("Gemini API error: %v", err)
			http.Error(w, "Failed to analyze symptoms", http.StatusInternalServerError)
			return
		}

		// Create a dynamic agent response
		agent := agentResponse{
			ID:              "dynamic-plan",
			Name:            "Personalized Protocol",
			Version:         "1.0",
			Category:        "dynamic",
			Description:     strings.TrimSpace(resp),
			PriceCents:      0,
			DurationDaysMin: 1,
			DurationDaysMax: 30,
			GeminiModel:     "gemini-3.5-flash",
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(agent)
	}
}

// CreateSubscription activates a follow-up for a profile.
func (h *Handler) CreateSubscription(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(auth.UserIDKey).(string)

	var req subscriptionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error":"invalid request body"}`, http.StatusBadRequest)
		return
	}

	// Verify the profile belongs to this user
	var count int
	h.db.QueryRow(r.Context(),
		`SELECT COUNT(*) FROM profiles WHERE id = $1 AND user_id = $2`,
		req.ProfileID, userID,
	).Scan(&count)
	if count == 0 {
		http.Error(w, `{"error":"profile not found"}`, http.StatusNotFound)
		return
	}

	// Get agent default duration if not specified
	if req.DurationDays == 0 {
		h.db.QueryRow(r.Context(),
			`SELECT duration_days_default FROM agent_catalog WHERE id = $1`,
			req.AgentID,
		).Scan(&req.DurationDays)
	}

	startsAt := time.Now()
	expiresAt := startsAt.AddDate(0, 0, req.DurationDays)

	paramsJSON, _ := json.Marshal(req.Parameters)

	var subID string
	err := h.db.QueryRow(r.Context(),
		`INSERT INTO subscriptions (profile_id, agent_id, private_backend_url, parameters_json, starts_at, expires_at)
		 VALUES ($1, $2, $3, $4, $5, $6) RETURNING id`,
		req.ProfileID, req.AgentID, req.PrivateBackendURL, paramsJSON, startsAt, expiresAt,
	).Scan(&subID)
	if err != nil {
		http.Error(w, `{"error":"failed to create subscription"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(subscriptionResponse{
		ID:                subID,
		ProfileID:         req.ProfileID,
		AgentID:           req.AgentID,
		Status:            "active",
		PrivateBackendURL: req.PrivateBackendURL,
		StartsAt:          startsAt.Format(time.RFC3339),
		ExpiresAt:         expiresAt.Format(time.RFC3339),
	})
}

// ListSubscriptions returns all subscriptions for profiles owned by the authenticated user.
func (h *Handler) ListSubscriptions(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(auth.UserIDKey).(string)

	rows, err := h.db.Query(r.Context(),
		`SELECT s.id, s.profile_id, s.agent_id, s.status, s.private_backend_url, s.starts_at, s.expires_at
		 FROM subscriptions s
		 JOIN profiles p ON s.profile_id = p.id
		 WHERE p.user_id = $1
		 ORDER BY s.created_at DESC`,
		userID,
	)
	if err != nil {
		http.Error(w, `{"error":"failed to fetch subscriptions"}`, http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	subs := []subscriptionResponse{}
	for rows.Next() {
		var s subscriptionResponse
		var startsAt, expiresAt time.Time
		if err := rows.Scan(&s.ID, &s.ProfileID, &s.AgentID, &s.Status, &s.PrivateBackendURL, &startsAt, &expiresAt); err != nil {
			continue
		}
		s.StartsAt = startsAt.Format(time.RFC3339)
		s.ExpiresAt = expiresAt.Format(time.RFC3339)
		subs = append(subs, s)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(subs)
}
