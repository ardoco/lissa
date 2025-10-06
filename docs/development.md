# Development Guide

## Development Setup

### Prerequisites

- Java JDK 21 or later
- Maven
- API keys for the language model platforms you plan to use (configured as environment variables):
  - OpenAI: `OPENAI_ORGANIZATION_ID` and `OPENAI_API_KEY`
  - Open WebUI: `OPENWEBUI_URL` and `OPENWEBUI_API_KEY`
  - Blablador: `BLABLADOR_API_KEY`
  - DeepSeek: `DEEPSEEK_API_KEY`
  - Ollama: `OLLAMA_HOST` (required), `OLLAMA_USER`, `OLLAMA_PASSWORD` (optional)

### Building the Project

```bash
mvn clean package
```

### Running Tests

```bash
mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

Please ensure your code follows the project's coding standards and includes appropriate tests.

## Troubleshooting

- If you encounter cache-related issues, try clearing the cache directory
- For API-related errors, verify your API key configuration for the platform you're using:
  - OpenAI: Check `OPENAI_ORGANIZATION_ID` and `OPENAI_API_KEY`
  - Open WebUI: Check `OPENWEBUI_URL` and `OPENWEBUI_API_KEY`
  - Blablador: Check `BLABLADOR_API_KEY`
  - DeepSeek: Check `DEEPSEEK_API_KEY`
  - Ollama: Check `OLLAMA_HOST` (required), `OLLAMA_USER`, `OLLAMA_PASSWORD` (optional)
- Check the console output for detailed error messages

## Additional Resources

- [Project Website](https://ardoco.de/)
- [Paper](https://ardoco.de/c/icse25)
- [Code of Conduct](../CODE_OF_CONDUCT.md)

