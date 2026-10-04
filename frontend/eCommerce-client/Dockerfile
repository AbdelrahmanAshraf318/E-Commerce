# ---------- Stage 1: build the Angular app ----------
FROM node:24-alpine AS build
WORKDIR /build

# Install dependencies first, in their own layer, so code changes don't reinstall node_modules.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npx ng build --configuration production


# ---------- Stage 2: serve the static files ----------
# The build output is plain HTML/JS/CSS, so a tiny web server is all that's needed at runtime.
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/eCommerce-client/browser /usr/share/nginx/html
EXPOSE 80
