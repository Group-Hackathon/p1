package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"

	"github.com/Group-Hackathon/p1/global-app-backend/internal/auth"
	"github.com/Group-Hackathon/p1/global-app-backend/internal/gemini"
)

// Handler holds dependencies for HTTP handlers.
type Handler struct {
	db     *pgxpool.Pool
	auth   *auth.Service
	gemini *gemini.Client
}

// New creates a new Handler.
func New(db *pgxpool.Pool, authService *auth.Service, geminiClient *gemini.Client) *Handler {
	return &Handler{db: db, auth: authService, gemini: geminiClient}
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
	Schedule        map[string][]string `json:"schedule,omitempty"`
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
	Status            string                 `json:"status"`
	PrivateBackendURL string                 `json:"private_backend_url"`
	Parameters        map[string]interface{} `json:"parameters,omitempty"`
	StartsAt          string                 `json:"starts_at"`
	ExpiresAt         string                 `json:"expires_at"`
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

		prompt += `
Based ONLY on this information, generate a short daily tracking plan.
Respond STRICTLY with a valid JSON object matching this schema:
{
  "title": "Short descriptive title (max 5 words, e.g. 'Post-Op Knee Monitoring', 'Fever Tracking')",
  "description": "Short human-readable summary of the plan",
  "schedule": {
    "08:00": ["pain", "temperature"],
    "12:00": ["temperature"],
    "20:00": ["pain", "temperature", "photo"]
  }
}
Do not include any markdown formatting, greetings, or disclaimers. Just the JSON object.
Use 24h format for the time keys. The array should contain strings like "pain", "temperature", "photo", "smartwatch", "blood_pressure". Only include actions that were requested or enabled in the patient's rules.`

		resp, err := geminiClient.GenerateText(r.Context(), prompt)
		if err != nil {
			log.Printf("Gemini API error: %v", err)
			http.Error(w, "Failed to analyze symptoms", http.StatusInternalServerError)
			return
		}

		cleanResp := strings.TrimSpace(resp)
		cleanResp = strings.TrimPrefix(cleanResp, "```json")
		cleanResp = strings.TrimPrefix(cleanResp, "```")
		cleanResp = strings.TrimSuffix(cleanResp, "```")
		cleanResp = strings.TrimSpace(cleanResp)

		var geminiOutput struct {
			Title       string              `json:"title"`
			Description string              `json:"description"`
			Schedule    map[string][]string `json:"schedule"`
		}

		if err := json.Unmarshal([]byte(cleanResp), &geminiOutput); err != nil {
			log.Printf("Failed to parse Gemini JSON: %v. Raw: %s", err, resp)
			geminiOutput.Title = "Personalized Protocol"
			geminiOutput.Description = cleanResp
			geminiOutput.Schedule = map[string][]string{
				"08:00": {"temperature", "pain"},
				"20:00": {"temperature", "pain"},
			}
		}
		if geminiOutput.Title == "" {
			geminiOutput.Title = "Personalized Protocol"
		}

		// Create a dynamic agent response
		agent := agentResponse{
			ID:              "dynamic-plan",
			Name:            geminiOutput.Title,
			Version:         "1.0",
			Category:        "dynamic",
			Description:     strings.TrimSpace(geminiOutput.Description),
			PriceCents:      0, // Price is calculated on frontend based on duration
			DurationDaysMin: 1,
			DurationDaysMax: 30,
			GeminiModel:     "gemini-3.5-flash",
			Schedule:        geminiOutput.Schedule,
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
		`SELECT s.id, s.profile_id, s.agent_id, s.status, s.private_backend_url, s.parameters_json, s.starts_at, s.expires_at
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
		var paramsJSON []byte
		if err := rows.Scan(&s.ID, &s.ProfileID, &s.AgentID, &s.Status, &s.PrivateBackendURL, &paramsJSON, &startsAt, &expiresAt); err != nil {
			continue
		}
		if len(paramsJSON) > 0 {
			var params map[string]interface{}
			if err := json.Unmarshal(paramsJSON, &params); err == nil {
				s.Parameters = params
			}
		}
		s.StartsAt = startsAt.Format(time.RFC3339)
		s.ExpiresAt = expiresAt.Format(time.RFC3339)
		subs = append(subs, s)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(subs)
}

// DeleteSubscription cancels a subscription.
func (h *Handler) DeleteSubscription(w http.ResponseWriter, r *http.Request) {
	subID := chi.URLParam(r, "id")
	userID := r.Context().Value(auth.UserIDKey).(string)

	res, err := h.db.Exec(r.Context(),
		`DELETE FROM subscriptions WHERE id = $1 AND profile_id IN (SELECT id FROM profiles WHERE user_id = $2)`,
		subID, userID,
	)
	if err != nil {
		http.Error(w, `{"error":"failed to delete"}`, http.StatusInternalServerError)
		return
	}
	if res.RowsAffected() == 0 {
		http.Error(w, `{"error":"not found"}`, http.StatusNotFound)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// PatchSubscription updates a subscription (e.g. change appointment date).
func (h *Handler) PatchSubscription(w http.ResponseWriter, r *http.Request) {
	subID := chi.URLParam(r, "id")
	userID := r.Context().Value(auth.UserIDKey).(string)

	var req struct {
		ExpiresAt string `json:"expires_at"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error":"invalid request body"}`, http.StatusBadRequest)
		return
	}

	newExpiry, err := time.Parse(time.RFC3339, req.ExpiresAt)
	if err != nil {
		// Try date-only format
		newExpiry, err = time.Parse("2006-01-02", req.ExpiresAt)
		if err != nil {
			http.Error(w, `{"error":"invalid date format, use RFC3339 or YYYY-MM-DD"}`, http.StatusBadRequest)
			return
		}
		// Set to end of day
		newExpiry = newExpiry.Add(23*time.Hour + 59*time.Minute + 59*time.Second)
	}

	res, err := h.db.Exec(r.Context(),
		`UPDATE subscriptions SET expires_at = $1 WHERE id = $2 AND profile_id IN (SELECT id FROM profiles WHERE user_id = $3)`,
		newExpiry, subID, userID,
	)
	if err != nil {
		http.Error(w, `{"error":"failed to update subscription"}`, http.StatusInternalServerError)
		return
	}
	if res.RowsAffected() == 0 {
		http.Error(w, `{"error":"subscription not found"}`, http.StatusNotFound)
		return
	}

	// Return updated subscription
	var s subscriptionResponse
	var startsAt, expiresAt time.Time
	var paramsJSON []byte
	err = h.db.QueryRow(r.Context(),
		`SELECT s.id, s.profile_id, s.agent_id, s.status, s.private_backend_url, s.parameters_json, s.starts_at, s.expires_at
		 FROM subscriptions s WHERE s.id = $1`, subID,
	).Scan(&s.ID, &s.ProfileID, &s.AgentID, &s.Status, &s.PrivateBackendURL, &paramsJSON, &startsAt, &expiresAt)
	if err != nil {
		http.Error(w, `{"error":"failed to fetch updated subscription"}`, http.StatusInternalServerError)
		return
	}
	if len(paramsJSON) > 0 {
		var params map[string]interface{}
		if err := json.Unmarshal(paramsJSON, &params); err == nil {
			s.Parameters = params
		}
	}
	s.StartsAt = startsAt.Format(time.RFC3339)
	s.ExpiresAt = expiresAt.Format(time.RFC3339)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(s)
}

