-include .env
export

.PHONY: setup dev dev-db dev-api dev-web stop test lint

setup:
	cp -n .env.example .env || true
	@echo "Created .env if missing."

dev: dev-db
	@echo "Run API and WEB in separate terminals:"
	@echo "  make dev-api"
	@echo "  make dev-web"

dev-db:
	docker compose -f infra/docker-compose.yml --env-file .env up -d db

dev-api:
	cd apps/api && mvn spring-boot:run

dev-web:
	cd apps/web && npm install && npm run dev

stop:
	docker compose -f infra/docker-compose.yml --env-file .env down

test:
	cd apps/api && mvn test

lint:
	cd apps/web && npm install && npm run lint
