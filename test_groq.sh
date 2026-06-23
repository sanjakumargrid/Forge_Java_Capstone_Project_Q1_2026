curl -X POST "https://api.groq.com/openai/v1/chat/completions" \
     -H "Authorization: Bearer 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" \
     -H "Content-Type: application/json" \
     -d '{"model": "llama3-8b-8192", "messages": [{"role": "user", "content": "Hello"}]}'