// DeleteAccount deletes the authenticated user and all associated data.
func (h *Handler) DeleteAccount(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(auth.UserIDKey).(string)

	_, err := h.db.Exec(r.Context(), `DELETE FROM users WHERE id = $1`, userID)
	if err != nil {
		http.Error(w, `{"error":"failed to delete account"}`, http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// --- Timeline Handlers ---

type timelineEventRequest struct {
	Content       string  `json:"content"`
	DateLabel     string  `json:"date_label"`      // e.g. 'Day 1 - Evening'
	EffectiveDate *string `json:"effective_date"`   // optional, for retroactive entries (RFC3339 or YYYY-MM-DD)
}

type timelineEventResponse struct {
	ID             string  `json:"id"`
	SubscriptionID string  `json:"subscription_id"`
	Type           string  `json:"type"`
	DateLabel      string  `json:"date_label"`
	Content        string  `json:"content"`
	CreatedAt      string  `json:"created_at"`
	EffectiveAt    *string `json:"effective_at,omitempty"`
}

// GetTimeline fetches the vertical timeline for a subscription.
func (h *Handler) GetTimeline(w http.ResponseWriter, r *http.Request) {
	subID := chi.URLParam(r, "id")
	userID := r.Context().Value(auth.UserIDKey).(string)

	// Verify ownership
	var count int
	h.db.QueryRow(r.Context(),
		`SELECT COUNT(*) FROM subscriptions s JOIN profiles p ON s.profile_id = p.id WHERE s.id = $1 AND p.user_id = $2`,
		subID, userID,
	).Scan(&count)
	if count == 0 {
		http.Error(w, `{"error":"subscription not found"}`, http.StatusNotFound)
		return
	}

	rows, err := h.db.Query(r.Context(),
		`SELECT id, subscription_id, type, date_label, content, created_at, effective_at FROM timeline_events WHERE subscription_id = $1 ORDER BY COALESCE(effective_at, created_at) ASC`,
		subID,
	)
	if err != nil {
		http.Error(w, `{"error":"failed to fetch timeline"}`, http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	events := []timelineEventResponse{}
	for rows.Next() {
		var e timelineEventResponse
		var t time.Time
		var effectiveAt *time.Time
		if err := rows.Scan(&e.ID, &e.SubscriptionID, &e.Type, &e.DateLabel, &e.Content, &t, &effectiveAt); err == nil {
			e.CreatedAt = t.Format(time.RFC3339)
			if effectiveAt != nil {
				formatted := effectiveAt.Format(time.RFC3339)
				e.EffectiveAt = &formatted
			}
			events = append(events, e)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(events)
}

// PostTimelineEvent adds a new event and simulates AI response.
func (h *Handler) PostTimelineEvent(w http.ResponseWriter, r *http.Request) {
	subID := chi.URLParam(r, "id")
	userID := r.Context().Value(auth.UserIDKey).(string)

	var req timelineEventRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error":"invalid request"}`, http.StatusBadRequest)
		return
	}

	// Verify ownership
	var count int
	h.db.QueryRow(r.Context(),
		`SELECT COUNT(*) FROM subscriptions s JOIN profiles p ON s.profile_id = p.id WHERE s.id = $1 AND p.user_id = $2`,
		subID, userID,
	).Scan(&count)
	if count == 0 {
		http.Error(w, `{"error":"subscription not found"}`, http.StatusNotFound)
		return
	}

	// Parse optional effective_date
	var effectiveAt *time.Time
	if req.EffectiveDate != nil && *req.EffectiveDate != "" {
		if t, err := time.Parse(time.RFC3339, *req.EffectiveDate); err == nil {
			effectiveAt = &t
		} else if t, err := time.Parse("2006-01-02", *req.EffectiveDate); err == nil {
			effectiveAt = &t
		}
	}

	// Insert User Event
	_, err := h.db.Exec(r.Context(),
		`INSERT INTO timeline_events (subscription_id, type, date_label, content, effective_at) VALUES ($1, 'user', $2, $3, $4)`,
		subID, req.DateLabel, req.Content, effectiveAt,
	)
	if err != nil {
		http.Error(w, `{"error":"failed to save event"}`, http.StatusInternalServerError)
		return
	}

	// Generate AI response based on context if it's a chat question
	aiReply := "Your measurements have been successfully saved."
	
	if req.DateLabel == "Question" && h.gemini != nil {
		var paramsJSON []byte
		h.db.QueryRow(r.Context(), `SELECT parameters_json FROM subscriptions WHERE id = $1`, subID).Scan(&paramsJSON)
		
		var history string
		rows, _ := h.db.Query(r.Context(), `SELECT type, date_label, content FROM timeline_events WHERE subscription_id = $1 ORDER BY created_at DESC LIMIT 10`, subID)
		var histEvents []string
		for rows.Next() {
			var t, l, c string
			if err := rows.Scan(&t, &l, &c); err == nil {
				histEvents = append([]string{fmt.Sprintf("[%s] %s: %s", t, l, c)}, histEvents...)
			}
		}
		rows.Close()
		history = strings.Join(histEvents, "\n")
		
		prompt := fmt.Sprintf(`You are a medical assistant chatbot for an active tracking protocol.
If the user asks questions outside the tracking scope or asks for a diagnosis, politely decline and tell them to consult a doctor. Do not give medical advice.
Current Date and Time: %s
Tracking Parameters: %s
Recent History:
%s
User's Question: %s`, time.Now().Format(time.RFC1123), string(paramsJSON), history, req.Content)
		
		// Use a detached context so if the client disconnects due to timeout, we still save the AI reply.
		bgCtx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
		defer cancel()
		if resp, err := h.gemini.GenerateText(bgCtx, prompt); err == nil {
			aiReply = strings.TrimSpace(resp)
		}
	}

	// Insert AI Event
	var aiID string
	var aiCreatedAt time.Time
	// Use background context here as well for safety
	err = h.db.QueryRow(context.Background(),
		`INSERT INTO timeline_events (subscription_id, type, date_label, content) VALUES ($1, 'ai', $2, $3) RETURNING id, created_at`,
		subID, req.DateLabel+" - Assistant", aiReply,
	).Scan(&aiID, &aiCreatedAt)

	// Return the AI event as confirmation
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(timelineEventResponse{
		ID:             aiID,
		SubscriptionID: subID,
		Type:           "ai",
		DateLabel:      req.DateLabel + " - Assistant",
		Content:        aiReply,
		CreatedAt:      aiCreatedAt.Format(time.RFC3339),
	})
}

// DeleteTimelineEvent deletes a specific user event and its automatically generated AI response.
func (h *Handler) DeleteTimelineEvent(w http.ResponseWriter, r *http.Request) {
	subID := chi.URLParam(r, "id")
	eventID := chi.URLParam(r, "eventId")
	userID := r.Context().Value(auth.UserIDKey).(string)

	// Verify ownership
	var count int
	h.db.QueryRow(r.Context(),
		`SELECT COUNT(*) FROM subscriptions s JOIN profiles p ON s.profile_id = p.id WHERE s.id = $1 AND p.user_id = $2`,
		subID, userID,
	).Scan(&count)
	if count == 0 {
		http.Error(w, `{"error":"subscription not found"}`, http.StatusNotFound)
		return
	}

	// Fetch the user event to find its creation time
	var createdAt time.Time
	err := h.db.QueryRow(r.Context(), `SELECT created_at FROM timeline_events WHERE id = $1 AND subscription_id = $2 AND type = 'user'`, eventID, subID).Scan(&createdAt)
	if err != nil {
		http.Error(w, `{"error":"event not found or not a user event"}`, http.StatusNotFound)
		return
	}

	// Delete the user event
	_, err = h.db.Exec(r.Context(), `DELETE FROM timeline_events WHERE id = $1`, eventID)
	if err != nil {
		http.Error(w, `{"error":"failed to delete user event"}`, http.StatusInternalServerError)
		return
	}

	// Also delete the AI event created immediately after (the very next AI event)
	h.db.Exec(r.Context(), `
		DELETE FROM timeline_events 
		WHERE id = (
			SELECT id FROM timeline_events 
			WHERE subscription_id = $1 AND type = 'ai' AND created_at >= $2 
			ORDER BY created_at ASC 
			LIMIT 1
		)`, subID, createdAt)

	w.WriteHeader(http.StatusNoContent)
}
