package middleware

import (
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/httprate/v2"
)

const maxBodyBytes = 1 << 20 // 1 MiB

// LimitBodySize rejects oversized request bodies.
func LimitBodySize(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Body != nil {
			r.Body = http.MaxBytesReader(w, r.Body, maxBodyBytes)
		}
		next.ServeHTTP(w, r)
	})
}

// TooManyRequestsJSON returns a JSON 429 response.
func TooManyRequestsJSON(w http.ResponseWriter, retryAfter time.Duration) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Retry-After", formatRetryAfter(retryAfter))
	w.WriteHeader(http.StatusTooManyRequests)
	_ = json.NewEncoder(w).Encode(map[string]string{
		"error": "rate limit exceeded, try again later",
	})
}

func formatRetryAfter(d time.Duration) string {
	secs := int(d.Seconds())
	if secs < 1 {
		return "1"
	}
	return strconv.Itoa(secs)
}

// GlobalPerIP limits all traffic per IP (health included, but health is cheap).
func GlobalPerIP() func(http.Handler) http.Handler {
	return httprate.LimitByIP(120, time.Minute, httprate.WithLimitHandler(func(w http.ResponseWriter, r *http.Request) {
		TooManyRequestsJSON(w, time.Minute)
	}))
}

// AuthPerIP limits register/login to reduce account spam.
func AuthPerIP() func(http.Handler) http.Handler {
	return httprate.LimitByIP(10, time.Minute, httprate.WithLimitHandler(func(w http.ResponseWriter, r *http.Request) {
		TooManyRequestsJSON(w, time.Minute)
	}))
}

// APIPerIP limits authenticated routes per IP.
func APIPerIP() func(http.Handler) http.Handler {
	return httprate.LimitByIP(60, time.Minute, httprate.WithLimitHandler(func(w http.ResponseWriter, r *http.Request) {
		TooManyRequestsJSON(w, time.Minute)
	}))
}

// RecommendPerIP limits Gemini calls per IP (expensive).
func RecommendPerIP() func(http.Handler) http.Handler {
	return httprate.LimitByIP(5, time.Minute, httprate.WithLimitHandler(func(w http.ResponseWriter, r *http.Request) {
		TooManyRequestsJSON(w, time.Minute)
	}))
}

// RecommendPerUser limits Gemini calls per authenticated user.
func RecommendPerUser(userIDFromContext func(*http.Request) string) func(http.Handler) http.Handler {
	return httprate.Limit(10, time.Hour, httprate.WithKeyFuncs(func(r *http.Request) (string, error) {
		uid := userIDFromContext(r)
		if uid == "" {
			uid = "anonymous"
		}
		return "recommend:user:" + uid, nil
	}), httprate.WithLimitHandler(func(w http.ResponseWriter, r *http.Request) {
		TooManyRequestsJSON(w, time.Hour)
	}))
}
