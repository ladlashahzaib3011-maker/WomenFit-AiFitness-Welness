# WomanFit Secure AI Proxy - Production Container Dockerfile
FROM node:20-alpine AS runner

WORKDIR /app

# Install dependencies first for optimal Docker layer caching
COPY package.json ./
RUN npm install --omit=dev

# Copy server code
COPY server.js ./

# Set environment defaults
ENV NODE_ENV=production
ENV PORT=8080

EXPOSE 8080

# Run server as non-root node user for container security
USER node

CMD ["node", "server.js"]
