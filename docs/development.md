# Development Guide

## Development Setup

### Prerequisites

- Java JDK 21 or later
- Maven
- Docker — **optional**. The Testcontainers-based `cache/RestRedisTest` starts a `redis:latest` container to exercise the REST Redis cache. Without a running Docker daemon that test class is skipped automatically (`@Testcontainers(disabledWithoutDocker = true)`) and the build still succeeds; you simply do not get REST Redis coverage.
- API keys for the language model platforms you plan to use, configured either as environment variables or in a `.env` file (see [Credentials](#credentials) below):
  - OpenAI: `OPENAI_ORGANIZATION_ID` and `OPENAI_API_KEY`
  - Open WebUI: `OPENWEBUI_URL` and `OPENWEBUI_API_KEY`
  - Blablador: `BLABLADOR_API_KEY`
  - DeepSeek: `DEEPSEEK_API_KEY`
  - Ollama (chat): `OLLAMA_HOST` (required), `OLLAMA_USER`, `OLLAMA_PASSWORD` (optional)
  - Ollama (embeddings): `OLLAMA_EMBEDDING_HOST` (required), `OLLAMA_EMBEDDING_USER`, `OLLAMA_EMBEDDING_PASSWORD` (optional)

### Credentials

Copy the tracked `env-template` to `.env` and fill in the keys you need, or export them as process environment variables:

```bash
cp env-template .env
```

Two things are easy to get wrong here:

- The `.env` file is read from the **current working directory** — the directory you run LiSSA from, which is not necessarily the repository root.
- The process environment takes **precedence** over `.env`. A key in `.env` is used only when that variable is not already exported, so an exported `OPENAI_API_KEY` silently overrides the one in your `.env`.

`OPENAI_ORGANIZATION_ID` and `OPENAI_API_KEY` must be set even for a run that is served entirely from the cache: the OpenAI embedding creator and chat model provider both validate them at construction time, before any cache is consulted. Dummy values are sufficient and open no connection — this is what `src/test/resources/.env-test` does for the offline end-to-end test.

### Building the Project

```bash
mvn clean package
```

This works with or without Docker — see the note on `RestRedisTest` under Prerequisites.

### Running Tests

```bash
mvn test
```

On a machine without Docker, `RestRedisTest` reports its tests as skipped and the rest of the suite runs normally. To get REST Redis coverage, start a Docker daemon before running the tests.

### Verifying Before a Pull Request

```bash
mvn verify
```

`verify` additionally runs `spotless:check`, which is what CI enforces.

> [!IMPORTANT]
> Spotless is configured with `<ratchetFrom>origin/main</ratchetFrom>`, so it resolves that ref through JGit. `mvn verify` therefore needs a git clone whose `origin/main` remote-tracking ref has been fetched. Building from an unpacked source archive requires `-Dspotless.check.skip=true`.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run `mvn spotless:apply`
5. Submit a pull request

### Formatting

Run `mvn spotless:apply` before committing — `mvn verify` runs `spotless:check` and CI fails otherwise. Spotless covers:

- Java sources: [palantir-java-format](https://github.com/palantir/palantir-java-format) in `PALANTIR` style, import order from `spotless.importorder`, and a mandatory license header from `header.txt`.
- Markdown: `README.md` and `docs/**/*.md` are formatted with flexmark, so documentation changes are format-checked too.

Please ensure your code follows the project's coding standards and includes appropriate tests.

## Troubleshooting

- If you encounter cache-related issues, try clearing the cache directory
- For API-related errors, verify your API key configuration for the platform you're using:
  - OpenAI: Check `OPENAI_ORGANIZATION_ID` and `OPENAI_API_KEY`
  - Open WebUI: Check `OPENWEBUI_URL` and `OPENWEBUI_API_KEY`
  - Blablador: Check `BLABLADOR_API_KEY`
  - DeepSeek: Check `DEEPSEEK_API_KEY`
  - Ollama (chat): Check `OLLAMA_HOST` (required), `OLLAMA_USER`, `OLLAMA_PASSWORD` (optional)
  - Ollama (embeddings): Check `OLLAMA_EMBEDDING_HOST` (required), `OLLAMA_EMBEDDING_USER`, `OLLAMA_EMBEDDING_PASSWORD` (optional)
- If a variable looks set in `.env` but LiSSA uses a different value, check whether it is also exported in your shell — the process environment wins (`unset <VAR>` to fall back to `.env`)
- If `RestRedisTest` is reported as skipped, Docker is not available — start the Docker daemon to run it
- Check the console output for detailed error messages

## Additional Resources

- [Project Website](https://ardoco.de/)
- [Paper](https://ardoco.de/c/icse25)
- [Code of Conduct](../CODE_OF_CONDUCT.md)

