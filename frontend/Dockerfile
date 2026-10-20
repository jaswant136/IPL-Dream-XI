FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
EXPOSE 5173
# Dev server (hot reload). VITE_* values are read from the container environment at start-up.
CMD ["npm", "run", "dev"]
