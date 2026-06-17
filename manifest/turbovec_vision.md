# Turbovec & RAG Integration Vision

Currently, Pre-Appointment 1 uses a direct prompt generation approach with Gemini to build personalized follow-up plans. The prompt is constructed dynamically by passing the user's symptoms, appointment date, and selected device rules (temperature, smartwatch, etc.).

## The Future: `turbovec` & `fllowpatientDB`

In the future, we will transition to a **Retrieval-Augmented Generation (RAG)** architecture using `turbovec` (or a similar vectorization engine).

### Architecture

1. **The Knowledge Base (`fllowpatientDB`)**
   A repository of medical guidelines, past successful protocols, and device-specific tracking best practices written in Markdown. This acts as the single source of truth for safe medical tracking parameters.

2. **Vectorization**
   `turbovec` will embed these markdown documents into a vector database (e.g., pgvector in our existing Postgres instance).

3. **Dynamic Context Retrieval**
   When a user submits their symptoms and rules, the Go backend will first query the vector database to retrieve the *K* most relevant protocol snippets from `fllowpatientDB`.

4. **Augmented Prompting**
   The retrieved context will be injected into the Gemini prompt:
   ```text
   System: You are an expert medical triage assistant.
   
   Medical Guidelines (from turbovec):
   [Snippet 1: Best practices for post-op wound tracking]
   [Snippet 2: How to use a smartwatch for fever detection]
   
   User's situation: {Symptoms}
   User's devices: {Rules}
   Days until appointment: {Duration}
   
   Based ONLY on the guidelines provided, generate a daily tracking plan...
   ```

### Benefits

- **Accuracy**: Eliminates AI hallucination by forcing Gemini to adhere strictly to the retrieved markdown protocols.
- **Maintainability**: Doctors can simply update the markdown files in `fllowpatientDB` without requiring a backend code change.
- **Scalability**: Allows supporting thousands of different medical conditions seamlessly.
