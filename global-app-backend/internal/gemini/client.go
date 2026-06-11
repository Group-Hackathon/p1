package gemini

import (
	"context"
	"fmt"
	"log"

	"google.golang.org/genai"
)

// Config controls which Gemini backends are available.
//
// If APIKey is set, the Gemini API (AI Studio) is the primary backend with
// Model. Vertex AI (Project/Location) is always configured as fallback with
// FallbackModel, so the service keeps working if the API key has no credits
// or the latest model is unavailable.
type Config struct {
	APIKey        string
	Project       string
	Location      string
	Model         string // latest flash model, served via AI Studio (e.g. gemini-3.5-flash)
	FallbackModel string // model available on Vertex AI (e.g. gemini-2.5-flash)
}

type backend struct {
	name   string
	client *genai.Client
	model  string
}

// Client wraps one or two genai clients with automatic fallback.
type Client struct {
	backends []backend
}

// NewClient initializes the Gemini client(s) from the given config.
func NewClient(ctx context.Context, cfg Config) (*Client, error) {
	c := &Client{}

	if cfg.APIKey != "" {
		primary, err := genai.NewClient(ctx, &genai.ClientConfig{
			Backend: genai.BackendGeminiAPI,
			APIKey:  cfg.APIKey,
		})
		if err != nil {
			return nil, fmt.Errorf("failed to create gemini api client: %w", err)
		}
		c.backends = append(c.backends, backend{name: "gemini-api", client: primary, model: cfg.Model})
	}

	vertex, err := genai.NewClient(ctx, &genai.ClientConfig{
		Backend:  genai.BackendVertexAI,
		Project:  cfg.Project,
		Location: cfg.Location,
	})
	if err != nil {
		if len(c.backends) == 0 {
			return nil, fmt.Errorf("failed to create vertex ai client: %w", err)
		}
		log.Printf("Warning: vertex ai fallback unavailable: %v", err)
	} else {
		c.backends = append(c.backends, backend{name: "vertex-ai", client: vertex, model: cfg.FallbackModel})
	}

	return c, nil
}

// Close is kept for symmetry; the genai HTTP clients hold no connection state.
func (c *Client) Close() {}

func (c *Client) generate(ctx context.Context, systemPrompt, userText string) (string, error) {
	var config *genai.GenerateContentConfig
	temp := float32(0.2)
	config = &genai.GenerateContentConfig{Temperature: &temp}
	if systemPrompt != "" {
		config.SystemInstruction = &genai.Content{
			Parts: []*genai.Part{{Text: systemPrompt}},
		}
	}

	var lastErr error
	for _, b := range c.backends {
		resp, err := b.client.Models.GenerateContent(ctx, b.model, genai.Text(userText), config)
		if err != nil {
			log.Printf("Gemini backend %s (%s) failed, trying next: %v", b.name, b.model, err)
			lastErr = err
			continue
		}
		text := resp.Text()
		if text == "" {
			lastErr = fmt.Errorf("empty response from model %s", b.model)
			continue
		}
		return text, nil
	}
	if lastErr == nil {
		lastErr = fmt.Errorf("no gemini backend configured")
	}
	return "", lastErr
}

// GenerateText generates text from a prompt, falling back across backends.
func (c *Client) GenerateText(ctx context.Context, prompt string) (string, error) {
	return c.generate(ctx, "", prompt)
}

// AnalyzeFollowUp runs an analysis with a domain-specific system prompt.
func (c *Client) AnalyzeFollowUp(ctx context.Context, systemPrompt string, patientData string) (string, error) {
	return c.generate(ctx, systemPrompt, patientData)
}
