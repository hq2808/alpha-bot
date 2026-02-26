# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Serve stage (Nginx)
FROM nginx:alpine
COPY --from=build /app/dist/alpha-bot-frontend/browser /usr/share/nginx/html
# Nginx config to proxy /api to backend
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
