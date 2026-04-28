# ─── Stage 1: Build ───────────────────────────────────────────────
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --silent

COPY . .
RUN npm run build -- --configuration=production

# ─── Stage 2: Runtime ─────────────────────────────────────────────
FROM node:22-alpine AS runtime

WORKDIR /app

COPY --from=build /app/dist/demo-app ./dist/demo-app

ENV PORT=4000

HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD wget -q -O- http://localhost:4000 || exit 1

EXPOSE 4000

CMD ["node", "dist/demo-app/server/server.mjs"]